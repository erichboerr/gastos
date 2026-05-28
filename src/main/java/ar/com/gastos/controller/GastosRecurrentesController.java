package ar.com.gastos.controller;

import ar.com.gastos.dao.GastoRecurrenteDao;
import ar.com.gastos.model.GastoRecurrente;
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

public class GastosRecurrentesController {

  private static final Logger logger = LoggerFactory.getLogger(GastosRecurrentesController.class);

  @FXML private TextField        txtDescripcion;
  @FXML private TextField        txtCategoria;
  @FXML private ComboBox<String> cmbMedioPago;
  @FXML private TableView<GastoRecurrente>        tablaRecurrentes;
  @FXML private TableColumn<GastoRecurrente, String> colDescripcion;
  @FXML private TableColumn<GastoRecurrente, String> colCategoria;
  @FXML private TableColumn<GastoRecurrente, String> colMedioPago;
  @FXML private TableColumn<GastoRecurrente, Void>   colAcciones;

  // Cuando estamos editando, guardamos el id aquí. -1 = modo alta.
  private int idEditando = -1;

  @FXML
  public void initialize() {
    // Opciones fijas de medio de pago
    cmbMedioPago.getItems().addAll("DEBITO", "CREDITO", "EFECTIVO");
    cmbMedioPago.setValue("DEBITO");

    configurarTabla();
    cargarTabla();
  }

  // --- Configuración de columnas ---

  private void configurarTabla() {
    colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
    colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
    colMedioPago.setCellValueFactory(new PropertyValueFactory<>("medioPago"));

    // Columna de acciones: Editar + Eliminar por fila
    colAcciones.setCellFactory(col -> new TableCell<>() {
      private final Button btnEditar   = new Button("Editar");
      private final Button btnEliminar = new Button("Eliminar");
      private final HBox   hbox        = new HBox(6, btnEditar, btnEliminar);

      {
        btnEditar.setStyle("-fx-background-color:#2c3e50; -fx-text-fill:white; -fx-font-size:11;");
        btnEliminar.setStyle("-fx-background-color:#c0392b; -fx-text-fill:white; -fx-font-size:11;");

        btnEditar.setOnAction(e -> {
          GastoRecurrente g = getTableView().getItems().get(getIndex());
          cargarEnFormulario(g);
        });

        btnEliminar.setOnAction(e -> {
          GastoRecurrente g = getTableView().getItems().get(getIndex());
          confirmarEliminar(g);
        });
      }

      @Override
      protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : hbox);
      }
    });
  }

  // --- Carga la tabla desde la DB ---

  private void cargarTabla() {
    try {
      GastoRecurrenteDao dao = new GastoRecurrenteDao();
      List<GastoRecurrente> lista = dao.findAll();
      tablaRecurrentes.setItems(FXCollections.observableArrayList(lista));
    } catch (SQLException ex) {
      logger.error("Error al cargar gastos recurrentes", ex);
    }
  }

  // --- Alta / Modificación ---

  @FXML
  private void guardar() {
    String descripcion = txtDescripcion.getText().trim();
    String categoria   = txtCategoria.getText().trim();
    String medioPago   = cmbMedioPago.getValue();

    if (descripcion.isEmpty() || medioPago == null) {
      Toast.show(getStage(), "Descripción y medio de pago son obligatorios");
      return;
    }

    try {
      GastoRecurrenteDao dao = new GastoRecurrenteDao();

      if (idEditando == -1) {
        // Modo alta
        GastoRecurrente nuevo = new GastoRecurrente(descripcion, categoria, medioPago);
        dao.save(nuevo);
        logger.info("Gasto recurrente creado: {}", descripcion);
        Toast.show(getStage(), "Recurrente guardado");
      } else {
        // Modo edición
        GastoRecurrente editado = new GastoRecurrente(descripcion, categoria, medioPago);
        editado.setId(idEditando);
        dao.update(editado);
        logger.info("Gasto recurrente actualizado: {}", descripcion);
        Toast.show(getStage(), "Recurrente actualizado");
      }

      limpiar();
      cargarTabla();

    } catch (SQLException ex) {
      Toast.show(getStage(), "Error al guardar");
      logger.error("Error al guardar gasto recurrente", ex);
    }
  }

  // --- Carga el seleccionado en el formulario para editar ---

  private void cargarEnFormulario(GastoRecurrente g) {
    idEditando = g.getId();
    txtDescripcion.setText(g.getDescripcion());
    txtCategoria.setText(g.getCategoria() != null ? g.getCategoria() : "");
    cmbMedioPago.setValue(g.getMedioPago());
  }

  // --- Eliminar con confirmación ---

  private void confirmarEliminar(GastoRecurrente g) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Confirmar eliminación");
    alert.setHeaderText("¿Eliminar recurrente?");
    alert.setContentText(g.getDescripcion());

    alert.showAndWait().ifPresent(respuesta -> {
      if (respuesta == ButtonType.OK) {
        try {
          GastoRecurrenteDao dao = new GastoRecurrenteDao();
          dao.delete(g.getId());
          logger.info("Gasto recurrente eliminado: {}", g.getDescripcion());
          Toast.show(getStage(), "Recurrente eliminado");
          limpiar();
          cargarTabla();
        } catch (SQLException ex) {
          Toast.show(getStage(), "Error al eliminar");
          logger.error("Error al eliminar gasto recurrente", ex);
        }
      }
    });
  }

  // --- Limpia el formulario y vuelve a modo alta ---

  @FXML
  private void limpiar() {
    idEditando = -1;
    txtDescripcion.clear();
    txtCategoria.clear();
    cmbMedioPago.setValue("DEBITO");
  }

  @FXML
  private void cerrar() {
    getStage().close();
  }

  private Stage getStage() {
    return (Stage) txtDescripcion.getScene().getWindow();
  }
}