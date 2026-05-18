package ar.com.gastos.controller;

import ar.com.gastos.dao.GastoRecurrenteDao;
import ar.com.gastos.dao.MovimientoDao;
import ar.com.gastos.dao.TarjetaDao;
import ar.com.gastos.model.GastoRecurrente;
import ar.com.gastos.model.GenerarMesItem;
import ar.com.gastos.model.Movimiento;
import ar.com.gastos.model.Tarjeta;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GenerarMesController {

  private static final Logger logger = LoggerFactory.getLogger(GenerarMesController.class);

  @FXML private DatePicker dpMes;
  @FXML private ListView<GenerarMesItem> lstItems;

  @FXML
  public void initialize() {
    // Mes por defecto: el actual
    dpMes.setValue(LocalDate.now().withDayOfMonth(1));

    // Cada celda muestra: [checkbox] [descripción - medio pago] [campo monto]
    lstItems.setCellFactory(lv -> new ListCell<>() {
      private final CheckBox chk = new CheckBox();
      private final javafx.scene.control.Label lblDesc = new javafx.scene.control.Label();
      private final TextField txtMonto = new TextField();
      private final HBox hbox = new HBox(10, chk, lblDesc, txtMonto);

      {
        HBox.setHgrow(lblDesc, javafx.scene.layout.Priority.ALWAYS);
        txtMonto.setPromptText("0.00");
        txtMonto.setPrefWidth(90);
      }

      // Guardamos el item anterior para desconectar sus listeners
      private GenerarMesItem itemActual = null;

      @Override
      protected void updateItem(GenerarMesItem item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
          setGraphic(null);
          // Desconectamos bindings del item anterior si existía
          if (itemActual != null) {
            chk.selectedProperty().unbindBidirectional(itemActual.seleccionadoProperty());
            txtMonto.textProperty().unbindBidirectional(itemActual.montoProperty());
            itemActual = null;
          }
          return;
        }

        // Desconectamos el item anterior antes de bindear el nuevo
        if (itemActual != null) {
          chk.selectedProperty().unbindBidirectional(itemActual.seleccionadoProperty());
          txtMonto.textProperty().unbindBidirectional(itemActual.montoProperty());
        }

        itemActual = item;
        chk.selectedProperty().bindBidirectional(item.seleccionadoProperty());
        lblDesc.setText(item.getGastoRecurrente().getDescripcion()
            + " (" + item.getGastoRecurrente().getMedioPago() + ")");
        txtMonto.textProperty().bindBidirectional(item.montoProperty());
        setGraphic(hbox);
      }
    });

    cargarRecurrentes();
  }

  // --- Carga solo los recurrentes DEBITO y EFECTIVO ---

  private void cargarRecurrentes() {
    lstItems.getItems().clear();
    try {
      GastoRecurrenteDao dao = new GastoRecurrenteDao();
      List<GastoRecurrente> todos = dao.findAll();
      for (GastoRecurrente g : todos) {
        if ("DEBITO".equals(g.getMedioPago()) || "EFECTIVO".equals(g.getMedioPago())) {
          lstItems.getItems().add(new GenerarMesItem(g));
        }
      }
    } catch (SQLException ex) {
      logger.error("Error al cargar recurrentes", ex);
    }
  }

  // --- Genera los egresos reales en la tabla movimientos ---

  @FXML
  private void generarEgresos() {
    LocalDate fechaMes = dpMes.getValue();
    if (fechaMes == null) {
      Toast.show(getStage(), "Seleccione el mes a generar");
      return;
    }

    // Buscamos la tarjeta DEBITO — todos los recurrentes van ahí
    Tarjeta tarjetaDebito;
    try {
      TarjetaDao tarjetaDao = new TarjetaDao();
      tarjetaDebito = tarjetaDao.findByTipo("DEBITO");
      if (tarjetaDebito == null) {
        Toast.show(getStage(), "No se encontró tarjeta de tipo DEBITO");
        return;
      }
    } catch (SQLException ex) {
      Toast.show(getStage(), "Error al buscar tarjeta débito");
      logger.error("Error al buscar tarjeta DEBITO", ex);
      return;
    }

    // Procesamos solo los items seleccionados con monto válido
    List<String> errores = new ArrayList<>();
    int generados = 0;

    for (GenerarMesItem item : lstItems.getItems()) {
      if (!item.isSeleccionado()) continue;

      String montoStr = item.getMonto().trim();
      if (montoStr.isEmpty()) continue; // sin monto lo saltamos silenciosamente

      try {
        BigDecimal monto = new BigDecimal(montoStr).setScale(2);
        GastoRecurrente g = item.getGastoRecurrente();

        Movimiento mov = new Movimiento(
            tarjetaDebito.getId(),
            fechaMes,                       // fecha = primer día del mes
            g.getDescripcion(),
            monto,
            "EGRESO",
            "ARS",
            1                               // siempre cuota única
        );

        MovimientoDao dao = new MovimientoDao();
        dao.save(mov);
        generados++;
        logger.info("Recurrente generado: {} - ${} - {}", g.getDescripcion(), monto, fechaMes);

      } catch (NumberFormatException ex) {
        errores.add(item.getGastoRecurrente().getDescripcion() + ": monto inválido");
      } catch (SQLException ex) {
        errores.add(item.getGastoRecurrente().getDescripcion() + ": error al guardar");
        logger.error("Error al guardar recurrente: {}", item.getGastoRecurrente().getDescripcion(), ex);
      }
    }

    // Notificamos al dashboard para que se recargue
    if (generados > 0) {
      MovimientoEventBus.publish("recurrentes");
    }

    // Feedback al usuario
    if (errores.isEmpty()) {
      Toast.show(getStage(), generados + " egreso(s) generado(s) correctamente");
    } else {
      Toast.show(getStage(), generados + " generado(s). Errores: " + String.join(", ", errores));
    }
  }

  // --- Helper para obtener el Stage ---

  private Stage getStage() {
    return (Stage) dpMes.getScene().getWindow();
  }
}