package ar.com.gastos.controller;

import ar.com.gastos.dao.*;
import ar.com.gastos.model.*;
import ar.com.gastos.util.BackupService;
import ar.com.gastos.util.MovimientoEventBus;

import ar.com.gastos.util.Toast;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
import java.util.function.Consumer;

public class DashboardController {

  private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

  private YearMonth mesVisible = YearMonth.now();

  private static final Locale LOCALE_ES = new Locale("es", "AR");
  private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(LOCALE_ES);
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  static {
    CURRENCY.setMaximumFractionDigits(2);
  }

  @FXML private Label    lblBalance;
  @FXML private Label    lblIngresos;
  @FXML private Label    lblEgresos;
  @FXML private Label    lblMesNavegacion;
  @FXML private GridPane cardsPane;
  @FXML private VBox     cardIngresos;

  private Consumer<String> subscriber;

  @FXML
  public void initialize() {
    recargarDashboard();
    subscriber = nombre -> Platform.runLater(this::recargarDashboard);
    MovimientoEventBus.subscribe(subscriber);
    Platform.runLater(() -> {
      Stage stage = (Stage) lblBalance.getScene().getWindow();
      stage.setOnCloseRequest(e -> MovimientoEventBus.unsubscribe(subscriber));
    });
  }

  // --- Navegación por mes ---

  @FXML private void meAnterior() { mesVisible = mesVisible.minusMonths(1); recargarDashboard(); }
  @FXML private void meSiguiente() { mesVisible = mesVisible.plusMonths(1); recargarDashboard(); }

  // --- Apertura de formularios ---

  @FXML
  private void abrirIngreso() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/gastos/ingreso.fxml"));
      Scene scene = new Scene(loader.load(), 600, 600);
      Stage stage = new Stage();
      stage.setTitle("Ingresos");
      stage.setScene(scene);
      stage.show();
    } catch (IOException ex) { logger.error("Error al abrir ingresos", ex); }
  }

  @FXML
  private void abrirCierreTarjeta() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/gastos/CierreTarjeta.fxml"));
      Scene scene = new Scene(loader.load(), 670, 620);
      Stage stage = new Stage();
      stage.setTitle("Cierre de Tarjeta");
      stage.setScene(scene);
      stage.show();
    } catch (IOException ex) { logger.error("Error al abrir cierres", ex); }
  }

  @FXML
  private void abrirComercios() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/gastos/comercios.fxml"));
      Scene scene = new Scene(loader.load(), 670, 620);
      Stage stage = new Stage();
      stage.setTitle("Comercios");
      stage.setScene(scene);
      stage.show();
    } catch (IOException ex) { logger.error("Error al abrir comercios", ex); }
  }

  @FXML private void abrirNuevaTarjeta()  { abrirFormulario("/ar/com/gastos/nueva-tarjeta.fxml",      "Nueva Tarjeta"); }
  @FXML private void abrirEgreso()        { abrirFormulario("/ar/com/gastos/egreso.fxml",             "Nuevo Egreso"); }
  @FXML private void abrirPago()          { abrirFormulario("/ar/com/gastos/pago.fxml",               "Registrar Pago"); }
  @FXML private void abrirRecurrentes()   { abrirFormulario("/ar/com/gastos/gastos-recurrentes.fxml", "Gastos Recurrentes"); }
  @FXML private void abrirGenerarMes()    { abrirFormulario("/ar/com/gastos/generar-mes.fxml",        "Generar Mes"); }

  @FXML
  private void abrirResumenAnual() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/gastos/resumen-anual.fxml"));
      Scene scene = new Scene(loader.load(), 700, 500);
      ResumenAnualController ctrl = loader.getController();
      ctrl.cargarAnio(mesVisible.getYear());
      Stage stage = new Stage();
      stage.setTitle("Resumen Anual " + mesVisible.getYear());
      stage.setScene(scene);
      stage.show();
    } catch (IOException ex) { logger.error("Error al abrir resumen anual", ex); }
  }

  private void abrirFormulario(String fxmlPath, String titulo) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Scene scene = new Scene(loader.load());
      Stage stage = new Stage();
      stage.setTitle(titulo);
      stage.setScene(scene);
      stage.show();
    } catch (IOException ex) { logger.error("Error al abrir formulario: {}", fxmlPath, ex); }
  }

  // --- Carga del dashboard ---

  private void recargarDashboard() {
    cardsPane.getChildren().clear();
    cardIngresos.getChildren().clear();

    String nombreMes = mesVisible.getMonth().getDisplayName(TextStyle.FULL, new Locale("es"));
    String nombreMesCap = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
    lblMesNavegacion.setText(nombreMesCap + " " + mesVisible.getYear());

    try {
      TarjetaDao tarjetaDao       = new TarjetaDao();
      MovimientoDao movimientoDao = new MovimientoDao();
      IngresoDao ingresoDao       = new IngresoDao();
      CierreTarjetaDao cierreDao  = new CierreTarjetaDao();
      CuotaDao cuotaDao           = new CuotaDao();

      List<Tarjeta> tarjetas = tarjetaDao.findAllActivas();

      double totalIngresos = ingresoDao.listarIngresos().stream()
          .filter(i -> YearMonth.from(i.getFecha()).equals(mesVisible))
          .map(Ingreso::getMonto)
          .mapToDouble(BigDecimal::doubleValue)
          .sum();

      double totalEgresos = 0;
      double totalPagos   = 0;

      for (Tarjeta t : tarjetas) {
        CierreTarjeta cierreMes = cierreDao.findCierrePorVencimiento(t.getId(), mesVisible);

        if (cierreMes != null) {
          CierreTarjeta cierreAnterior = cierreDao.findAnteriorPorTarjeta(
              t.getId(), cierreMes.getFechaCierre());

          LocalDate desde = (cierreAnterior != null)
              ? cierreAnterior.getFechaCierre().plusDays(1)
              : cierreMes.getMes();
          LocalDate hasta = cierreMes.getFechaCierre();

          logger.debug("Tarjeta [{}] — período desde: {} hasta: {}", t.getNombre(), desde, hasta);

          List<Movimiento> movimientos = movimientoDao.findByTarjetaEnRangoPeriodo(t.getId(), desde, hasta);

          BigDecimal totalPeriodo = BigDecimal.ZERO;
          BigDecimal pagosPeriodo = BigDecimal.ZERO;

          for (Movimiento m : movimientos) {
            if ("EGRESO".equals(m.getCategoria())) {
              if (m.getCuotas() == 1) {
                totalPeriodo = totalPeriodo.add(m.getMonto());
                totalEgresos += m.getMonto().doubleValue();
              } else {
                // ✅ Filtramos por mes y año de vencimiento, no por rango exacto
                List<Cuota> cuotas = cuotaDao.findByMovimiento(m.getId());
                for (Cuota c : cuotas) {
                  if (YearMonth.from(c.getFechaVencimiento()).equals(mesVisible)) {
                    totalPeriodo = totalPeriodo.add(c.getMonto());
                    totalEgresos += c.getMonto().doubleValue();
                    break;
                  }
                }
              }
            } else if ("PAGO".equals(m.getCategoria())) {
              pagosPeriodo = pagosPeriodo.add(m.getMonto());
              totalPagos  += m.getMonto().doubleValue();
            }
          }

          t.setTotalGastado(totalPeriodo.doubleValue());
          t.setTotalPagado(pagosPeriodo.doubleValue());
          t.setRestaAbonar(totalPeriodo.subtract(pagosPeriodo).doubleValue());

          // Creamos la card con el cierre disponible
          cardsPane.add(crearCard(t, cierreMes), col(tarjetas, t), row(tarjetas, t));
        } else {
          // Sin cierre definido para este mes
          cardsPane.add(crearCardSinCierre(t), col(tarjetas, t), row(tarjetas, t));
        }
      }

      double balance = totalIngresos - totalEgresos;
      logger.debug("Mes: {} | Ingresos: {} | Egresos: {} | Balance: {}",
          mesVisible, totalIngresos, totalEgresos, balance);

      lblIngresos.setText("Ingresos: " + CURRENCY.format(totalIngresos));
      lblEgresos.setText("Egresos: "   + CURRENCY.format(totalEgresos));
      lblBalance.setText("Balance: "   + CURRENCY.format(balance));

      cardIngresos.getChildren().add(crearCardIngresos(totalIngresos));

    } catch (Exception ex) {
      logger.error("Error al recargar dashboard", ex);
    }
  }

  // Helpers para calcular col/row en la grilla
  private int col(List<Tarjeta> tarjetas, Tarjeta t) { return tarjetas.indexOf(t) % 3; }
  private int row(List<Tarjeta> tarjetas, Tarjeta t) { return tarjetas.indexOf(t) / 3; }

  // --- Cards ---

  private VBox crearCardIngresos(double totalIngresos) {
    VBox card = new VBox(6);
    card.getStyleClass().add("card");
    card.setPrefWidth(400);
    card.setMaxWidth(400);

    Label lblTitulo = new Label("INGRESOS DEL MES");
    lblTitulo.getStyleClass().add("card-title");

    Label lblTotal = new Label("Total: " + CURRENCY.format(totalIngresos));
    lblTotal.getStyleClass().add("card-body");

    card.getChildren().addAll(lblTitulo, lblTotal);
    card.setStyle("-fx-padding:20; -fx-alignment:center;");
    return card;
  }

  /**
   * Card con cierre definido — muestra dos columnas:
   * Izquierda: fechas de cierre y vencimiento
   * Derecha: gastos, pagos, resta abonar
   */
  private VBox crearCard(Tarjeta t, CierreTarjeta cierre) {
    VBox card = new VBox(8);
    card.getStyleClass().add("card");
    card.setPrefWidth(480);
    card.setStyle("-fx-padding:16;");

    // Título
    Label lblNombre = new Label(t.getNombre());
    lblNombre.getStyleClass().add("card-title");

    // Contenido en dos columnas
    GridPane grid = new GridPane();
    grid.setHgap(16);
    grid.setVgap(4);
    ColumnConstraints col1 = new ColumnConstraints();
    col1.setPercentWidth(50);
    ColumnConstraints col2 = new ColumnConstraints();
    col2.setPercentWidth(50);
    grid.getColumnConstraints().addAll(col1, col2);

    // Columna izquierda — fechas
    Label lblFechaTitulo = new Label("Fechas");
    lblFechaTitulo.getStyleClass().add("card-label-key");

    Label lblCierreLabel = new Label("Cierre:");
    lblCierreLabel.getStyleClass().add("card-label-key");
    Label lblCierreVal = new Label(cierre.getFechaCierre().format(DATE_FMT));
    lblCierreVal.getStyleClass().add("card-label-val");

    Label lblVencLabel = new Label("Vencimiento:");
    lblVencLabel.getStyleClass().add("card-label-key");
    Label lblVencVal = new Label(cierre.getFechaVencimiento().format(DATE_FMT));
    lblVencVal.getStyleClass().add("card-label-val");

    // Columna derecha — movimientos
    Label lblMovTitulo = new Label("Movimientos");
    lblMovTitulo.getStyleClass().add("card-label-key");

    Label lblGastosLabel = new Label("Gastos del mes:");
    lblGastosLabel.getStyleClass().add("card-label-key");
    Label lblGastosVal = new Label(CURRENCY.format(t.getTotalGastado()));
    lblGastosVal.getStyleClass().add("card-label-val");

    Label lblPagosLabel = new Label("Pagos realizados:");
    lblPagosLabel.getStyleClass().add("card-label-key");
    Label lblPagosVal = new Label(CURRENCY.format(t.getTotalPagado()));
    lblPagosVal.setStyle("-fx-font-size:12; -fx-text-fill:#27ae60;"); // verde fijo

    Label lblRestaLabel = new Label("Resta abonar:");
    lblRestaLabel.getStyleClass().add("card-label-key");
    Label lblRestaVal = new Label(CURRENCY.format(t.getRestaAbonar()));
    String colorResta = t.getRestaAbonar() > 0
        ? "-fx-font-size:12; -fx-font-weight:bold; -fx-text-fill:#c0392b;"
        : "-fx-font-size:12; -fx-font-weight:bold; -fx-text-fill:#27ae60;";
    lblRestaVal.setStyle(colorResta);

    // Armamos el grid — col 0 = fechas, col 1 = movimientos
    grid.add(lblFechaTitulo,  0, 0);
    grid.add(lblCierreLabel,  0, 1);
    grid.add(lblCierreVal,    0, 2);
    grid.add(lblVencLabel,    0, 3);
    grid.add(lblVencVal,      0, 4);

    grid.add(lblMovTitulo,    1, 0);
    grid.add(lblGastosLabel,  1, 1);
    grid.add(lblGastosVal,    1, 2);
    grid.add(lblPagosLabel,   1, 3);
    grid.add(lblPagosVal,     1, 4);
    grid.add(lblRestaLabel,   1, 5);
    grid.add(lblRestaVal,     1, 6);

    // Botones
    Button btnDetalle = crearBotonDetalle(t);
    HBox.setHgrow(btnDetalle, Priority.ALWAYS);
    btnDetalle.setMaxWidth(Double.MAX_VALUE);

    Button btnEditar = crearBotonEditarTarjeta(t);
    HBox.setHgrow(btnEditar, Priority.ALWAYS);
    btnEditar.setMaxWidth(Double.MAX_VALUE);

    HBox hboxBotones = new HBox(6, btnDetalle, btnEditar);
    hboxBotones.setMaxWidth(Double.MAX_VALUE);

    card.getChildren().addAll(lblNombre, new Separator(), grid, new Separator(), hboxBotones);
    return card;
  }

  /**
   * Card sin cierre definido — muestra indicador visual claro
   * en lugar de ceros que pueden confundir.
   */
  private VBox crearCardSinCierre(Tarjeta t) {
    VBox card = new VBox(8);
    card.getStyleClass().add("card");
    card.setPrefWidth(480);
    card.setStyle("-fx-padding:16;");

    Label lblNombre = new Label(t.getNombre());
    lblNombre.getStyleClass().add("card-title");

    Label lblSinCierre = new Label("⚠ Sin cierre definido para este mes");
    lblSinCierre.setStyle("-fx-font-size:12; -fx-text-fill:#e67e22; -fx-font-style:italic;");

    Button btnDetalle = crearBotonDetalle(t);
    HBox.setHgrow(btnDetalle, Priority.ALWAYS);
    btnDetalle.setMaxWidth(Double.MAX_VALUE);

    Button btnEditar = crearBotonEditarTarjeta(t);
    HBox.setHgrow(btnEditar, Priority.ALWAYS);
    btnEditar.setMaxWidth(Double.MAX_VALUE);

    HBox hboxBotones = new HBox(6, btnDetalle, btnEditar);
    hboxBotones.setMaxWidth(Double.MAX_VALUE);

    card.getChildren().addAll(lblNombre, new Separator(), lblSinCierre, new Separator(), hboxBotones);
    return card;
  }

  private Button crearBotonDetalle(Tarjeta t) {
    Button btn = new Button("Ver detalle");
    btn.setOnAction(e -> {
      try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/gastos/detalle.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);
        DetalleController ctrl = loader.getController();
        ctrl.setTarjeta(t, mesVisible);
        Stage stage = new Stage();
        stage.setTitle("Detalle de " + t.getNombre());
        stage.setScene(scene);
        stage.show();
      } catch (IOException ex) {
        logger.error("Error al abrir detalle de tarjeta: {}", t.getNombre(), ex);
      }
    });
    return btn;
  }

  private Button crearBotonEditarTarjeta(Tarjeta t) {
    Button btn = new Button("Editar / Eliminar");
    btn.setOnAction(e -> {
      try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/gastos/editar-tarjeta.fxml"));
        Scene scene = new Scene(loader.load(), 400, 320);
        EditarTarjetaController ctrl = loader.getController();
        ctrl.setTarjeta(t);
        Stage stage = new Stage();
        stage.setTitle("Editar Tarjeta");
        stage.setScene(scene);
        stage.show();
      } catch (IOException ex) {
        logger.error("Error al abrir editar tarjeta", ex);
      }
    });
    return btn;
  }
  @FXML
  private void ejecutarBackup() {
    try {
      String path = BackupService.ejecutarBackup();
      Toast.show((Stage) lblBalance.getScene().getWindow(),
          "Backup guardado: " + path);
      logger.info("Backup ejecutado correctamente: {}", path);
    } catch (Exception ex) {
      Toast.show((Stage) lblBalance.getScene().getWindow(),
          "Error al ejecutar backup: " + ex.getMessage());
      logger.error("Error al ejecutar backup", ex);
    }
  }

  @FXML
  private void abrirRestaurarBackup() {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/ar/com/gastos/restaurar-backup.fxml"));
      Scene scene = new Scene(loader.load());
      Stage stage = new Stage();
      stage.setTitle("Restaurar Backup");
      stage.setScene(scene);
      stage.show();
    } catch (IOException ex) {
      logger.error("Error al abrir restaurar backup", ex);
    }
  }
}