package ar.com.gastos.controller;

import ar.com.gastos.util.BackupService;
import ar.com.gastos.util.MovimientoEventBus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class RestaurarBackupController {

  private static final Logger logger = LoggerFactory.getLogger(RestaurarBackupController.class);

  @FXML
  private ListView<String> lstBackups;
  @FXML
  private Button btnRestaurar;

  @FXML
  public void initialize() {
    // Primero registramos el listener
    btnRestaurar.setDisable(true);
    lstBackups.getSelectionModel().selectedItemProperty().addListener(
        (obs, anterior, nuevo) -> btnRestaurar.setDisable(nuevo == null)
    );

    // Después cargamos la lista — así el selectFirst() dispara el listener
    cargarLista();
  }

  // --- Carga la lista de backups disponibles ---

  private void cargarLista() {
    try {
      List<File> backups = BackupService.listarBackups();
      if (backups.isEmpty()) {
        lstBackups.setPlaceholder(new Label("No hay backups disponibles"));
        return;
      }
      // Mostramos solo el nombre del archivo
      lstBackups.setItems(FXCollections.observableArrayList(
          backups.stream().map(File::getName).toList()
      ));

      // Seleccionamos el primero automáticamente y habilitamos el botón
      lstBackups.getSelectionModel().selectFirst();

    } catch (Exception ex) {
      logger.error("Error al listar backups", ex);
    }
  }

  // --- Restaurar el seleccionado ---

  @FXML
  private void restaurar() {
    String seleccionado = lstBackups.getSelectionModel().getSelectedItem();
    if (seleccionado == null) return;

    // Confirmación con mensaje claro
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("⚠ Confirmar restauración");
    alert.setHeaderText("Esta acción es irreversible");
    alert.setContentText(
        "Se borrarán TODOS los datos actuales y se reemplazarán\n" +
            "con los del backup:\n\n" +
            "📁 " + seleccionado + "\n\n" +
            "¿Estás seguro de que querés continuar?"
    );

    ButtonType btnSi = new ButtonType("Sí, restaurar", ButtonBar.ButtonData.OK_DONE);
    ButtonType btnNo = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
    alert.getButtonTypes().setAll(btnSi, btnNo);

    Optional<ButtonType> respuesta = alert.showAndWait();
    if (respuesta.isEmpty() || respuesta.get() != btnSi) return;

    try {
      // Obtenemos el path completo
      List<File> backups = BackupService.listarBackups();
      File archivo = backups.stream()
          .filter(f -> f.getName().equals(seleccionado))
          .findFirst()
          .orElseThrow(() -> new Exception("Archivo no encontrado"));

      // Deshabilitamos el botón mientras restaura
      btnRestaurar.setDisable(true);
      btnRestaurar.setText("Restaurando...");

      BackupService.restaurarBackup(archivo.getAbsolutePath());

      // Notificamos al dashboard para que recargue
      MovimientoEventBus.publish("restauracion");

      mostrarExito(seleccionado);
      getStage().close();

    } catch (Exception ex) {
      btnRestaurar.setDisable(false);
      btnRestaurar.setText("Restaurar");
      mostrarError(ex.getMessage());
      logger.error("Error al restaurar backup", ex);
    }
  }

  @FXML
  private void cerrar() {
    getStage().close();
  }

  private void mostrarExito(String archivo) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Restauración completada");
    alert.setHeaderText("Base de datos restaurada correctamente");
    alert.setContentText("Se restauró el backup:\n" + archivo);
    alert.showAndWait();
  }

  private void mostrarError(String mensaje) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Error al restaurar");
    alert.setHeaderText("No se pudo restaurar el backup");
    alert.setContentText(mensaje);
    alert.showAndWait();
  }

  private Stage getStage() {
    return (Stage) lstBackups.getScene().getWindow();
  }
}