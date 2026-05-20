package ar.com.gastos.controller;

import ar.com.gastos.dao.CierreTarjetaDao;
import ar.com.gastos.dao.TarjetaDao;
import ar.com.gastos.model.CierreTarjeta;
import ar.com.gastos.model.Tarjeta;
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

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CierreTarjetaController {

  private static final Logger logger = LoggerFactory.getLogger(CierreTarjetaController.class);

  @FXML private ComboBox<String> cmbTarjeta;
  @FXML private DatePicker dpMes;
  @FXML private DatePicker dpCierre;
  @FXML private DatePicker dpVencimiento;
  @FXML private TableView<CierreTarjeta> tablaCierres;
  @FXML private TableColumn<CierreTarjeta, LocalDate> colMes;
  @FXML private TableColumn<CierreTarjeta, LocalDate> colFechaCierre;
  @FXML private TableColumn<CierreTarjeta, LocalDate> colFechaVenc;
  @FXML private TableColumn<CierreTarjeta, Void>      colAcciones;

  // Cuando editamos un cierre existente guardamos su idCierre. -1 = modo alta.
  private int idEditando = -1;

  // Tarjeta actualmente seleccionada en el combo
  private Tarjeta tarjetaSeleccionada = null;

  @FXML
  public void initialize() {
    cargarTarjetas();
    configurarTabla();
  }

  // --- Carga las tarjetas activas en el ComboBox ---

  private void cargarTarjetas() {
    try {
      TarjetaDao dao = new TarjetaDao();
      List<Tarjeta> tarjetas = dao.findAllActivas();
      cmbTarjeta.getItems().clear();
      for (Tarjeta t : tarjetas) {
        cmbTarjeta.getItems().add(t.getNombre());
      }
    } catch (Exception e) {
      logger.error("Error al cargar tarjetas", e);
    }
  }

  // --- Cuando se selecciona una tarjeta carga sus cierres en la tabla ---

  @FXML
  private void onTarjetaSeleccionada() {
    String nombre = cmbTarjeta.getValue();
    if (nombre == null) return;
    try {
      TarjetaDao dao = new TarjetaDao();
      tarjetaSeleccionada = dao.findByNombre(nombre);
      if (tarjetaSeleccionada != null) cargarTabla();
    } catch (SQLException ex) {
      logger.error("Error al buscar tarjeta", ex);
    }
  }

  // --- Configuración de columnas de la tabla ---

  private void configurarTabla() {
    colMes.setCellValueFactory(new PropertyValueFactory<>("mes"));
    colFechaCierre.setCellValueFactory(new PropertyValueFactory<>("fechaCierre"));
    colFechaVenc.setCellValueFactory(new PropertyValueFactory<>("fechaVencimiento"));

    // Formato de fecha dd/MM/yyyy para las tres columnas de fecha
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    colMes.setCellFactory(col -> new TableCell<>() {
      @Override protected void updateItem(LocalDate v, boolean empty) {
        super.updateItem(v, empty);
        setText(empty || v == null ? null : formatter.format(v));
      }
    });
    colFechaCierre.setCellFactory(col -> new TableCell<>() {
      @Override protected void updateItem(LocalDate v, boolean empty) {
        super.updateItem(v, empty);
        setText(empty || v == null ? null : formatter.format(v));
      }
    });
    colFechaVenc.setCellFactory(col -> new TableCell<>() {
      @Override protected void updateItem(LocalDate v, boolean empty) {
        super.updateItem(v, empty);
        setText(empty || v == null ? null : formatter.format(v));
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
          CierreTarjeta c = getTableView().getItems().get(getIndex());
          cargarEnFormulario(c);
        });

        btnEliminar.setOnAction(e -> {
          CierreTarjeta c = getTableView().getItems().get(getIndex());
          confirmarEliminar(c);
        });
      }

      @Override
      protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : hbox);
      }
    });
  }

  // --- Carga los cierres de la tarjeta seleccionada ---

  private void cargarTabla() {
    if (tarjetaSeleccionada == null) return;
    try {
      CierreTarjetaDao dao = new CierreTarjetaDao();
      List<CierreTarjeta> cierres = dao.findByTarjeta(tarjetaSeleccionada.getId());
      tablaCierres.setItems(FXCollections.observableArrayList(cierres));
    } catch (SQLException ex) {
      logger.error("Error al cargar cierres", ex);
    }
  }

  // --- Alta / Modificación ---

  @FXML
  private void guardarCierre() {
    String tarjetaNombre = cmbTarjeta.getValue();
    LocalDate mes        = dpMes.getValue();
    LocalDate cierre     = dpCierre.getValue();
    LocalDate vencimiento = dpVencimiento.getValue();

    if (tarjetaNombre == null || mes == null || cierre == null || vencimiento == null) {
      Toast.show(getStage(), "Debe completar todos los campos");
      return;
    }

    try {
      TarjetaDao tarjetaDao = new TarjetaDao();
      int tarjetaId = tarjetaDao.findByNombre(tarjetaNombre).getId();
      CierreTarjetaDao dao = new CierreTarjetaDao();

      if (idEditando == -1) {
        // Modo alta
        CierreTarjeta nuevo = new CierreTarjeta(tarjetaId, mes, cierre, vencimiento);
        dao.save(nuevo);
        logger.info("Cierre guardado: tarjeta {} mes {}", tarjetaNombre, mes);
        Toast.show(getStage(), "Cierre guardado correctamente");
      } else {
        // Modo edición
        CierreTarjeta editado = new CierreTarjeta(idEditando, tarjetaId, mes, cierre, vencimiento);
        dao.update(editado);
        logger.info("Cierre actualizado id {}: tarjeta {} mes {}", idEditando, tarjetaNombre, mes);
        Toast.show(getStage(), "Cierre actualizado correctamente");
      }

      // Notificamos al dashboard para que recargue
      MovimientoEventBus.publish("cierre");
      limpiar();
      cargarTabla();

    } catch (SQLException ex) {
      Toast.show(getStage(), "Error al guardar cierre");
      logger.error("Error al guardar cierre", ex);
    }
  }

  // --- Carga el cierre seleccionado en el formulario para editar ---

  private void cargarEnFormulario(CierreTarjeta c) {
    idEditando = c.getIdCierre();
    dpMes.setValue(c.getMes());
    dpCierre.setValue(c.getFechaCierre());
    dpVencimiento.setValue(c.getFechaVencimiento());
  }

  // --- Eliminar con confirmación ---

  private void confirmarEliminar(CierreTarjeta c) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Confirmar eliminación");
    alert.setHeaderText("¿Eliminar cierre?");
    alert.setContentText("Mes: " + c.getMes() + " — Cierre: " + c.getFechaCierre());

    alert.showAndWait().ifPresent(respuesta -> {
      if (respuesta == ButtonType.OK) {
        try {
          CierreTarjetaDao dao = new CierreTarjetaDao();
          dao.delete(c.getIdCierre());
          logger.info("Cierre eliminado id {}", c.getIdCierre());
          MovimientoEventBus.publish("cierre");
          Toast.show(getStage(), "Cierre eliminado");
          limpiar();
          cargarTabla();
        } catch (SQLException ex) {
          Toast.show(getStage(), "Error al eliminar cierre");
          logger.error("Error al eliminar cierre id {}", c.getIdCierre(), ex);
        }
      }
    });
  }

  // --- Limpia el formulario y vuelve a modo alta ---

  @FXML
  private void limpiar() {
    idEditando = -1;
    dpMes.setValue(null);
    dpCierre.setValue(null);
    dpVencimiento.setValue(null);
  }

  private Stage getStage() {
    return (Stage) cmbTarjeta.getScene().getWindow();
  }
}