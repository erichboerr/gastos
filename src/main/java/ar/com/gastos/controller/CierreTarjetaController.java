package ar.com.gastos.controller;

import ar.com.gastos.dao.CierreTarjetaDao;
import ar.com.gastos.dao.TarjetaDao;
import ar.com.gastos.model.CierreTarjeta;
import ar.com.gastos.model.Tarjeta;
import ar.com.gastos.util.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CierreTarjetaController {

  private static final Logger logger = LoggerFactory.getLogger(EgresoController.class);

  @FXML private ComboBox<String> cmbTarjeta;
  @FXML private DatePicker dpMes;
  @FXML private DatePicker dpCierre;
  @FXML private DatePicker dpVencimiento;

  @FXML
  public void initialize() {
    try {
      TarjetaDao tarjetaDao = new TarjetaDao();
      List<Tarjeta> tarjetas = tarjetaDao.findAllActivas();
      for (Tarjeta t : tarjetas) {
        cmbTarjeta.getItems().add(t.getNombre());
      }
    } catch (Exception e) {
      logger.error("Error al inicializar CierreController", e);
    }
  }

  @FXML
  private void guardarCierre() {
    try {
      String tarjetaNombre = cmbTarjeta.getValue();
      if (tarjetaNombre == null) {
        Toast.show((Stage) dpMes.getScene().getWindow(), "Debe seleccionar una tarjeta");
        return;
      }

      LocalDate mes = dpMes.getValue();
      LocalDate cierre = dpCierre.getValue();
      LocalDate vencimiento = dpVencimiento.getValue();

      TarjetaDao tarjetaDao = new TarjetaDao();
      int tarjetaId = tarjetaDao.findByNombre(tarjetaNombre).getId();

      CierreTarjeta cierreTarjeta = new CierreTarjeta(tarjetaId, mes, cierre, vencimiento);
      CierreTarjetaDao dao = new CierreTarjetaDao();
      dao.save(cierreTarjeta);

      Toast.show((Stage) dpMes.getScene().getWindow(), "Cierre guardado correctamente");
    } catch (SQLException ex) {
      Toast.show((Stage) dpMes.getScene().getWindow(), "Error al guardar cierre");
      logger.info("Cierre guardado correctamente", ex);
    }
  }
}
