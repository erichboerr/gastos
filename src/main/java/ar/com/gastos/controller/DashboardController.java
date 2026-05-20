package ar.com.gastos.controller;

import ar.com.gastos.dao.*;
import ar.com.gastos.model.*;
import ar.com.gastos.util.MovimientoEventBus;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class DashboardController {

  private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

  // El mes que se está mostrando actualmente en el dashboard.
  // Arranca en el mes actual y cambia con las flechas de navegación.
  private YearMonth mesVisible = YearMonth.now();

  private static final Locale LOCALE_ES = new Locale("es", "AR");
  private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(LOCALE_ES);

  static {
    CURRENCY.setMaximumFractionDigits(2);
  }

  @FXML
  private Label lblBalance;
  @FXML
  private Label lblIngresos;
  @FXML
  private Label lblEgresos;
  @FXML
  private Label lblMesNavegacion;
  @FXML
  private GridPane cardsPane;
  @FXML
  private VBox cardIngresos;

  private Consumer<String> subscriber;

  @FXML
  public void initialize() {
    recargarDashboard();

    // Guardamos la referencia para poder desuscribir cuando se cierre la ventana
    subscriber = nombre -> Platform.runLater(this::recargarDashboard);
    MovimientoEventBus.subscribe(subscriber);

    // Cuando el dashboard se cierra, limpiamos el subscriber
    // para que no queden referencias acumuladas en el EventBus
    Platform.runLater(() -> {
      Stage stage = (Stage) lblBalance.getScene().getWindow();
      stage.setOnCloseRequest(e -> MovimientoEventBus.unsubscribe(subscriber));
    });
  }

  // --- Navegación por mes ---

  @FXML
  private void meAnterior() {
    mesVisible = mesVisible.minusMonths(1);
    recargarDashboard();
  }

  @FXML
  private void meSiguiente() {
    mesVisible = mesVisible.plusMonths(1);
    recargarDashboard();
  }

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
    } catch (IOException ex) {
      logger.error("Error al abrir ingresos", ex);
    }
  }

  @FXML private void abrirNuevaTarjeta() { abrirFormulario("/ar/com/gastos/nueva-tarjeta.fxml", "Nueva Tarjeta"); }
  @FXML
  private void abrirEgreso() {
    abrirFormulario("/ar/com/gastos/egreso.fxml", "Nuevo Egreso");
  }

  @FXML
  private void abrirPago() {
    abrirFormulario("/ar/com/gastos/pago.fxml", "Registrar Pago");
  }

  @FXML
  private void abrirCierreTarjeta() {
    abrirFormulario("/ar/com/gastos/CierreTarjeta.fxml", "Cierre de Tarjeta");
  }

  @FXML
  private void abrirRecurrentes() {
    abrirFormulario("/ar/com/gastos/gastos-recurrentes.fxml", "Gastos Recurrentes");
  }

  @FXML
  private void abrirGenerarMes() {
    abrirFormulario("/ar/com/gastos/generar-mes.fxml", "Generar Mes");
  }

  // Abre la ventana de resumen anual para el año del mes visible
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
    } catch (IOException ex) {
      logger.error("Error al abrir resumen anual", ex);
    }
  }



  // Metodo genérico para abrir cualquier formulario en una nueva ventana modal
  private void abrirFormulario(String fxmlPath, String titulo) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Scene scene = new Scene(loader.load(), 400, 420);
      Stage stage = new Stage();
      stage.setTitle(titulo);
      stage.setScene(scene);
      stage.show();
    } catch (IOException ex) {
      logger.error("Error al abrir formulario: {}", fxmlPath, ex);
    }
  }

  // --- Carga del dashboard ---

  private void recargarDashboard() {
    cardsPane.getChildren().clear();
    cardIngresos.getChildren().clear();

    // Actualizamos el label de navegación — ej: "Mayo 2026"
    String nombreMes = mesVisible.getMonth()
        .getDisplayName(TextStyle.FULL, new Locale("es"));
    String nombreMesCap = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
    lblMesNavegacion.setText(nombreMesCap + " " + mesVisible.getYear());

    try {
      TarjetaDao tarjetaDao = new TarjetaDao();
      MovimientoDao movimientoDao = new MovimientoDao();
      IngresoDao ingresoDao = new IngresoDao();
      CierreTarjetaDao cierreDao = new CierreTarjetaDao();
      CuotaDao cuotaDao = new CuotaDao();

      List<Tarjeta> tarjetas = tarjetaDao.findAllActivas();

      // Ingresos filtrados solo por el mes visible
      double totalIngresos = ingresoDao.listarIngresos().stream()
          .filter(i -> YearMonth.from(i.getFecha()).equals(mesVisible))
          .map(Ingreso::getMonto)
          .mapToDouble(BigDecimal::doubleValue)
          .sum();

      double totalEgresos = 0;
      double totalPagos = 0;

      for (Tarjeta t : tarjetas) {

        // FIX: usamos mesVisible en lugar de YearMonth.now() para que
        // la navegación por mes y el EventBus recarguen correctamente.
        CierreTarjeta cierreMes = cierreDao.findCierrePorMes(t.getId(), mesVisible);

        if (cierreMes != null) {

          // Buscamos el cierre anterior para determinar el inicio del período
          CierreTarjeta cierreAnterior = cierreDao.findAnteriorPorTarjeta(
              t.getId(), cierreMes.getFechaCierre());

          // Si hay cierre anterior: el período arranca el día siguiente a ese cierre.
          // Si no hay cierre anterior: arranca desde el primer día del mes del cierre.
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
                // Pago único — suma el monto del movimiento directamente
                totalPeriodo = totalPeriodo.add(m.getMonto());
                totalEgresos += m.getMonto().doubleValue();

              } else {
                // En cuotas — buscamos cuáles vencen dentro del período del cierre
                List<Cuota> cuotas = cuotaDao.findByMovimiento(m.getId());
                for (Cuota c : cuotas) {
                  if (!c.getFechaVencimiento().isBefore(desde) &&
                      !c.getFechaVencimiento().isAfter(hasta)) {
                    totalPeriodo = totalPeriodo.add(c.getMonto());
                    totalEgresos += c.getMonto().doubleValue();
                  }
                }
              }

            } else if ("PAGO".equals(m.getCategoria())) {
              // Pagos realizados a la tarjeta en el período
              pagosPeriodo = pagosPeriodo.add(m.getMonto());
              totalPagos += m.getMonto().doubleValue();
            }
          }

          t.setTotalGastado(totalPeriodo.doubleValue());
          t.setTotalPagado(pagosPeriodo.doubleValue());
          t.setRestaAbonar(totalPeriodo.subtract(pagosPeriodo).doubleValue());
        }
      }

      double balance = totalIngresos - totalEgresos;
      logger.debug("Mes: {} | Ingresos: {} | Egresos: {} | Balance: {}",
          mesVisible, totalIngresos, totalEgresos, balance);

      // Actualizamos los tres labels del header
      lblIngresos.setText("Ingresos: " + CURRENCY.format(totalIngresos));
      lblEgresos.setText("Egresos: " + CURRENCY.format(totalEgresos));
      lblBalance.setText("Balance: " + CURRENCY.format(balance));

      // Card de ingresos arriba y cards de tarjetas en grilla
      cardIngresos.getChildren().add(crearCardIngresos(totalIngresos));

      int col = 0, row = 0;
      for (Tarjeta t : tarjetas) {
        cardsPane.add(crearCard(t), col, row);
        col++;
        if (col == 3) {
          col = 0;
          row++;
        }
      }

    } catch (Exception ex) {
      logger.error("Error al recargar dashboard", ex);
    }
  }

  // --- Cards ---

  private VBox crearCardIngresos(double totalIngresos) {
    VBox card = new VBox(6);
    card.getStyleClass().add("card");
    card.setPrefWidth(400);
    card.setPrefHeight(120);
    card.setMaxWidth(400);

    Label lblTitulo = new Label("Ingresos del mes");
    lblTitulo.getStyleClass().add("card-title");

    Label lblTotal = new Label("Total: " + CURRENCY.format(totalIngresos));
    lblTotal.getStyleClass().add("card-body");

    card.getChildren().addAll(lblTitulo, lblTotal);
    card.setStyle("-fx-padding:20; -fx-alignment:center;");
    return card;
  }

  private VBox crearCard(Tarjeta t) {
    VBox card = new VBox(6);
    card.getStyleClass().add("card");
    card.setPrefWidth(450);
    card.setPrefHeight(240);

    Label lblNombre = new Label(t.getNombre());
    lblNombre.getStyleClass().add("card-title");

    Label lblGastos = new Label("Gastos del mes: " + CURRENCY.format(t.getTotalGastado()));
    lblGastos.getStyleClass().add("card-body");

    Label lblPagos = new Label("Pagos del mes: " + CURRENCY.format(t.getTotalPagado()));
    lblPagos.getStyleClass().add("card-body");

    Label lblResta = new Label("Resta abonar: " + CURRENCY.format(t.getRestaAbonar()));
    lblResta.getStyleClass().add("card-body");

    Button btnDetalle = crearBotonDetalle(t);
    btnDetalle.getStyleClass().add("card-footer");
    HBox.setHgrow(btnDetalle, javafx.scene.layout.Priority.ALWAYS);
    btnDetalle.setMaxWidth(Double.MAX_VALUE);

    Button btnEditar = crearBotonEditarTarjeta(t);
    btnEditar.getStyleClass().add("card-footer");
    HBox.setHgrow(btnEditar, javafx.scene.layout.Priority.ALWAYS);
    btnEditar.setMaxWidth(Double.MAX_VALUE);

    HBox hboxBotones = new HBox(6, btnDetalle, btnEditar);
    hboxBotones.setMaxWidth(Double.MAX_VALUE);

    card.getChildren().addAll(lblNombre, new Separator(),
        lblGastos, lblPagos, lblResta, new Separator(), hboxBotones);

    card.setStyle("-fx-padding:20; -fx-alignment:center;");
    return card;
  }

  // Crea el botón "Ver detalle" que abre una nueva ventana con el movimiento de la tarjeta
  private Button crearBotonDetalle(Tarjeta t) {
    Button btn = new Button("Ver detalle");
    btn.setOnAction(e -> {
      try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/ar/com/gastos/detalle.fxml"));
        Scene scene = new Scene(loader.load(), 720, 600);
        DetalleController ctrl = loader.getController();
        ctrl.setTarjeta(t, mesVisible); // ← pasamos el mes visible
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
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/ar/com/gastos/editar-tarjeta.fxml"));
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


}