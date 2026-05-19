package ar.com.gastos.controller;

import ar.com.gastos.dao.TarjetaDao;
import ar.com.gastos.model.Tarjeta;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class NuevaTarjetaController {

  private static final Logger logger = LoggerFactory.getLogger(NuevaTarjetaController.class);

  @FXML private TextField txtNombre;
  @FXML private ComboBox<String> cmbTipo;

  @FXML
  public void initialize() {
    cargarTipos();
  }

  // Carga los tipos de tarjeta ya existentes en la base.
  // Si la base está vacía, el combo queda vacío y el usuario
  // puede escribir directamente o usar el botón + Nuevo.
  private void cargarTipos() {
    try {
      TarjetaDao dao = new TarjetaDao();
      List<String> tipos = dao.findTiposDistintos();
      cmbTipo.getItems().clear();
      cmbTipo.getItems().addAll(tipos);
      cmbTipo.setEditable(true);
    } catch (SQLException e) {
      logger.error("Error al cargar tipos de tarjeta", e);
    }
  }

  // Abre un diálogo para ingresar un tipo nuevo que no existe todavía.
  // Mismo comportamiento que el botón + Nueva en egresos.
  @FXML
  private void nuevoTipo() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Nuevo tipo");
    dialog.setHeaderText(null);
    dialog.setContentText("Ingresá el tipo de tarjeta:");

    Optional<String> resultado = dialog.showAndWait();
    resultado.ifPresent(texto -> {
      String nuevo = texto.toUpperCase().trim();
      if (!nuevo.isEmpty()) {
        if (!cmbTipo.getItems().contains(nuevo)) {
          cmbTipo.getItems().add(nuevo);
        }
        cmbTipo.setValue(nuevo);
      }
    });
  }

  @FXML
  private void guardarTarjeta() {
    String nombre = txtNombre.getText();
    if (nombre == null || nombre.isBlank()) {
      Toast.show(getStage(), "Ingresá el nombre de la tarjeta");
      return;
    }

    String tipo = cmbTipo.getValue();
    if (tipo == null || tipo.isBlank()) {
      Toast.show(getStage(), "Seleccioná o ingresá el tipo de tarjeta");
      return;
    }

    try {
      TarjetaDao dao = new TarjetaDao();
      String nombreNormalizado = nombre.toUpperCase().trim();

      // Buscamos si ya existe una tarjeta con ese nombre, activa o no
      Tarjeta existente = dao.findByNombreIgnorandoBaja(nombreNormalizado);

      if (existente != null) {
        // Armamos el mensaje según si está activa o dada de baja
        String estado = existente.getHabilitado() ? "activa" : "dada de baja";
        String mensaje = "Ya existe una tarjeta con el nombre '" + existente.getNombre() + "' (" + estado + ").\n"
            + "¿Querés crear una nueva de todas formas o activar la existente?";

        // Botones personalizados
        ButtonType btnCrear   = new ButtonType("Crear nueva");
        ButtonType btnActivar = new ButtonType("Activar existente");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Tarjeta duplicada");
        alert.setHeaderText("Tarjeta existente encontrada");
        alert.setContentText(mensaje);
        alert.getButtonTypes().setAll(btnCrear, btnActivar, btnCancelar);

        Optional<ButtonType> respuesta = alert.showAndWait();

        if (respuesta.isEmpty() || respuesta.get() == btnCancelar) {
          // No hace nada — el usuario canceló
          return;
        }

        if (respuesta.get() == btnActivar) {
          // Reactivamos la tarjeta existente
          dao.reactivar(existente.getId());
          logger.info("Tarjeta reactivada: {} - id {}", existente.getNombre(), existente.getId());
          MovimientoEventBus.publish("tarjeta-reactivada");
          Toast.show(getStage(), "Tarjeta '" + existente.getNombre() + "' reactivada");
          limpiarFormulario();
          return;
        }

        // Si llegamos acá el usuario eligió "Crear nueva" — continuamos el flujo normal
      }

      // --- Alta de tarjeta nueva ---
      Tarjeta nueva = new Tarjeta(0, nombreNormalizado, tipo, true);
      dao.save(nueva);

      logger.info("Nueva tarjeta creada: {} ({})", nombreNormalizado, tipo.toUpperCase().trim());
      MovimientoEventBus.publish("tarjeta-nueva");
      Toast.show(getStage(), "Tarjeta guardada correctamente");

      limpiarFormulario();

    } catch (SQLException e) {
      Toast.show(getStage(), "Error al guardar la tarjeta");
      logger.error("Error al guardar tarjeta", e);
    }
  }

  // Limpia el formulario para permitir cargar otra tarjeta sin cerrar la ventana
  private void limpiarFormulario() {
    txtNombre.clear();
    cmbTipo.setValue(null);
    cargarTipos();
  }

  private Stage getStage() {
    return (Stage) txtNombre.getScene().getWindow();
  }
}
