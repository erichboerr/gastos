package ar.com.gastos.controller;

import ar.com.gastos.dao.IngresoDao;
import ar.com.gastos.model.Ingreso;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class IngresoController {

  private static final Logger logger = LoggerFactory.getLogger(IngresoController.class);

  @FXML private ComboBox<String> cmbTipoIngreso;
  @FXML private DatePicker dpFecha;
  @FXML private TextField txtMonto;
  @FXML private TableView<Ingreso> tablaIngresos;
  @FXML private TableColumn<Ingreso, LocalDate> colFecha;
  @FXML private TableColumn<Ingreso, String>    colTipo;
  @FXML private TableColumn<Ingreso, BigDecimal> colMonto;
  @FXML private TableColumn<Ingreso, Void>      colAcciones;

  // Cuando editamos un ingreso existente guardamos su id. -1 = modo alta.
  private int idEditando = -1;

  @FXML
  public void initialize() {
    // Tipos de ingreso predefinidos — editables por el usuario
    cmbTipoIngreso.getItems().addAll("Sueldo Erich", "Sueldo Lorena", "Lucía", "Frasco");
    dpFecha.setValue(LocalDate.now());

    configurarTabla();
    cargarTabla();
  }

  // --- Configuración de columnas de la tabla ---

  private void configurarTabla() {
    colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
    colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
    colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

    // Formato de fecha dd/MM/yyyy
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    colFecha.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(LocalDate fecha, boolean empty) {
        super.updateItem(fecha, empty);
        setText(empty || fecha == null ? null : formatter.format(fecha));
      }
    });

    // Formato de monto en pesos
    NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
    currency.setMaximumFractionDigits(2);
    colMonto.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(BigDecimal monto, boolean empty) {
        super.updateItem(monto, empty);
        setText(empty || monto == null ? null : currency.format(monto));
      }
    });

    // Columna de acciones: Editar + Eliminar por fila
    colAcciones.setCellFactory(col -> new TableCell<>() {
      private final Button btnEditar   = new Button("Editar");
      private final Button btnEliminar = new Button("Eliminar");
      private final HBox   hbox        = new HBox(6, btnEditar, btnEliminar);

      {
        btnEditar.setStyle("-fx-background-color:#2c3e50; -fx-text-fill:white; -fx-font-size:11;");
        btnEliminar.setStyle("-fx-background-color:#c0392b; -fx-text-fill:white; -fx-font-size:11;");

        btnEditar.setOnAction(e -> {
          Ingreso ingreso = getTableView().getItems().get(getIndex());
          cargarEnFormulario(ingreso);
        });

        btnEliminar.setOnAction(e -> {
          Ingreso ingreso = getTableView().getItems().get(getIndex());
          confirmarEliminar(ingreso);
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
      IngresoDao dao = new IngresoDao();
      List<Ingreso> ingresos = dao.listarIngresos();
      tablaIngresos.setItems(FXCollections.observableArrayList(ingresos));
    } catch (SQLException ex) {
      logger.error("Error al cargar ingresos", ex);
    }
  }

  // --- Alta / Modificación ---

  @FXML
  private void guardarIngreso() {
    String tipo     = cmbTipoIngreso.getEditor().getText().trim();
    String montoStr = txtMonto.getText().trim();
    LocalDate fecha = dpFecha.getValue();

    if (tipo.isEmpty() || montoStr.isEmpty() || fecha == null) {
      Toast.show(getStage(), "Debe completar todos los campos");
      return;
    }

    try {
      BigDecimal monto = new BigDecimal(montoStr).setScale(2);
      IngresoDao dao = new IngresoDao();

      if (idEditando == -1) {
        // Modo alta
        dao.insertarIngreso(tipo, monto, fecha);
        logger.info("Ingreso registrado: {} - ${} - {}", tipo, monto, fecha);
        Toast.show(getStage(), "Ingreso registrado");
      } else {
        // Modo edición
        Ingreso editado = new Ingreso(idEditando, fecha, tipo, monto, "ARS");
        dao.update(editado);
        logger.info("Ingreso actualizado id {}: {} - ${} - {}", idEditando, tipo, monto, fecha);
        Toast.show(getStage(), "Ingreso actualizado");
      }

      MovimientoEventBus.publish("ingreso");
      limpiar();
      cargarTabla();

    } catch (NumberFormatException ex) {
      Toast.show(getStage(), "Monto inválido");
      logger.error("Error al parsear monto", ex);
    } catch (SQLException ex) {
      Toast.show(getStage(), "Error al guardar ingreso");
      logger.error("Error al guardar ingreso", ex);
    }
  }

  // --- Carga el ingreso seleccionado en el formulario para editar ---

  private void cargarEnFormulario(Ingreso i) {
    idEditando = i.getId();
    cmbTipoIngreso.setValue(i.getTipo());
    dpFecha.setValue(i.getFecha());
    txtMonto.setText(i.getMonto().toPlainString());
  }

  // --- Eliminar con confirmación ---

  private void confirmarEliminar(Ingreso i) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Confirmar eliminación");
    alert.setHeaderText("¿Eliminar ingreso?");
    alert.setContentText(i.getTipo() + " — " + i.getMonto() + " — " + i.getFecha());

    alert.showAndWait().ifPresent(respuesta -> {
      if (respuesta == ButtonType.OK) {
        try {
          IngresoDao dao = new IngresoDao();
          dao.delete(i.getId());
          logger.info("Ingreso eliminado id {}", i.getId());
          MovimientoEventBus.publish("ingreso");
          Toast.show(getStage(), "Ingreso eliminado");
          cargarTabla();
        } catch (SQLException ex) {
          Toast.show(getStage(), "Error al eliminar");
          logger.error("Error al eliminar ingreso id {}", i.getId(), ex);
        }
      }
    });
  }

  // --- Limpia el formulario y vuelve a modo alta ---

  @FXML
  private void limpiar() {
    idEditando = -1;
    cmbTipoIngreso.setValue(null);
    dpFecha.setValue(LocalDate.now());
    txtMonto.clear();
  }

  @FXML
  private void cerrar() {
    getStage().close();
  }

  private Stage getStage() {
    return (Stage) txtMonto.getScene().getWindow();
  }
}