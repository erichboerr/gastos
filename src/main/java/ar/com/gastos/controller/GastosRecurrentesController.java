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

  @FXML
  private TextField txtDescripcion;
  @FXML
  private TextField txtCategoria;
  @FXML
  private TableView<GastoRecurrente> tablaRecurrentes;
  @FXML
  private TableColumn<GastoRecurrente, String> colDescripcion;
  @FXML
  private TableColumn<GastoRecurrente, String> colCategoria;
  @FXML
  private TableColumn<GastoRecurrente, Void> colAcciones;

  // Medio de pago siempre es DEBITO — no se expone en la UI
  private static final String MEDIO_PAGO = "DEBITO";

  private int idEditando = -1;

  @FXML
  public void initialize() {
    configurarTabla();
    cargarTabla();
  }

  private void configurarTabla() {
    colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
    colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));

    colAcciones.setCellFactory(col -> new TableCell<>() {
      private final Button btnEditar = new Button("Editar");
      private final Button btnEliminar = new Button("Eliminar");
      private final HBox hbox = new HBox(6, btnEditar, btnEliminar);

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

  private void cargarTabla() {
    try {
      GastoRecurrenteDao dao = new GastoRecurrenteDao();
      List<GastoRecurrente> lista = dao.findAll();
      tablaRecurrentes.setItems(FXCollections.observableArrayList(lista));
    } catch (SQLException ex) {
      logger.error("Error al cargar gastos recurrentes", ex);
    }
  }

  @FXML
  private void guardar() {
    String descripcion = txtDescripcion.getText().toUpperCase().trim();
    String categoria = txtCategoria.getText().trim();

    if (descripcion.isEmpty()) {
      Toast.show(getStage(), "La descripción es obligatoria");
      return;
    }

    try {
      GastoRecurrenteDao dao = new GastoRecurrenteDao();

      if (idEditando == -1) {
        GastoRecurrente nuevo = new GastoRecurrente(descripcion, categoria, MEDIO_PAGO);
        dao.save(nuevo);
        logger.info("Gasto recurrente creado: {}", descripcion);
        Toast.show(getStage(), "Recurrente guardado");
      } else {
        GastoRecurrente editado = new GastoRecurrente(descripcion, categoria, MEDIO_PAGO);
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

  private void cargarEnFormulario(GastoRecurrente g) {
    idEditando = g.getId();
    txtDescripcion.setText(g.getDescripcion());
    txtCategoria.setText(g.getCategoria() != null ? g.getCategoria() : "");
  }

  private void confirmarEliminar(GastoRecurrente g) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Confirmar eliminación");
    alert.setHeaderText("¿Eliminar recurrente?");
    alert.setContentText(g.getDescripcion());

    alert.showAndWait().ifPresent(respuesta -> {
      if (respuesta == ButtonType.OK) {
        try {
          new GastoRecurrenteDao().delete(g.getId());
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

  @FXML
  private void limpiar() {
    idEditando = -1;
    txtDescripcion.clear();
    txtCategoria.clear();
  }

  @FXML
  private void cerrar() {
    getStage().close();
  }

  private Stage getStage() {
    return (Stage) txtDescripcion.getScene().getWindow();
  }
}