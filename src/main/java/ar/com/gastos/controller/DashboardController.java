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
import javafx.scene.control.*;
import javafx.scene.layout.*;
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

  private static final Locale LOCALE_ES = Locale.of("es", "AR");
  private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(LOCALE_ES);
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  static { CURRENCY.setMaximumFractionDigits(2); }

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

  @FXML private void meAnterior()  { mesVisible = mesVisible.minusMonths(1); recargarDashboard(); }
  @FXML private void meSiguiente() { mesVisible = mesVisible.plusMonths(1);  recargarDashboard(); }

  @FXML private void abrirIngreso() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/gastos/ingreso.fxml"));
      Scene scene = new Scene(loader.load(), 600, 600);
      Stage stage = new Stage();
      stage.setTitle("Ingresos");
      stage.setScene(scene);
      stage.show();
    } catch (IOException ex) { logger.error("Error al abrir ingresos", ex); }
  }

  @FXML private void abrirCierreTarjeta() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/gastos/CierreTarjeta.fxml"));
      Scene scene = new Scene(loader.load(), 670, 620);
      Stage stage = new Stage();
      stage.setTitle("Cierre de Tarjeta");
      stage.setScene(scene);
      stage.show();
    } catch (IOException ex) { logger.error("Error al abrir cierres", ex); }
  }

  @FXML private void abrirComercios() {
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

  @FXML private void abrirResumenAnual() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/gastos/resumen-anual.fxml"));
      Scene scene = new Scene(loader.load(), 780, 600);
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

    String nombreMes = mesVisible.getMonth().getDisplayName(TextStyle.FULL, Locale.of("es"));
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

        // Período anterior — cuyo vencimiento cae en mesVisible (lo que se abona este mes)
        CierreTarjeta cierrePeriodoAnterior = cierreDao.findCierrePorVencimiento(t.getId(), mesVisible);

        // Período actual — cuyo cierre cae en mesVisible (lo que se está consumiendo)
        CierreTarjeta cierrePeriodoActual = cierreDao.findCierrePorMesDeCierre(t.getId(), mesVisible);

        if (cierrePeriodoAnterior != null) {
          // Calculamos el período anterior (lo que se abona este mes)
          CierreTarjeta cierreAnt = cierreDao.findAnteriorPorTarjeta(
              t.getId(), cierrePeriodoAnterior.getFechaCierre());

          LocalDate desdeAnt = (cierreAnt != null)
              ? cierreAnt.getFechaCierre().plusDays(1)
              : cierrePeriodoAnterior.getMes();
          LocalDate hastaAnt = cierrePeriodoAnterior.getFechaCierre();

          List<Movimiento> movsAnt = movimientoDao.findByTarjetaEnRangoPeriodo(
              t.getId(), desdeAnt, hastaAnt);

          BigDecimal totalPeriodoAnt = BigDecimal.ZERO;
          BigDecimal pagosPeriodoAnt = BigDecimal.ZERO;

          for (Movimiento m : movsAnt) {
            if ("EGRESO".equals(m.getCategoria())) {
              if (m.getCuotas() == 1) {
                totalPeriodoAnt = totalPeriodoAnt.add(m.getMonto());
                totalEgresos   += m.getMonto().doubleValue();
              } else {
                int nroCuota = cierreDao.calcularNroCuota(
                    t.getId(), m.getFecha(), desdeAnt, hastaAnt);
                if (nroCuota >= 1 && nroCuota <= m.getCuotas()) {
                  List<Cuota> cuotas = cuotaDao.findByMovimiento(m.getId());
                  for (Cuota c : cuotas) {
                    if (c.getNroCuota() == nroCuota) {
                      totalPeriodoAnt = totalPeriodoAnt.add(c.getMonto());
                      totalEgresos   += c.getMonto().doubleValue();
                      break;
                    }
                  }
                }
              }
            } else if ("PAGO".equals(m.getCategoria())) {
              pagosPeriodoAnt = pagosPeriodoAnt.add(m.getMonto());
              totalPagos     += m.getMonto().doubleValue();
            }
          }

          t.setTotalGastado(totalPeriodoAnt.doubleValue());
          t.setTotalPagado(pagosPeriodoAnt.doubleValue());
          t.setRestaAbonar(totalPeriodoAnt.subtract(pagosPeriodoAnt).doubleValue());

          // Calculamos consumos del período actual (lo que se está gastando este mes)
          BigDecimal totalPeriodoActual = BigDecimal.ZERO;
          if (cierrePeriodoActual != null) {
            CierreTarjeta cierreAntActual = cierreDao.findAnteriorPorTarjeta(
                t.getId(), cierrePeriodoActual.getFechaCierre());

            LocalDate desdeAct = (cierreAntActual != null)
                ? cierreAntActual.getFechaCierre().plusDays(1)
                : cierrePeriodoActual.getMes();
            LocalDate hastaAct = cierrePeriodoActual.getFechaCierre();

            List<Movimiento> movsAct = movimientoDao.findByTarjetaEnRangoPeriodo(
                t.getId(), desdeAct, hastaAct);

            for (Movimiento m : movsAct) {
              if ("EGRESO".equals(m.getCategoria())) {
                if (m.getCuotas() == 1) {
                  totalPeriodoActual = totalPeriodoActual.add(m.getMonto());
                } else {
                  int nroCuota = cierreDao.calcularNroCuota(
                      t.getId(), m.getFecha(), desdeAct, hastaAct);
                  if (nroCuota >= 1 && nroCuota <= m.getCuotas()) {
                    List<Cuota> cuotas = cuotaDao.findByMovimiento(m.getId());
                    for (Cuota c : cuotas) {
                      if (c.getNroCuota() == nroCuota) {
                        totalPeriodoActual = totalPeriodoActual.add(c.getMonto());
                        break;
                      }
                    }
                  }
                }
              }
            }
          }

          cardsPane.add(
              crearCard(t, cierrePeriodoAnterior, cierrePeriodoActual, totalPeriodoActual),
              col(tarjetas, t), row(tarjetas, t));

        } else {
          cardsPane.add(crearCardSinCierre(t), col(tarjetas, t), row(tarjetas, t));
        }
      }

      double balance = totalIngresos - totalEgresos;
      lblIngresos.setText("Ingresos: " + CURRENCY.format(totalIngresos));
      lblEgresos.setText("Egresos: "   + CURRENCY.format(totalEgresos));
      lblBalance.setText("Balance: "   + CURRENCY.format(balance));

      cardIngresos.getChildren().add(crearCardIngresos(totalIngresos));

    } catch (Exception ex) {
      logger.error("Error al recargar dashboard", ex);
    }
  }

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

    Button btnDetalle = new Button("Ver detalle");
    btnDetalle.setMaxWidth(Double.MAX_VALUE);
    btnDetalle.setOnAction(e -> abrirDetalleIngresos());

    card.getChildren().addAll(lblTitulo, lblTotal, new Separator(), btnDetalle);
    card.setStyle("-fx-padding:20; -fx-alignment:center;");
    return card;
  }

  /**
   * Card con tres columnas:
   * - Izquierda: fechas (cierre y vencimiento del período anterior)
   * - Centro: período anterior — lo que se abona este mes
   * - Derecha: período actual — lo que se está gastando este mes
   */
  private VBox crearCard(Tarjeta t, CierreTarjeta cierreAnt,
                         CierreTarjeta cierreAct, BigDecimal totalActual) {
    VBox card = new VBox(8);
    card.getStyleClass().add("card");
    card.setPrefWidth(560);
    card.setStyle("-fx-padding:16;");

    Label lblNombre = new Label(t.getNombre());
    lblNombre.getStyleClass().add("card-title");

    // Grid de 3 columnas
    GridPane grid = new GridPane();
    grid.setHgap(12);
    grid.setVgap(4);
    ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(33);
    ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(34);
    ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(33);
    grid.getColumnConstraints().addAll(c1, c2, c3);

    // Nombre del mes anterior (período que se abona)
    String mesAnt = cierreAnt.getFechaCierre().getMonth()
        .getDisplayName(TextStyle.FULL, Locale.of("es"));
    mesAnt = mesAnt.substring(0, 1).toUpperCase() + mesAnt.substring(1);

    // Nombre del mes actual
    String mesAct = mesVisible.getMonth()
        .getDisplayName(TextStyle.FULL, Locale.of("es"));
    mesAct = mesAct.substring(0, 1).toUpperCase() + mesAct.substring(1);

    // --- Col 0: Fechas ---
    Label lblFechaTitulo = new Label("Fechas");
    lblFechaTitulo.getStyleClass().add("card-label-key");

    Label lblCierreLabel = new Label("Cierre:");
    lblCierreLabel.getStyleClass().add("card-label-key");
    Label lblCierreVal = new Label(cierreAnt.getFechaCierre().format(DATE_FMT));
    lblCierreVal.getStyleClass().add("card-label-val");

    Label lblVencLabel = new Label("Vencimiento:");
    lblVencLabel.getStyleClass().add("card-label-key");
    Label lblVencVal = new Label(cierreAnt.getFechaVencimiento().format(DATE_FMT));
    lblVencVal.getStyleClass().add("card-label-val");

    // --- Col 1: Período anterior (lo que se abona) ---
    Label lblAntTitulo = new Label("Movimientos " + mesAnt);
    lblAntTitulo.getStyleClass().add("card-label-key");

    Label lblGastosLabel = new Label("Gastos:");
    lblGastosLabel.getStyleClass().add("card-label-key");
    Label lblGastosVal = new Label(CURRENCY.format(t.getTotalGastado()));
    lblGastosVal.getStyleClass().add("card-label-val");

    Label lblPagosLabel = new Label("Pagos:");
    lblPagosLabel.getStyleClass().add("card-label-key");
    Label lblPagosVal = new Label(CURRENCY.format(t.getTotalPagado()));
    lblPagosVal.setStyle("-fx-font-size:12; -fx-text-fill:#27ae60;");

    Label lblRestaLabel = new Label("Resta:");
    lblRestaLabel.getStyleClass().add("card-label-key");
    Label lblRestaVal = new Label(CURRENCY.format(t.getRestaAbonar()));
    lblRestaVal.setStyle(t.getRestaAbonar() > 0
        ? "-fx-font-size:12; -fx-font-weight:bold; -fx-text-fill:#c0392b;"
        : "-fx-font-size:12; -fx-font-weight:bold; -fx-text-fill:#27ae60;");

    // --- Col 2: Período actual (lo que se está gastando) ---
    Label lblActTitulo = new Label("Movimientos " + mesAct);
    lblActTitulo.getStyleClass().add("card-label-key");

    Label lblActGastosLabel = new Label("Gastos:");
    lblActGastosLabel.getStyleClass().add("card-label-key");
    Label lblActGastosVal = new Label(cierreAct != null
        ? CURRENCY.format(totalActual) : "Sin cierre");
    lblActGastosVal.getStyleClass().add("card-label-val");

    // Armamos el grid
    grid.add(lblFechaTitulo,  0, 0);
    grid.add(lblCierreLabel,  0, 1);
    grid.add(lblCierreVal,    0, 2);
    grid.add(lblVencLabel,    0, 3);
    grid.add(lblVencVal,      0, 4);

    grid.add(lblAntTitulo,    1, 0);
    grid.add(lblGastosLabel,  1, 1);
    grid.add(lblGastosVal,    1, 2);
    grid.add(lblPagosLabel,   1, 3);
    grid.add(lblPagosVal,     1, 4);
    grid.add(lblRestaLabel,   1, 5);
    grid.add(lblRestaVal,     1, 6);

    grid.add(lblActTitulo,    2, 0);
    grid.add(lblActGastosLabel, 2, 1);
    grid.add(lblActGastosVal,   2, 2);

    // Botones
    Button btnDetalle = crearBotonDetalle(t);
    HBox.setHgrow(btnDetalle, Priority.ALWAYS);
    btnDetalle.setMaxWidth(Double.MAX_VALUE);

    Button btnEditar = crearBotonEditarTarjeta(t);
    HBox.setHgrow(btnEditar, Priority.ALWAYS);
    btnEditar.setMaxWidth(Double.MAX_VALUE);

    HBox hboxBotones;
    if (!"DEBITO".equals(t.getTipo())) {
      // Solo crédito tiene botón Pagar
      Button btnPagar = crearBotonPagar(t, cierreAnt);
      HBox.setHgrow(btnPagar, Priority.ALWAYS);
      btnPagar.setMaxWidth(Double.MAX_VALUE);
      hboxBotones = new HBox(6, btnDetalle, btnPagar, btnEditar);
    } else {
      hboxBotones = new HBox(6, btnDetalle, btnEditar);
    }
    hboxBotones.setMaxWidth(Double.MAX_VALUE);

    card.getChildren().addAll(lblNombre, new Separator(), grid, new Separator(), hboxBotones);
    return card;
  }

  private VBox crearCardSinCierre(Tarjeta t) {
    VBox card = new VBox(8);
    card.getStyleClass().add("card");
    card.setPrefWidth(560);
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

  // --- Botón Pagar — abre modal con monto, registra pago con fecha de cierre del período ---

  private Button crearBotonPagar(Tarjeta t, CierreTarjeta cierreAnt) {
    Button btn = new Button("Pagar");
    btn.setStyle("-fx-background-color:#27ae60; -fx-text-fill:white; -fx-font-weight:bold;");
    btn.setOnAction(e -> {
      // Modal con solo campo de monto
      Dialog<ButtonType> dialog = new Dialog<>();
      dialog.setTitle("Pagar tarjeta");
      dialog.setHeaderText("Pagar: " + t.getNombre());

      TextField txtMonto = new TextField();
      txtMonto.setPromptText("Ingrese el monto a pagar");

      // Prellenamos con el saldo restante si hay algo pendiente
      if (t.getRestaAbonar() > 0) {
        txtMonto.setText(String.format("%.2f", t.getRestaAbonar()));
      }

      javafx.scene.layout.VBox contenido = new javafx.scene.layout.VBox(8,
          new Label("Monto:"), txtMonto
      );
      contenido.setStyle("-fx-padding:10;");
      dialog.getDialogPane().setContent(contenido);

      ButtonType btnConfirmar = new ButtonType("Pagar", ButtonBar.ButtonData.OK_DONE);
      ButtonType btnCancelar  = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
      dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, btnCancelar);

      // Focus en el campo de monto al abrir
      Platform.runLater(txtMonto::requestFocus);

      dialog.showAndWait().ifPresent(respuesta -> {
        if (respuesta == btnConfirmar) {
          try {
            BigDecimal monto = new BigDecimal(txtMonto.getText().trim()).setScale(2);

            // El pago se registra con la fecha de cierre del período anterior
            // así queda contabilizado en el período correcto
            LocalDate fechaPago = cierreAnt.getFechaCierre();

            Movimiento pago = new Movimiento(
                t.getId(),
                fechaPago,
                "PAGO " + t.getNombre().toUpperCase(),
                monto,
                "ARS"
            );

            new MovimientoDao().save(pago);
            MovimientoEventBus.publish("pago");
            Toast.show((Stage) lblBalance.getScene().getWindow(),
                "Pago registrado: " + CURRENCY.format(monto));
            logger.info("Pago registrado: {} - ${} - fecha {}",
                t.getNombre(), monto, fechaPago);

          } catch (NumberFormatException ex) {
            Toast.show((Stage) lblBalance.getScene().getWindow(), "Monto inválido");
          } catch (Exception ex) {
            Toast.show((Stage) lblBalance.getScene().getWindow(), "Error al registrar pago");
            logger.error("Error al registrar pago de tarjeta {}", t.getNombre(), ex);
          }
        }
      });
    });
    return btn;
  }

  private Button crearBotonDetalle(Tarjeta t) {
    Button btn = new Button("Ver detalle");
    btn.setOnAction(e -> {
      try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/gastos/detalle.fxml"));
        Scene scene = new Scene(loader.load(), 950, 600);
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

  private void abrirDetalleIngresos() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/gastos/ingreso.fxml"));
      Scene scene = new Scene(loader.load(), 620, 600);
      IngresoController ctrl = loader.getController();
      ctrl.setMesVisible(mesVisible);
      Stage stage = new Stage();
      stage.setTitle("Ingresos - " + mesVisible.getMonth()
          .getDisplayName(TextStyle.FULL, Locale.of("es")));
      stage.setScene(scene);
      stage.show();
    } catch (IOException ex) {
      logger.error("Error al abrir detalle ingresos", ex);
    }
  }

  @FXML private void ejecutarBackup() {
    try {
      String path = BackupService.ejecutarBackup();
      Toast.show((Stage) lblBalance.getScene().getWindow(), "Backup guardado: " + path);
      logger.info("Backup ejecutado correctamente: {}", path);
    } catch (Exception ex) {
      Toast.show((Stage) lblBalance.getScene().getWindow(), "Error al ejecutar backup: " + ex.getMessage());
      logger.error("Error al ejecutar backup", ex);
    }
  }

  @FXML private void abrirRestaurarBackup() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/gastos/restaurar-backup.fxml"));
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
