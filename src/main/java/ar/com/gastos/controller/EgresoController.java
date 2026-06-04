package ar.com.gastos.controller;

import ar.com.gastos.dao.ComercioDao;
import ar.com.gastos.dao.MovimientoDao;
import ar.com.gastos.dao.TarjetaDao;
import ar.com.gastos.model.Comercio;
import ar.com.gastos.model.Movimiento;
import ar.com.gastos.model.Tarjeta;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EgresoController {

  private static final Logger logger = LoggerFactory.getLogger(EgresoController.class);

  @FXML
  private ComboBox<String> cmbTarjeta;
  @FXML
  private DatePicker txtFecha;
  @FXML
  private Label lblComercio;
  @FXML
  private HBox hboxComercio;
  @FXML
  private ComboBox<Comercio> cmbDescripcion;
  @FXML
  private Label lblDescripcion;
  @FXML
  private TextField txtDescripcion;
  @FXML
  private TextField txtMonto;
  @FXML
  private Label lblCuotas;
  @FXML
  private TextField txtCuotas;

  // Lista completa de comercios para el filtro
  private final ObservableList<Comercio> todosLosComercios = FXCollections.observableArrayList();

  // Tarjeta actualmente seleccionada
  private Tarjeta tarjetaSeleccionada = null;

  @FXML
  public void initialize() {
    cargarTarjetas();
    cargarComercios();
    configurarFiltroComercios();
    // Por defecto ocultamos todos los campos específicos
    mostrarCamposDebito(false);
    mostrarCamposCredito(false);
  }

  // --- Al seleccionar tarjeta mostramos los campos correspondientes ---

  @FXML
  private void onTarjetaSeleccionada() {
    String nombre = cmbTarjeta.getValue();
    if (nombre == null) return;
    try {
      TarjetaDao dao = new TarjetaDao();
      tarjetaSeleccionada = dao.findByNombre(nombre);
      if (tarjetaSeleccionada == null) return;

      boolean esDebito = "DEBITO".equals(tarjetaSeleccionada.getTipo());
      mostrarCamposDebito(esDebito);
      mostrarCamposCredito(!esDebito);
      limpiarCampos();
    } catch (SQLException ex) {
      logger.error("Error al cargar tarjeta", ex);
    }
  }

  private void mostrarCamposDebito(boolean visible) {
    lblDescripcion.setVisible(visible);
    lblDescripcion.setManaged(visible);
    txtDescripcion.setVisible(visible);
    txtDescripcion.setManaged(visible);
  }

  private void mostrarCamposCredito(boolean visible) {
    lblComercio.setVisible(visible);
    lblComercio.setManaged(visible);
    hboxComercio.setVisible(visible);
    hboxComercio.setManaged(visible);
    lblCuotas.setVisible(visible);
    lblCuotas.setManaged(visible);
    txtCuotas.setVisible(visible);
    txtCuotas.setManaged(visible);
  }

  // --- Carga tarjetas ---

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

  // --- Carga comercios ---

  private void cargarComercios() {
    try {
      ComercioDao dao = new ComercioDao();
      List<Comercio> comercios = dao.findAllActivos();
      todosLosComercios.setAll(comercios);
      cmbDescripcion.setItems(FXCollections.observableArrayList(comercios));
    } catch (Exception e) {
      logger.error("Error al cargar comercios", e);
    }
  }

  // --- Filtro automático en el ComboBox de comercios ---

  private void configurarFiltroComercios() {
    cmbDescripcion.setEditable(true);
    cmbDescripcion.getEditor().textProperty().addListener((obs, anterior, nuevo) -> {
      Comercio seleccionado = cmbDescripcion.getValue();
      if (seleccionado != null && seleccionado.getNombre().equalsIgnoreCase(nuevo)) return;

      String filtro = nuevo == null ? "" : nuevo.toUpperCase().trim();
      if (filtro.isEmpty()) {
        cmbDescripcion.setItems(FXCollections.observableArrayList(todosLosComercios));
      } else {
        List<Comercio> filtrados = todosLosComercios.stream()
            .filter(c -> c.getNombre().toUpperCase().contains(filtro))
            .collect(Collectors.toList());
        cmbDescripcion.setItems(FXCollections.observableArrayList(filtrados));
      }
      if (!cmbDescripcion.isShowing()) cmbDescripcion.show();
    });
  }

  // --- Nuevo comercio on the fly ---

  @FXML
  private void nuevaDescripcion() {
    String textoActual = cmbDescripcion.getEditor().getText().toUpperCase().trim();
    TextInputDialog dialog = new TextInputDialog(textoActual);
    dialog.setTitle("Nuevo comercio");
    dialog.setHeaderText(null);
    dialog.setContentText("Nombre del comercio:");

    Optional<String> resultado = dialog.showAndWait();
    resultado.ifPresent(texto -> {
      String nombre = texto.toUpperCase().trim();
      if (nombre.isEmpty()) return;
      try {
        ComercioDao dao = new ComercioDao();
        Comercio existente = dao.findByNombre(nombre);
        if (existente != null) {
          cmbDescripcion.setValue(existente);
          return;
        }
        dao.save(new Comercio(nombre, null));
        cargarComercios();
        cmbDescripcion.getItems().stream()
            .filter(c -> c.getNombre().equals(nombre))
            .findFirst()
            .ifPresent(cmbDescripcion::setValue);
        logger.info("Comercio creado: {}", nombre);
      } catch (SQLException ex) {
        logger.error("Error al crear comercio", ex);
      }
    });
  }

  // --- Guardar movimiento ---

  @FXML
  private void guardarMovimiento() {
    if (tarjetaSeleccionada == null) {
      Toast.show(getStage(), "Debe seleccionar una tarjeta");
      return;
    }

    LocalDate fecha = txtFecha.getValue();
    if (fecha == null) {
      Toast.show(getStage(), "Debe seleccionar una fecha");
      return;
    }

    String montoStr = txtMonto.getText().trim();
    if (montoStr.isEmpty()) {
      Toast.show(getStage(), "Debe ingresar un monto");
      return;
    }

    try {
      BigDecimal monto = new BigDecimal(montoStr).setScale(2);
      boolean esDebito = "DEBITO".equals(tarjetaSeleccionada.getTipo());

      if (esDebito) {
        saveDebito(tarjetaSeleccionada, fecha, monto);
      } else {
        saveCredito(tarjetaSeleccionada, fecha, monto);
      }

      limpiar();
      Toast.show(getStage(), "Egreso guardado en " + tarjetaSeleccionada.getNombre());
      MovimientoEventBus.publish(tarjetaSeleccionada.getNombre());

    } catch (NumberFormatException ex) {
      Toast.show(getStage(), "Monto inválido");
    } catch (SQLException ex) {
      Toast.show(getStage(), "Error al guardar egreso");
      logger.error("Error al guardar egreso", ex);
    }
  }

  /**
   * Guarda un egreso en tarjeta DÉBITO.
   * Sin comercio_id, con descripción libre, siempre cuota única.
   */
  private void saveDebito(Tarjeta tarjeta, LocalDate fecha, BigDecimal monto)
      throws SQLException {
    String descripcion = txtDescripcion.getText().toUpperCase().trim();
    if (descripcion.isEmpty()) {
      Toast.show(getStage(), "Debe ingresar una descripción");
      throw new IllegalArgumentException("Descripción vacía");
    }

    Movimiento mov = new Movimiento(
        tarjeta.getId(),
        fecha,
        descripcion,
        monto,
        "EGRESO",
        "ARS"
    );
    new MovimientoDao().save(mov);
    logger.info("Egreso DÉBITO guardado: {} - ${} - {}", descripcion, monto, fecha);
  }

  /**
   * Guarda un egreso en tarjeta CRÉDITO.
   * Con comercio_id, sin descripción libre, con cuotas.
   */
  private void saveCredito(Tarjeta tarjeta, LocalDate fecha, BigDecimal monto)
      throws SQLException {
    // Resolvemos el comercio
    Comercio comercio = null;
    Object valor = cmbDescripcion.getValue();
    if (valor instanceof Comercio) {
      comercio = (Comercio) valor;
    } else if (valor instanceof String) {
      String nombre = ((String) valor).toUpperCase().trim();
      if (!nombre.isEmpty()) {
        ComercioDao dao = new ComercioDao();
        comercio = dao.findByNombre(nombre);
        if (comercio == null) {
          dao.save(new Comercio(nombre, null));
          cargarComercios();
          comercio = dao.findByNombre(nombre);
        }
      }
    }

    if (comercio == null) {
      Toast.show(getStage(), "Debe seleccionar o ingresar un comercio");
      throw new IllegalArgumentException("Comercio vacío");
    }

    int cuotas = txtCuotas.getText().isEmpty() ? 1 : Integer.parseInt(txtCuotas.getText());

    Movimiento mov = new Movimiento(
        tarjeta.getId(),
        comercio.getId(),
        fecha,
        monto,
        "ARS",
        cuotas
    );
    new MovimientoDao().save(mov);
    logger.info("Egreso CRÉDITO guardado: {} en {} - ${} - {} cuotas",
        comercio.getNombre(), tarjeta.getNombre(), monto, cuotas);
  }

  // --- Limpia el formulario ---

  private void limpiarCampos() {
    txtDescripcion.clear();
    cmbDescripcion.getEditor().clear();
    cmbDescripcion.setValue(null);
    txtMonto.clear();
    txtCuotas.clear();
    txtFecha.setValue(null);
  }

  @FXML
  private void limpiar() {
    limpiarCampos();
    cargarComercios();
  }

  @FXML
  private void cerrar() {
    getStage().close();
  }

  private Stage getStage() {
    return (Stage) cmbTarjeta.getScene().getWindow();
  }
}