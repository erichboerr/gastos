package ar.com.gastos.controller;

import ar.com.gastos.dao.TarjetaDao;
import ar.com.gastos.model.Tarjeta;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class EditarTarjetaController {

  private static final Logger logger = LoggerFactory.getLogger(EditarTarjetaController.class);

  @FXML
  private TextField txtNombre;
  @FXML
  private ComboBox<String> cmbTipo;

  // Tarjeta que se está editando — se setea desde DashboardController
  private Tarjeta tarjetaActual;

  /**
   * Recibe la tarjeta seleccionada y precarga los campos del formulario.
   * Llamado desde DashboardController al abrir esta ventana.
   */
  public void setTarjeta(Tarjeta t) {
    this.tarjetaActual = t;
    txtNombre.setText(t.getNombre());
    cmbTipo.setValue(t.getTipo());
  }

  @FXML
  public void initialize() {
    // Tipos predefinidos — editables por el usuario
    try {
      TarjetaDao dao = new TarjetaDao();
      cmbTipo.getItems().addAll(dao.findTiposDistintos());
    } catch (SQLException ex) {
      logger.error("Error al cargar tipos de tarjeta", ex);
    }
  }

  // --- Guardar cambios de nombre y tipo ---

  @FXML
  private void guardar() {
    String nombre = txtNombre.getText().trim();
    String tipo = cmbTipo.getEditor().getText().trim();

    if (nombre.isEmpty() || tipo.isEmpty()) {
      Toast.show(getStage(), "Nombre y tipo son obligatorios");
      return;
    }

    try {
      tarjetaActual.setNombre(nombre);
      tarjetaActual.setTipo(tipo);

      TarjetaDao dao = new TarjetaDao();
      dao.update(tarjetaActual);

      logger.info("Tarjeta {} actualizada: nombre={} tipo={}", tarjetaActual.getId(), nombre, tipo);

      // Notificamos al dashboard para que recargue las cards
      MovimientoEventBus.publish("tarjeta-editada");

      Toast.show(getStage(), "Tarjeta actualizada");
      getStage().close();

    } catch (SQLException ex) {
      Toast.show(getStage(), "Error al guardar cambios");
      logger.error("Error al actualizar tarjeta id {}", tarjetaActual.getId(), ex);
    }
  }

  // --- Baja soft con confirmación ---

  @FXML
  private void darDeBaja() {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Confirmar baja");
    alert.setHeaderText("¿Dar de baja la tarjeta?");
    alert.setContentText("\"" + tarjetaActual.getNombre() + "\"\n"
        + "Ya no aparecerá en el dashboard ni podrá usarse para nuevos movimientos.");

    alert.showAndWait().ifPresent(respuesta -> {
      if (respuesta == ButtonType.OK) {
        try {
          TarjetaDao dao = new TarjetaDao();
          dao.darDeBaja(tarjetaActual.getId());

          logger.info("Tarjeta dada de baja: {} - id {}", tarjetaActual.getNombre(), tarjetaActual.getId());

          // Notificamos al dashboard para que quite la card
          MovimientoEventBus.publish("tarjeta-baja");

          Toast.show(getStage(), "Tarjeta dada de baja");
          getStage().close();

        } catch (SQLException ex) {
          Toast.show(getStage(), "Error al dar de baja");
          logger.error("Error al dar de baja tarjeta id {}", tarjetaActual.getId(), ex);
        }
      }
    });
  }

  private Stage getStage() {
    return (Stage) txtNombre.getScene().getWindow();
  }
}