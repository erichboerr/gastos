package ar.com.gastos.controller;

import ar.com.gastos.dao.IngresoDao;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class IngresoController {

  private static final Logger logger = LoggerFactory.getLogger(IngresoController.class);

  @FXML private ComboBox<String> cmbTipoIngreso;
  @FXML private DatePicker dpFecha;
  @FXML private TextField txtMonto;

  @FXML
  public void initialize() {
    // Tipos de ingreso predefinidos — editables por el usuario
    cmbTipoIngreso.getItems().addAll(
        "Sueldo Erich",
        "Sueldo Lorena",
        "Lucía",
        "Frasco"
    );
    // Fecha por defecto: hoy
    dpFecha.setValue(LocalDate.now());
  }

  @FXML
  private void guardarIngreso() {
    String tipo = cmbTipoIngreso.getEditor().getText().trim();
    String montoStr = txtMonto.getText();
    LocalDate fecha = dpFecha.getValue();

    if (tipo.isEmpty() || montoStr == null || montoStr.isEmpty() || fecha == null) {
      Toast.show((Stage) txtMonto.getScene().getWindow(), "Debe completar todos los campos");
      return;
    }

    try {
      BigDecimal monto = new BigDecimal(montoStr).setScale(2);

      IngresoDao ingresoDao = new IngresoDao();
      ingresoDao.insertarIngreso(tipo, monto, fecha);

      Toast.show((Stage) txtMonto.getScene().getWindow(), "Ingreso registrado");
      logger.info("Ingreso registrado: {} - ${} - {}", tipo, monto, fecha);

      MovimientoEventBus.publish("ingreso");

      ((Stage) txtMonto.getScene().getWindow()).close();

    } catch (NumberFormatException ex) {
      Toast.show((Stage) txtMonto.getScene().getWindow(), "Monto inválido");
      logger.error("Error al parsear monto", ex);

    } catch (SQLException ex) {
      Toast.show((Stage) txtMonto.getScene().getWindow(), "Error al guardar ingreso");
      logger.error("Error al guardar ingreso", ex);
    }
  }
}
