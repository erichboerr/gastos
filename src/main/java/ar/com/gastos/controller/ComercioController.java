package ar.com.gastos.controller;

import ar.com.gastos.dao.ComercioDao;
import ar.com.gastos.model.Comercio;
import ar.com.gastos.util.Toast;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class ComercioController {

  private static final Logger logger = LoggerFactory.getLogger(ComercioController.class);

  @FXML private TextField          txtNombre;
  @FXML private ComboBox<String>   cmbCategoria;
  @FXML private TableView<Comercio> tablaComercio;
  @FXML private TableColumn<Comercio, String>  colNombre;
  @FXML private TableColumn<Comercio, String>  colCategoria;
  @FXML private TableColumn<Comercio, String>  colEstado;
  @FXML private TableColumn<Comercio, Void>    colAcciones;

  // -1 = modo alta, >0 = modo edición
  private int idEditando = -1;

  @FXML
  public void initialize() {
    configurarTabla();
    cargarCategorias();
    cargarTabla();
  }

  // --- Configuración de columnas ---

  private void configurarTabla() {
    colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
    colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));

    // Columna estado — muestra "Activo" o "Inactivo" según habilitado
    colEstado.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || getTableRow() == null || getTableRow().getItem() == null) {
          setText(null);
          setStyle("");
          return;
        }
        Comercio c = (Comercio) getTableRow().getItem();
        if (c.isHabilitado()) {
          setText("Activo");
          setStyle("-fx-text-fill:#27ae60; -fx-font-weight:bold;");
        } else {
          setText("Inactivo");
          setStyle("-fx-text-fill:#c0392b; -fx-font-weight:bold;");
        }
      }
    });

    // Columna acciones — Editar + Dar de baja / Reactivar
    colAcciones.setCellFactory(col -> new TableCell<>() {
      private final Button btnEditar    = new Button("Editar");
      private final Button btnToggle    = new Button();
      private final HBox   hbox         = new HBox(6, btnEditar, btnToggle);

      {
        btnEditar.setStyle("-fx-background-color:#2c3e50; -fx-text-fill:white; -fx-font-size:11;");

        btnEditar.setOnAction(e -> {
          Comercio c = getTableView().getItems().get(getIndex());
          cargarEnFormulario(c);
        });

        btnToggle.setOnAction(e -> {
          Comercio c = getTableView().getItems().get(getIndex());
          toggleEstado(c);
        });
      }

      @Override
      protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || getTableRow() == null || getTableRow().getItem() == null) {
          setGraphic(null);
          return;
        }
        Comercio c = (Comercio) getTableRow().getItem();
        if (c.isHabilitado()) {
          btnToggle.setText("Dar de baja");
          btnToggle.setStyle("-fx-background-color:#c0392b; -fx-text-fill:white; -fx-font-size:11;");
        } else {
          btnToggle.setText("Reactivar");
          btnToggle.setStyle("-fx-background-color:#27ae60; -fx-text-fill:white; -fx-font-size:11;");
        }
        setGraphic(hbox);
      }
    });
  }

  // --- Carga las categorías existentes en el ComboBox ---

  private void cargarCategorias() {
    try {
      ComercioDao dao = new ComercioDao();
      List<String> categorias = dao.findCategoriasDistintas();
      cmbCategoria.getItems().clear();
      cmbCategoria.getItems().addAll(categorias);
    } catch (SQLException ex) {
      logger.error("Error al cargar categorías", ex);
    }
  }

  // --- Carga todos los comercios en la tabla (activos e inactivos) ---

  private void cargarTabla() {
    try {
      ComercioDao dao = new ComercioDao();
      List<Comercio> comercios = dao.findAll();
      tablaComercio.setItems(FXCollections.observableArrayList(comercios));
    } catch (SQLException ex) {
      logger.error("Error al cargar comercios", ex);
    }
  }

  // --- Alta / Modificación ---

  @FXML
  private void guardar() {
    String nombre    = txtNombre.getText().toUpperCase().trim();
    String categoria = cmbCategoria.getEditor().getText().toUpperCase().trim();

    if (nombre.isEmpty()) {
      Toast.show(getStage(), "El nombre es obligatorio");
      return;
    }

    try {
      ComercioDao dao = new ComercioDao();

      if (idEditando == -1) {
        // Modo alta — verificamos que no exista
        Comercio existente = dao.findByNombre(nombre);
        if (existente != null) {
          Toast.show(getStage(), "Ya existe un comercio con ese nombre");
          return;
        }
        Comercio nuevo = new Comercio(nombre, categoria.isEmpty() ? null : categoria);
        dao.save(nuevo);
        logger.info("Comercio creado: {} - {}", nombre, categoria);
        Toast.show(getStage(), "Comercio guardado");
      } else {
        // Modo edición
        Comercio editado = new Comercio(idEditando, nombre,
            categoria.isEmpty() ? null : categoria, true);
        dao.update(editado);
        logger.info("Comercio actualizado id {}: {} - {}", idEditando, nombre, categoria);
        Toast.show(getStage(), "Comercio actualizado");
      }

      limpiar();
      cargarCategorias();
      cargarTabla();

    } catch (SQLException ex) {
      Toast.show(getStage(), "Error al guardar comercio");
      logger.error("Error al guardar comercio", ex);
    }
  }

  // --- Carga el comercio en el formulario para editar ---

  private void cargarEnFormulario(Comercio c) {
    idEditando = c.getId();
    txtNombre.setText(c.getNombre());
    cmbCategoria.setValue(c.getCategoria());
  }

  // --- Baja soft / Reactivación ---

  private void toggleEstado(Comercio c) {
    String accion = c.isHabilitado() ? "dar de baja" : "reactivar";

    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Confirmar");
    alert.setHeaderText("¿Desea " + accion + " el comercio?");
    alert.setContentText(c.getNombre());

    alert.showAndWait().ifPresent(respuesta -> {
      if (respuesta == ButtonType.OK) {
        try {
          ComercioDao dao = new ComercioDao();
          if (c.isHabilitado()) {
            dao.darDeBaja(c.getId());
            logger.info("Comercio dado de baja: {}", c.getNombre());
            Toast.show(getStage(), "Comercio dado de baja");
          } else {
            dao.reactivar(c.getId());
            logger.info("Comercio reactivado: {}", c.getNombre());
            Toast.show(getStage(), "Comercio reactivado");
          }
          cargarTabla();
        } catch (SQLException ex) {
          Toast.show(getStage(), "Error al cambiar estado");
          logger.error("Error al cambiar estado del comercio", ex);
        }
      }
    });
  }

  // --- Limpia el formulario y vuelve a modo alta ---

  @FXML
  private void limpiar() {
    idEditando = -1;
    txtNombre.clear();
    cmbCategoria.setValue(null);
  }

  @FXML
  private void cerrar() {
    getStage().close();
  }

  private Stage getStage() {
    return (Stage) txtNombre.getScene().getWindow();
  }
}