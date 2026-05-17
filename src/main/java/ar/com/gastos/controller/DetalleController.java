package ar.com.gastos.controller;

import ar.com.gastos.dao.CuotaDao;
import ar.com.gastos.dao.MovimientoDao;
import ar.com.gastos.model.Cuota;
import ar.com.gastos.model.Movimiento;
import ar.com.gastos.model.Tarjeta;
import ar.com.gastos.util.MovimientoEventBus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;

public class DetalleController {

  @FXML
  private Label lblTitulo;
  @FXML
  private TableView<Movimiento> tablaMovimientos;
  @FXML
  private TableColumn<Movimiento, LocalDate> colFecha;
  @FXML
  private TableColumn<Movimiento, String> colDescripcion;
  @FXML
  private TableColumn<Movimiento, java.math.BigDecimal> colMonto;
  @FXML
  private TableColumn<Movimiento, String> colCuota;


  private Tarjeta tarjetaActual;

  public void setTarjeta(Tarjeta tarjeta) {
    this.tarjetaActual = tarjeta;
    lblTitulo.setText("Detalle de " + tarjeta.getNombre());
    recargarMovimientos();
  }

  private void recargarMovimientos() {
    if (tarjetaActual != null) {
      try {
        MovimientoDao movimientoDao = new MovimientoDao();
        List<Movimiento> movimientos = movimientoDao.findByTarjeta(tarjetaActual.getId());

        CuotaDao cuotaDao = new CuotaDao();

        for (Movimiento m : movimientos) {
          if (m.getCuotas() == 1) {
            m.setCuotaTexto("Pago único");
          } else {
            List<Cuota> cuotas = cuotaDao.findByMovimiento(m.getId());

            // Mostrar la primera cuota pendiente
            cuotas.stream()
                .filter(c -> "pendiente".equals(c.getEstado()))
                .findFirst()
                .ifPresent(c -> {
                  m.setMonto(c.getMonto()); // 🔹 monto de la cuota
                  m.setCuotaTexto(c.getNroCuota() + " de " + m.getCuotas());
                });
          }
        }

        tablaMovimientos.setItems(FXCollections.observableArrayList(movimientos));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }



  @FXML
  public void initialize() {
    colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
    colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
    colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
    colCuota.setCellValueFactory(new PropertyValueFactory<>("cuotaTexto"));

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    colFecha.setCellFactory(column -> new TableCell<Movimiento, LocalDate>() {
      @Override
      protected void updateItem(LocalDate fecha, boolean empty) {
        super.updateItem(fecha, empty);
        setText(empty || fecha == null ? null : formatter.format(fecha));
      }
    });

    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
    currencyFormat.setMaximumFractionDigits(2);

    colMonto.setCellFactory(column -> new TableCell<Movimiento, java.math.BigDecimal>() {
      @Override
      protected void updateItem(java.math.BigDecimal monto, boolean empty) {
        super.updateItem(monto, empty);
        setText(empty || monto == null ? null : currencyFormat.format(monto));
      }
    });


    MovimientoEventBus.subscribe(tarjetaNombre -> {
      if (tarjetaActual != null && tarjetaActual.getNombre().equalsIgnoreCase(tarjetaNombre)) {
        Platform.runLater(this::recargarMovimientos);
      }
    });
  }
}

