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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class IngresoController {

  private static final Logger logger = LoggerFactory.getLogger(IngresoController.class);

  @FXML
  private ComboBox<String> cmbTipoIngreso;
  @FXML
  private DatePicker dpFecha;
  @FXML
  private TextField txtMonto;
  @FXML
  private Label lblMes;
  @FXML
  private Label lblTotalMes;
  @FXML
  private TableView<Ingreso> tablaIngresos;
  @FXML
  private TableColumn<Ingreso, LocalDate> colFecha;
  @FXML
  private TableColumn<Ingreso, String> colTipo;
  @FXML
  private TableColumn<Ingreso, BigDecimal> colMonto;
  @FXML
  private TableColumn<Ingreso, Void> colAcciones;

  // Mes navegable
  private YearMonth mesVisible = YearMonth.now();

  // -1 = modo alta
  private int idEditando = -1;

  private static final NumberFormat CURRENCY =
      NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

  static {
    CURRENCY.setMaximumFractionDigits(2);
  }

  @FXML
  public void initialize() {
    cmbTipoIngreso.getItems().addAll("Sueldo Erich", "Sueldo Lorena", "Lucía", "Frasco");
    dpFecha.setValue(LocalDate.now());
    configurarTabla();
    cargarTabla();
  }

  // --- Navegación por mes ---

  @FXML
  private void meAnterior() {
    mesVisible = mesVisible.minusMonths(1);
    cargarTabla();
  }

  @FXML
  private void meSiguiente() {
    mesVisible = mesVisible.plusMonths(1);
    cargarTabla();
  }

  // --- Configuración de columnas ---

  private void configurarTabla() {
    colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
    colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
    colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    colFecha.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(LocalDate fecha, boolean empty) {
        super.updateItem(fecha, empty);
        setText(empty || fecha == null ? null : formatter.format(fecha));
      }
    });

    colMonto.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(BigDecimal monto, boolean empty) {
        super.updateItem(monto, empty);
        setText(empty || monto == null ? null : CURRENCY.format(monto));
      }
    });

    colAcciones.setCellFactory(col -> new TableCell<>() {
      private final Button btnEditar = new Button("Editar");
      private final Button btnEliminar = new Button("Eliminar");
      private final HBox hbox = new HBox(6, btnEditar, btnEliminar);

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

  // --- Carga la tabla filtrada por mes visible ---

  private void cargarTabla() {
    // Actualizamos el label del mes
    String nombreMes = mesVisible.getMonth()
        .getDisplayName(TextStyle.FULL, new Locale("es"));
    nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
    lblMes.setText(nombreMes + " " + mesVisible.getYear());

    try {
      IngresoDao dao = new IngresoDao();
      List<Ingreso> todos = dao.listarIngresos();

      // Filtramos por mes visible
      List<Ingreso> delMes = todos.stream()
          .filter(i -> YearMonth.from(i.getFecha()).equals(mesVisible))
          .collect(Collectors.toList());

      // Total del mes
      BigDecimal total = delMes.stream()
          .map(Ingreso::getMonto)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      lblTotalMes.setText("Total: " + CURRENCY.format(total));
      tablaIngresos.setItems(FXCollections.observableArrayList(delMes));

    } catch (SQLException ex) {
      logger.error("Error al cargar ingresos", ex);
    }
  }

  // --- Alta / Modificación ---

  @FXML
  private void guardarIngreso() {
    String tipo = cmbTipoIngreso.getEditor().getText().trim();
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
        dao.insertarIngreso(tipo, monto, fecha);
        logger.info("Ingreso registrado: {} - ${} - {}", tipo, monto, fecha);
        Toast.show(getStage(), "Ingreso registrado");
      } else {
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
    } catch (SQLException ex) {
      Toast.show(getStage(), "Error al guardar ingreso");
      logger.error("Error al guardar ingreso", ex);
    }
  }

  private void cargarEnFormulario(Ingreso i) {
    idEditando = i.getId();
    cmbTipoIngreso.setValue(i.getTipo());
    dpFecha.setValue(i.getFecha());
    txtMonto.setText(i.getMonto().toPlainString());
  }

  private void confirmarEliminar(Ingreso i) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Confirmar eliminación");
    alert.setHeaderText("¿Eliminar ingreso?");
    alert.setContentText(i.getTipo() + " — " + CURRENCY.format(i.getMonto()) + " — " + i.getFecha());

    alert.showAndWait().ifPresent(respuesta -> {
      if (respuesta == ButtonType.OK) {
        try {
          new IngresoDao().delete(i.getId());
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

  /**
   * Llamado desde DashboardController para precargar el mes visible
   */
  public void setMesVisible(YearMonth mes) {
    this.mesVisible = mes;
    cargarTabla();
  }

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