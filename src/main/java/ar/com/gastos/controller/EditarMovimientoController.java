package ar.com.gastos.controller;

import ar.com.gastos.dao.ComercioDao;
import ar.com.gastos.dao.MovimientoDao;
import ar.com.gastos.model.Comercio;
import ar.com.gastos.model.Movimiento;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class EditarMovimientoController {

  private static final Logger logger = LoggerFactory.getLogger(EditarMovimientoController.class);

  @FXML
  private DatePicker dpFecha;
  @FXML
  private Label lblComercio;
  @FXML
  private ComboBox<Comercio> cmbComercio;
  @FXML
  private Label lblDescripcion;
  @FXML
  private TextField txtDescripcion;
  @FXML
  private TextField txtComentario;
  @FXML
  private TextField txtMonto;
  @FXML
  private Label lblCuotas;
  @FXML
  private TextField txtCuotas;

  private Movimiento movimientoActual;

  /**
   * Recibe el movimiento y precarga los campos según su categoría y tipo:
   * - EGRESO con comercio_id > 0 → ComboBox de comercios + cuotas
   * - EGRESO con comercio_id = 0 → descripción libre (débito/recurrente)
   * - PAGO                       → descripción libre, sin cuotas
   */
  public void setMovimiento(Movimiento m) {
    this.movimientoActual = m;
    dpFecha.setValue(m.getFecha());
    txtMonto.setText(m.getMonto().toPlainString());

    boolean esEgresoConComercio = "EGRESO".equals(m.getCategoria()) && m.getComercioId() > 0;
    boolean esDescripcionLibre = !esEgresoConComercio; // débito recurrente o pago

    // --- ComboBox de comercios ---
    setVisible(lblComercio, esEgresoConComercio);
    setVisible(cmbComercio, esEgresoConComercio);

    // --- TextField de descripción libre ---
    setVisible(lblDescripcion, esDescripcionLibre);
    setVisible(txtDescripcion, esDescripcionLibre);

    // --- Cuotas solo para egreso con comercio ---
    setVisible(lblCuotas, esEgresoConComercio);
    setVisible(txtCuotas, esEgresoConComercio);

    if (esEgresoConComercio) {
      txtCuotas.setText(String.valueOf(m.getCuotas()));
      cargarComercios(m.getComercioId());
    } else {
      // Débito recurrente o pago — mostramos la descripción
      txtDescripcion.setText(m.getDescripcion() != null ? m.getDescripcion() : "");
    }
    // Cargamos el comentario siempre, independiente del tipo
    txtComentario.setText(m.getComentario() != null ? m.getComentario() : "");
  }

  // --- Helper para mostrar/ocultar nodos ---
  private void setVisible(javafx.scene.Node node, boolean visible) {
    node.setVisible(visible);
    node.setManaged(visible);
  }

  // --- Carga comercios y selecciona el actual ---
  private void cargarComercios(int comercioIdActual) {
    try {
      ComercioDao dao = new ComercioDao();
      List<Comercio> comercios = dao.findAllActivos();
      cmbComercio.getItems().clear();
      cmbComercio.getItems().addAll(comercios);
      comercios.stream()
          .filter(c -> c.getId() == comercioIdActual)
          .findFirst()
          .ifPresent(cmbComercio::setValue);
    } catch (SQLException ex) {
      logger.error("Error al cargar comercios", ex);
    }
  }

  // --- Guardar cambios ---
  @FXML
  private void guardar() {
    String montoStr = txtMonto.getText().trim();
    if (montoStr.isEmpty() || dpFecha.getValue() == null) {
      Toast.show(getStage(), "Fecha y monto son obligatorios");
      return;
    }

    try {
      BigDecimal monto = new BigDecimal(montoStr).setScale(2);
      movimientoActual.setFecha(dpFecha.getValue());
      movimientoActual.setMonto(monto);

      boolean esEgresoConComercio = "EGRESO".equals(movimientoActual.getCategoria())
          && movimientoActual.getComercioId() > 0;

      if (esEgresoConComercio) {
        Comercio comercio = cmbComercio.getValue();
        if (comercio == null) {
          Toast.show(getStage(), "Debe seleccionar un comercio");
          return;
        }
        String cuotasStr = txtCuotas.getText().trim();
        int cuotas = cuotasStr.isEmpty() ? 1 : Integer.parseInt(cuotasStr);
        if (cuotas < 1) {
          Toast.show(getStage(), "Las cuotas deben ser al menos 1");
          return;
        }
        movimientoActual.setComercioId(comercio.getId());
        movimientoActual.setDescripcion(null);
        movimientoActual.setCuotas(cuotas);

      } else {
        String descripcion = txtDescripcion.getText().trim();
        if (descripcion.isEmpty()) {
          Toast.show(getStage(), "Debe ingresar una descripción");
          return;
        }
        movimientoActual.setDescripcion(descripcion.toUpperCase());
      }

      // Comentario — aplica siempre, independiente del tipo
      String comentario = txtComentario.getText().trim();
      movimientoActual.setComentario(comentario.isEmpty() ? null : comentario.toUpperCase());

      new MovimientoDao().update(movimientoActual);
      logger.info("Movimiento {} actualizado - id {}",
          movimientoActual.getCategoria(), movimientoActual.getId());

      MovimientoEventBus.publish("edicion");
      Toast.show(getStage(), "Movimiento actualizado");
      getStage().close();

    } catch (NumberFormatException ex) {
      Toast.show(getStage(), "Monto o cuotas inválidos");
      logger.error("Error al parsear monto/cuotas", ex);
    } catch (SQLException ex) {
      Toast.show(getStage(), "Error al guardar cambios");
      logger.error("Error al actualizar movimiento", ex);
    }
  }

  private Stage getStage() {
    return (Stage) dpFecha.getScene().getWindow();
  }
}