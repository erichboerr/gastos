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

  @FXML private DatePicker      dpFecha;
  @FXML private Label           lblComercio;
  @FXML private ComboBox<Comercio> cmbComercio;
  @FXML private Label           lblDescripcion;
  @FXML private TextField       txtDescripcion;
  @FXML private TextField       txtMonto;
  @FXML private Label           lblCuotas;
  @FXML private TextField       txtCuotas;

  private Movimiento movimientoActual;

  /**
   * Recibe el movimiento y precarga los campos según su categoría.
   * EGRESO → muestra ComboBox de comercios y campo cuotas.
   * PAGO   → muestra TextField de descripción libre, oculta cuotas.
   */
  public void setMovimiento(Movimiento m) {
    this.movimientoActual = m;
    dpFecha.setValue(m.getFecha());
    txtMonto.setText(m.getMonto().toPlainString());

    if ("EGRESO".equals(m.getCategoria())) {
      // Ocultamos el campo de descripción libre
      lblDescripcion.setVisible(false);
      lblDescripcion.setManaged(false);
      txtDescripcion.setVisible(false);
      txtDescripcion.setManaged(false);

      // Mostramos el ComboBox de comercios
      lblComercio.setVisible(true);
      lblComercio.setManaged(true);
      cmbComercio.setVisible(true);
      cmbComercio.setManaged(true);

      // Mostramos cuotas
      lblCuotas.setVisible(true);
      lblCuotas.setManaged(true);
      txtCuotas.setVisible(true);
      txtCuotas.setManaged(true);
      txtCuotas.setText(String.valueOf(m.getCuotas()));

      // Cargamos los comercios y seleccionamos el actual
      cargarComercios(m.getComercioId());

    } else {
      // PAGO — ocultamos ComboBox y cuotas
      lblComercio.setVisible(false);
      lblComercio.setManaged(false);
      cmbComercio.setVisible(false);
      cmbComercio.setManaged(false);
      lblCuotas.setVisible(false);
      lblCuotas.setManaged(false);
      txtCuotas.setVisible(false);
      txtCuotas.setManaged(false);

      // Mostramos descripción libre
      lblDescripcion.setVisible(true);
      lblDescripcion.setManaged(true);
      txtDescripcion.setVisible(true);
      txtDescripcion.setManaged(true);
      txtDescripcion.setText(m.getDescripcion() != null ? m.getDescripcion() : "");
    }
  }

  // --- Carga comercios y selecciona el del movimiento actual ---

  private void cargarComercios(int comercioIdActual) {
    try {
      ComercioDao dao = new ComercioDao();
      List<Comercio> comercios = dao.findAllActivos();
      cmbComercio.getItems().clear();
      cmbComercio.getItems().addAll(comercios);

      // Seleccionamos el comercio actual del movimiento
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
    String montoStr  = txtMonto.getText().trim();
    if (montoStr.isEmpty() || dpFecha.getValue() == null) {
      Toast.show(getStage(), "Fecha y monto son obligatorios");
      return;
    }

    try {
      BigDecimal monto = new BigDecimal(montoStr).setScale(2);
      movimientoActual.setFecha(dpFecha.getValue());
      movimientoActual.setMonto(monto);

      if ("EGRESO".equals(movimientoActual.getCategoria())) {
        // Validamos que haya un comercio seleccionado
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
        // PAGO — descripción libre
        String descripcion = txtDescripcion.getText().trim();
        if (descripcion.isEmpty()) {
          Toast.show(getStage(), "Debe ingresar una descripción");
          return;
        }
        movimientoActual.setDescripcion(descripcion.toUpperCase());
      }

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