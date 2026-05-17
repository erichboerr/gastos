package ar.com.gastos.controller;

import ar.com.gastos.dao.IngresoDao;
import ar.com.gastos.util.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class IngresoController {

  private static final Logger logger = LoggerFactory.getLogger(IngresoController.class);

  @FXML
  private TextField txtTipoIngreso;
  @FXML
  private TextField txtMonto;

  @FXML
  private void guardarIngreso() {
    String tipo = txtTipoIngreso.getText();
    String montoStr = txtMonto.getText();

    if (tipo == null || tipo.isEmpty() || montoStr == null || montoStr.isEmpty()) {
      Toast.show((Stage) txtTipoIngreso.getScene().getWindow(), "Debe completar todos los campos");
      return;
    }

    try {
      BigDecimal monto = new BigDecimal(montoStr).setScale(2);

      IngresoDao ingresoDao = new IngresoDao();
      ingresoDao.insertarIngreso(tipo, monto, LocalDate.now());

      Toast.show((Stage) txtTipoIngreso.getScene().getWindow(), "Ingreso registrado");
      logger.info("Ingreso registrado: {} - ${}", tipo, monto);

      ((Stage) txtTipoIngreso.getScene().getWindow()).close();

    } catch (NumberFormatException ex) {
      Toast.show((Stage) txtTipoIngreso.getScene().getWindow(), "Monto inválido");
      logger.error("Error al parsear monto", ex);

    } catch (SQLException ex) {   // 🔹 capturamos SQLException
      Toast.show((Stage) txtTipoIngreso.getScene().getWindow(), "Error al guardar ingreso");
      logger.error("Error al guardar ingreso", ex);
    }

  }
}
