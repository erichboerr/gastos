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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GenerarMesController {

  private static final Logger logger = LoggerFactory.getLogger(GenerarMesController.class);

  @FXML private ListView<GenerarMesItem> lstItems;

  @FXML
  public void initialize() {
    lstItems.setCellFactory(lv -> new ListCell<>() {

      private final CheckBox   chk      = new CheckBox();
      private final Label      lblDesc  = new Label();
      private final DatePicker dpFecha  = new DatePicker();
      private final TextField  txtMonto = new TextField();
      private final HBox       hbox     = new HBox(8, chk, lblDesc, dpFecha, txtMonto);

      {
        HBox.setHgrow(lblDesc, Priority.ALWAYS);
        dpFecha.setPrefWidth(130);
        dpFecha.setValue(LocalDate.now());
        txtMonto.setPromptText("0.00");
        txtMonto.setPrefWidth(90);

        // El DatePicker y el monto arrancan deshabilitados
        // Se habilitan solo cuando el checkbox está marcado
        dpFecha.setDisable(true);
        txtMonto.setDisable(true);

        chk.selectedProperty().addListener((obs, anterior, seleccionado) -> {
          dpFecha.setDisable(!seleccionado);
          txtMonto.setDisable(!seleccionado);
          if (!seleccionado) {
            txtMonto.clear();
          }
        });
      }

      private GenerarMesItem itemActual = null;

      @Override
      protected void updateItem(GenerarMesItem item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
          setGraphic(null);
          if (itemActual != null) {
            chk.selectedProperty().unbindBidirectional(itemActual.seleccionadoProperty());
            dpFecha.valueProperty().unbindBidirectional(itemActual.fechaProperty());
            txtMonto.textProperty().unbindBidirectional(itemActual.montoProperty());
            itemActual = null;
          }
          return;
        }

        if (itemActual != null) {
          chk.selectedProperty().unbindBidirectional(itemActual.seleccionadoProperty());
          dpFecha.valueProperty().unbindBidirectional(itemActual.fechaProperty());
          txtMonto.textProperty().unbindBidirectional(itemActual.montoProperty());
        }

        itemActual = item;
        chk.selectedProperty().bindBidirectional(item.seleccionadoProperty());
        dpFecha.valueProperty().bindBidirectional(item.fechaProperty());
        txtMonto.textProperty().bindBidirectional(item.montoProperty());

        lblDesc.setText(item.getGastoRecurrente().getDescripcion());

        // Sincronizamos el estado visual con el modelo
        dpFecha.setDisable(!item.isSeleccionado());
        txtMonto.setDisable(!item.isSeleccionado());

        setGraphic(hbox);
      }
    });

    cargarRecurrentes();
  }

  // --- Carga los recurrentes DEBITO ---

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

  // --- Genera los egresos seleccionados ---

  @FXML
  private void generarEgresos() {
    // Buscamos la tarjeta DEBITO
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

    // Verificamos que haya al menos un ítem seleccionado con fecha y monto
    boolean haySeleccionados = lstItems.getItems().stream()
        .anyMatch(i -> i.isSeleccionado() && !i.getMonto().trim().isEmpty());

    if (!haySeleccionados) {
      Toast.show(getStage(), "Seleccioná al menos un ítem con monto");
      return;
    }

    List<String> errores = new ArrayList<>();
    int generados = 0;

    for (GenerarMesItem item : lstItems.getItems()) {
      if (!item.isSeleccionado()) continue;

      String montoStr = item.getMonto().trim();
      if (montoStr.isEmpty()) continue;

      LocalDate fecha = item.getFecha();
      if (fecha == null) {
        errores.add(item.getGastoRecurrente().getDescripcion() + ": fecha inválida");
        continue;
      }

      try {
        BigDecimal monto = new BigDecimal(montoStr).setScale(2);
        GastoRecurrente g = item.getGastoRecurrente();

        Movimiento mov = new Movimiento(
            tarjetaDebito.getId(),
            fecha,
            g.getDescripcion().toUpperCase().trim(),
            monto,
            "EGRESO",
            "ARS"
        );

        new MovimientoDao().save(mov);
        generados++;
        logger.info("Recurrente generado: {} - ${} - {}", g.getDescripcion(), monto, fecha);

      } catch (NumberFormatException ex) {
        errores.add(item.getGastoRecurrente().getDescripcion() + ": monto inválido");
      } catch (SQLException ex) {
        errores.add(item.getGastoRecurrente().getDescripcion() + ": error al guardar");
        logger.error("Error al guardar recurrente: {}", item.getGastoRecurrente().getDescripcion(), ex);
      }
    }

    if (generados > 0) {
      MovimientoEventBus.publish("recurrentes");
    }

    if (errores.isEmpty()) {
      Toast.show(getStage(), generados + " egreso(s) generado(s) correctamente");
    } else {
      Toast.show(getStage(), generados + " generado(s). Errores: " + String.join(", ", errores));
    }

    limpiar();
  }

  // --- Limpia la selección y montos ---

  @FXML
  private void limpiar() {
    for (GenerarMesItem item : lstItems.getItems()) {
      item.setSeleccionado(false);
      item.setMonto("");
      item.setFecha(LocalDate.now());
    }
  }

  @FXML
  private void cerrar() {
    getStage().close();
  }

  private Stage getStage() {
    return (Stage) lstItems.getScene().getWindow();
  }
}