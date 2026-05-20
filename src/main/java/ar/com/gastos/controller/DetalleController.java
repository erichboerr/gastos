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
import java.util.List;
import java.util.Locale;

public class DetalleController {

  private static final Logger logger = LoggerFactory.getLogger(DetalleController.class);

  @FXML private Label lblTitulo;
  @FXML private Label lblMes;
  @FXML private Label lblTotalMes;
  @FXML private TableView<Movimiento> tablaMovimientos;
  @FXML private TableColumn<Movimiento, LocalDate>  colFecha;
  @FXML private TableColumn<Movimiento, String>     colDescripcion;
  @FXML private TableColumn<Movimiento, BigDecimal> colMonto;
  @FXML private TableColumn<Movimiento, String>     colCuota;
  @FXML private TableColumn<Movimiento, Void>       colAcciones;

  private Tarjeta tarjetaActual;

  // Mes navegable — se inicializa con el mes que pasa el DashboardController
  private YearMonth mesVisible = YearMonth.now();

  private static final NumberFormat CURRENCY =
      NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
  static { CURRENCY.setMaximumFractionDigits(2); }

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

    // Columna de acciones: Editar + Eliminar por fila
    colAcciones.setCellFactory(col -> new TableCell<>() {
      private final Button btnEditar   = new Button("Editar");
      private final Button btnEliminar = new Button("Eliminar");
      private final HBox   hbox        = new HBox(6, btnEditar, btnEliminar);

      {
        btnEditar.setStyle("-fx-background-color:#2c3e50; -fx-text-fill:white; -fx-font-size:11;");
        btnEliminar.setStyle("-fx-background-color:#c0392b; -fx-text-fill:white; -fx-font-size:11;");

        btnEditar.setOnAction(e -> {
          Movimiento m = getTableView().getItems().get(getIndex());
          abrirEditar(m);
        });

        btnEliminar.setOnAction(e -> {
          Movimiento m = getTableView().getItems().get(getIndex());
          confirmarEliminar(m);
        });
      }

      @Override
      protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : hbox);
      }
    });

    // Recarga cuando el EventBus notifica cambios
    MovimientoEventBus.subscribe(evento -> {
      if (tarjetaActual != null) {
        Platform.runLater(this::recargarMovimientos);
      }
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

    // Actualizamos el label del mes — ej: "Mayo 2026"
    String nombreMes = mesVisible.getMonth()
        .getDisplayName(TextStyle.FULL, new Locale("es"));
    nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
    lblMes.setText(nombreMes + " " + mesVisible.getYear());

    try {
      MovimientoDao movimientoDao   = new MovimientoDao();
      CuotaDao cuotaDao             = new CuotaDao();
      CierreTarjetaDao cierreDao    = new CierreTarjetaDao();

      // Buscamos el cierre del mes visible para esta tarjeta
      CierreTarjeta cierreMes = cierreDao.findCierrePorMes(tarjetaActual.getId(), mesVisible);

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

      // Calculamos el total del mes y armamos el texto de cuota por fila
      BigDecimal totalMes = BigDecimal.ZERO;

      for (Movimiento m : movimientos) {
        if ("EGRESO".equals(m.getCategoria())) {
          if (m.getCuotas() == 1) {
            m.setCuotaTexto("Pago único");
            totalMes = totalMes.add(m.getMonto());
          } else {
            // Mostramos la cuota que vence en el período
            List<Cuota> cuotas = cuotaDao.findByMovimiento(m.getId());
            for (Cuota c : cuotas) {
              if (!c.getFechaVencimiento().isBefore(desde) &&
                  !c.getFechaVencimiento().isAfter(hasta)) {
                m.setMonto(c.getMonto());
                m.setCuotaTexto(c.getNroCuota() + " de " + m.getCuotas());
                totalMes = totalMes.add(c.getMonto());
                break;
              }
            }
          }
        } else {
          m.setCuotaTexto("-");
        }
      }

      lblTotalMes.setText("Total del mes: " + CURRENCY.format(totalMes));
      tablaMovimientos.setItems(FXCollections.observableArrayList(movimientos));

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