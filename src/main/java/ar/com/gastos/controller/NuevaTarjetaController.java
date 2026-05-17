package ar.com.gastos.controller;

import ar.com.gastos.dao.TarjetaDao;
import ar.com.gastos.model.Tarjeta;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
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
      // Editable: el usuario puede escribir un tipo que no esté en la lista
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
      Toast.show((Stage) txtNombre.getScene().getWindow(), "Ingresá el nombre de la tarjeta");
      return;
    }

    String tipo = cmbTipo.getValue();
    if (tipo == null || tipo.isBlank()) {
      Toast.show((Stage) txtNombre.getScene().getWindow(), "Seleccioná o ingresá el tipo de tarjeta");
      return;
    }

    try {
      TarjetaDao dao = new TarjetaDao();

      // Verificamos que no exista ya una tarjeta con ese nombre
      // para evitar duplicados silenciosos.
      Tarjeta existente = dao.findByNombre(nombre.toUpperCase().trim());
      if (existente != null) {
        Toast.show((Stage) txtNombre.getScene().getWindow(),
            "Ya existe una tarjeta con ese nombre");
        return;
      }

      // habilitado = true por defecto al crear
      Tarjeta nueva = new Tarjeta(0, nombre, tipo, true);
      dao.save(nueva);

      // Notificamos al EventBus para que el dashboard se recargue
      // y muestre la nueva tarjeta inmediatamente.
      MovimientoEventBus.publish(nombre);

      Toast.show((Stage) txtNombre.getScene().getWindow(),
          "Tarjeta guardada correctamente");

      logger.info("Nueva tarjeta creada: {} ({})", nombre.toUpperCase().trim(), tipo.toUpperCase().trim());

      // Limpiamos el formulario para permitir cargar otra tarjeta
      txtNombre.clear();
      cmbTipo.setValue(null);
      cargarTipos();

    } catch (SQLException e) {
      Toast.show((Stage) txtNombre.getScene().getWindow(), "Error al guardar la tarjeta");
      logger.error("Error al guardar tarjeta", e);
    }
  }
}
