package ar.com.gastos.controller;

import ar.com.gastos.dao.CierreTarjetaDao;
import ar.com.gastos.dao.CuotaDao;
import ar.com.gastos.dao.MovimientoDao;
import ar.com.gastos.model.CierreTarjeta;
import ar.com.gastos.model.Cuota;
import ar.com.gastos.model.Movimiento;
import ar.com.gastos.model.Tarjeta;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class DetalleController {

  private static final Logger logger = LoggerFactory.getLogger(DetalleController.class);

  @FXML
  private Label lblTitulo;
  @FXML
  private Label lblMes;
  @FXML
  private Label lblConsumos;
  @FXML
  private Label lblPagos;
  @FXML
  private Label lblSaldo;
  @FXML
  private TableView<Movimiento> tablaMovimientos;
  @FXML
  private TableColumn<Movimiento, LocalDate> colFecha;
  @FXML
  private TableColumn<Movimiento, String> colDescripcion;
  @FXML
  private TableColumn<Movimiento, BigDecimal> colMonto;
  @FXML
  private TableColumn<Movimiento, String> colCuota;
  @FXML
  private TableColumn<Movimiento, Void> colAcciones;

  private Tarjeta tarjetaActual;

  // Campo nuevo — referencia al subscriber para poder desuscribir
  private Consumer<String> subscriber;

  // Mes navegable — se inicializa con el mes que pasa el DashboardController
  private YearMonth mesVisible = YearMonth.now();

  private static final NumberFormat CURRENCY =
      NumberFormat.getCurrencyInstance(Locale.of("es", "AR"));

  static {
    CURRENCY.setMaximumFractionDigits(2);
  }

  // --- Recibe la tarjeta y el mes visible desde DashboardController ---

  public void setTarjeta(Tarjeta tarjeta, YearMonth mes) {
    this.tarjetaActual = tarjeta;
    this.mesVisible = mes;
    lblTitulo.setText("Detalle de " + tarjeta.getNombre());
    recargarMovimientos();
  }

  // --- Inicialización de columnas ---

  @FXML
  public void initialize() {
    colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
    colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
    colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
    colCuota.setCellValueFactory(new PropertyValueFactory<>("cuotaTexto"));

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
    colMonto.setCellFactory(col -> new TableCell<>() {
      @Override
      protected void updateItem(BigDecimal monto, boolean empty) {
        super.updateItem(monto, empty);
        setText(empty || monto == null ? null : CURRENCY.format(monto));
      }
    });

    // Formato de monto en pesos
    colAcciones.setCellFactory(col -> new TableCell<>() {
      private final Button btnEditar = new Button("Editar");
      private final Button btnEliminar = new Button("Eliminar");
      private final Button btnPagar = new Button("Pagar");

      {
        btnEditar.setStyle("-fx-background-color:#2c3e50; -fx-text-fill:white; -fx-font-size:11;");
        btnEliminar.setStyle("-fx-background-color:#c0392b; -fx-text-fill:white; -fx-font-size:11;");
        btnPagar.setStyle("-fx-background-color:#27ae60; -fx-text-fill:white; -fx-font-size:11;");

        btnEditar.setOnAction(e -> {
          Movimiento m = getTableView().getItems().get(getIndex());
          abrirEditar(m);
        });

        btnEliminar.setOnAction(e -> {
          Movimiento m = getTableView().getItems().get(getIndex());
          confirmarEliminar(m);
        });

        btnPagar.setOnAction(e -> {
          Movimiento m = getTableView().getItems().get(getIndex());
          confirmarPago(m);
        });
      }

      @Override
      protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || getIndex() >= getTableView().getItems().size()) {
          setGraphic(null);
          return;
        }
        Movimiento m = getTableView().getItems().get(getIndex());
        boolean esDebito = tarjetaActual != null && "DEBITO".equals(tarjetaActual.getTipo());
        boolean esEgreso = "EGRESO".equals(m.getCategoria());

        // Armamos el HBox dinámicamente según el contexto
        HBox hbox = new HBox(6, btnEditar, btnEliminar);
        if (esDebito && esEgreso) {
          hbox.getChildren().add(btnPagar);
        }
        setGraphic(hbox);
      }
    });

    // Guardamos la referencia para poder desuscribir cuando se cierre la ventana
    subscriber = evento -> {
      if (tarjetaActual != null) {
        Platform.runLater(this::recargarMovimientos);
      }
    };
    MovimientoEventBus.subscribe(subscriber);

// Cuando el detalle se cierra, limpiamos el subscriber
    Platform.runLater(() -> {
      Stage stage = (Stage) tablaMovimientos.getScene().getWindow();
      stage.setOnCloseRequest(e -> MovimientoEventBus.unsubscribe(subscriber));
    });
  }

  // --- Navegación por mes ---

  @FXML
  private void meAnterior() {
    mesVisible = mesVisible.minusMonths(1);
    recargarMovimientos();
  }

  @FXML
  private void meSiguiente() {
    mesVisible = mesVisible.plusMonths(1);
    recargarMovimientos();
  }

  // --- Carga los movimientos filtrados por período de cierre del mes visible ---

  private void recargarMovimientos() {
    if (tarjetaActual == null) return;

    // Actualizamos el label del mes — ej.: "Mayo 2026"
    String nombreMes = mesVisible.getMonth()
        .getDisplayName(TextStyle.FULL, Locale.of("es"));
    nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
    lblMes.setText(nombreMes + " " + mesVisible.getYear());

    try {
      MovimientoDao movimientoDao = new MovimientoDao();
      CuotaDao cuotaDao = new CuotaDao();
      CierreTarjetaDao cierreDao = new CierreTarjetaDao();

      // Buscamos el cierre del mes visible para esta tarjeta
      CierreTarjeta cierreMes = cierreDao.findCierrePorMesDeCierre(tarjetaActual.getId(), mesVisible);

      List<Movimiento> movimientos;
      LocalDate desde = mesVisible.atDay(1);
      LocalDate hasta = mesVisible.atEndOfMonth();

      if (cierreMes != null) {
        // Si hay cierre definido usamos el período real
        CierreTarjeta cierreAnterior = cierreDao.findAnteriorPorTarjeta(
            tarjetaActual.getId(), cierreMes.getFechaCierre());
        desde = (cierreAnterior != null)
            ? cierreAnterior.getFechaCierre().plusDays(1)
            : cierreMes.getMes();
        hasta = cierreMes.getFechaCierre();
      }

      movimientos = movimientoDao.findByTarjetaEnRangoPeriodo(
          tarjetaActual.getId(), desde, hasta);

      // Calculamos consumos y pagos del período por separado
      BigDecimal totalConsumos = BigDecimal.ZERO;
      BigDecimal totalPagos = BigDecimal.ZERO;

      List<Movimiento> movimientosFiltrados = new ArrayList<>();

      for (Movimiento m : movimientos) {
        if ("PAGO".equals(m.getCategoria())) {
          m.setCuotaTexto("Pago");
          totalPagos = totalPagos.add(m.getMonto());
          movimientosFiltrados.add(m);

        } else if ("EGRESO".equals(m.getCategoria())) {
          if (m.getCuotas() == 1) {
            m.setCuotaTexto("Pago único");
            totalConsumos = totalConsumos.add(m.getMonto());
            movimientosFiltrados.add(m);
          } else {
            int nroCuota = cierreDao.calcularNroCuota(
                tarjetaActual.getId(), m.getFecha(), desde, hasta);

            if (nroCuota >= 1 && nroCuota <= m.getCuotas()) {
              List<Cuota> cuotas = cuotaDao.findByMovimiento(m.getId());
              for (Cuota c : cuotas) {
                if (c.getNroCuota() == nroCuota) {
                  m.setMonto(c.getMonto());
                  m.setCuotaTexto(nroCuota + " de " + m.getCuotas());
                  totalConsumos = totalConsumos.add(c.getMonto());
                  movimientosFiltrados.add(m); // ← SOLO acá se agrega
                  break;
                }
              }
            }
            // ← Si nroCuota es -1 o > cuotas, NO se agrega a movimientosFiltrados
          }
        }
      }

      tablaMovimientos.setItems(FXCollections.observableArrayList(movimientosFiltrados));

      BigDecimal saldo = totalConsumos.subtract(totalPagos);

      lblConsumos.setText("Consumos: " + CURRENCY.format(totalConsumos));
      lblPagos.setText("Pagos: " + CURRENCY.format(totalPagos));
      lblSaldo.setText("Saldo: " + CURRENCY.format(saldo));

// Color del saldo: rojo si positivo (debe), verde si cero o negativo (a favor)
      lblSaldo.setStyle(saldo.compareTo(BigDecimal.ZERO) > 0
          ? "-fx-font-size:12; -fx-font-weight:bold; -fx-text-fill:#c0392b;"
          : "-fx-font-size:12; -fx-font-weight:bold; -fx-text-fill:#27ae60;");


    } catch (Exception ex) {
      logger.error("Error al recargar movimientos de tarjeta {}",
          tarjetaActual.getNombre(), ex);
    }
  }

  // --- Abre el formulario de edición ---

  private void abrirEditar(Movimiento m) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/ar/com/gastos/editar-movimiento.fxml"));
      Scene scene = new Scene(loader.load(), 400, 380);
      EditarMovimientoController ctrl = loader.getController();
      ctrl.setMovimiento(m);
      Stage stage = new Stage();
      stage.setTitle("Editar Movimiento");
      stage.setScene(scene);
      stage.show();
    } catch (IOException ex) {
      logger.error("Error al abrir formulario de edición", ex);
    }
  }

  // --- Confirmar y eliminar movimiento ---

  private void confirmarEliminar(Movimiento m) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Confirmar eliminación");
    alert.setHeaderText("¿Eliminar movimiento?");
    alert.setContentText("\"" + m.getDescripcion() + "\"\n"
        + (m.getCuotas() > 1 ? "Se eliminarán también todas sus cuotas." : ""));

    alert.showAndWait().ifPresent(respuesta -> {
      if (respuesta == ButtonType.OK) eliminar(m);
    });
  }

  // --- Registra el pago de un ítem de débito ---
  private void confirmarPago(Movimiento m) {
    // Armamos un diálogo personalizado con DatePicker + TextField de monto
    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Registrar pago");
    dialog.setHeaderText("Pagar: " + m.getDescripcion());

    // Contenido del diálogo
    DatePicker dpFecha = new DatePicker(LocalDate.now());
    TextField txtMonto = new TextField(m.getMonto().toPlainString());

    javafx.scene.layout.VBox contenido = new javafx.scene.layout.VBox(10,
        new Label("Fecha:"), dpFecha,
        new Label("Monto:"), txtMonto
    );
    contenido.setStyle("-fx-padding:10;");
    dialog.getDialogPane().setContent(contenido);

    ButtonType btnConfirmar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
    ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
    dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, btnCancelar);

    dialog.showAndWait().ifPresent(respuesta -> {
      if (respuesta == btnConfirmar) {
        try {
          LocalDate fecha = dpFecha.getValue();
          if (fecha == null) {
            Toast.show((Stage) tablaMovimientos.getScene().getWindow(), "Debe seleccionar una fecha");
            return;
          }

          BigDecimal monto = new BigDecimal(txtMonto.getText().trim()).setScale(2);

          Movimiento pago = new Movimiento(
              tarjetaActual.getId(),
              fecha,
              "PAGO " + m.getDescripcion(),
              monto,
              "ARS"
          );

          new MovimientoDao().save(pago);

          logger.info("Pago registrado: {} - ${} - {}", m.getDescripcion(), monto, fecha);
          MovimientoEventBus.publish("pago");
          Toast.show((Stage) tablaMovimientos.getScene().getWindow(),
              "Pago registrado: " + CURRENCY.format(monto));

        } catch (NumberFormatException ex) {
          Toast.show((Stage) tablaMovimientos.getScene().getWindow(), "Monto inválido");
        } catch (Exception ex) {
          Toast.show((Stage) tablaMovimientos.getScene().getWindow(), "Error al registrar pago");
          logger.error("Error al registrar pago de {}", m.getDescripcion(), ex);
        }
      }
    });
  }

  private void eliminar(Movimiento m) {
    try {
      new MovimientoDao().delete(m.getId());
      logger.info("Movimiento eliminado: {} - id {}", m.getDescripcion(), m.getId());
      MovimientoEventBus.publish("eliminacion");
      Toast.show((Stage) tablaMovimientos.getScene().getWindow(), "Movimiento eliminado");
    } catch (Exception ex) {
      Toast.show((Stage) tablaMovimientos.getScene().getWindow(), "Error al eliminar");
      logger.error("Error al eliminar movimiento id {}", m.getId(), ex);
    }
  }
}