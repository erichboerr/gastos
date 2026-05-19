package ar.com.gastos.controller;

import ar.com.gastos.dao.MovimientoDao;
import ar.com.gastos.model.Movimiento;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;

public class EditarMovimientoController {

  private static final Logger logger = LoggerFactory.getLogger(EditarMovimientoController.class);

  @FXML private DatePicker dpFecha;
  @FXML private TextField txtDescripcion;
  @FXML private TextField txtMonto;
  @FXML private TextField txtCuotas;

  // Movimiento que se está editando — se setea desde DetalleController
  private Movimiento movimientoActual;

  /**
   * Recibe el movimiento seleccionado y precarga los campos del formulario.
   * Llamado desde DetalleController al abrir esta ventana.
   */
  public void setMovimiento(Movimiento m) {
    this.movimientoActual = m;
    dpFecha.setValue(m.getFecha());
    txtDescripcion.setText(m.getDescripcion());
    txtMonto.setText(m.getMonto().toPlainString());
    txtCuotas.setText(String.valueOf(m.getCuotas()));
  }

  // --- Guardar cambios ---

  @FXML
  private void guardar() {
    String descripcion = txtDescripcion.getText().trim();
    String montoStr    = txtMonto.getText().trim();
    String cuotasStr   = txtCuotas.getText().trim();

    // Validación de campos obligatorios
    if (descripcion.isEmpty() || montoStr.isEmpty() || cuotasStr.isEmpty()
        || dpFecha.getValue() == null) {
      Toast.show(getStage(), "Todos los campos son obligatorios");
      return;
    }

    try {
      BigDecimal monto  = new BigDecimal(montoStr).setScale(2);
      int cuotas        = Integer.parseInt(cuotasStr);

      if (cuotas < 1) {
        Toast.show(getStage(), "Las cuotas deben ser al menos 1");
        return;
      }

      // Actualizamos el objeto con los nuevos valores
      movimientoActual.setFecha(dpFecha.getValue());
      movimientoActual.setDescripcion(descripcion.toUpperCase());
      movimientoActual.setMonto(monto);
      movimientoActual.setCuotas(cuotas);

      MovimientoDao dao = new MovimientoDao();
      dao.update(movimientoActual);

      logger.info("Movimiento {} actualizado: {} - ${} - {} cuotas",
          movimientoActual.getId(), descripcion, monto, cuotas);

      // Notificamos al dashboard y al detalle para que recarguen
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
    return (Stage) txtDescripcion.getScene().getWindow();
  }
}