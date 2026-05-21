package ar.com.gastos.controller;

import ar.com.gastos.dao.*;
import ar.com.gastos.model.*;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class ResumenAnualController {

  private static final Logger logger = LoggerFactory.getLogger(ResumenAnualController.class);
  private static final Locale LOCALE_ES = new Locale("es", "AR");
  private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(LOCALE_ES);

  static {
    CURRENCY.setMaximumFractionDigits(2);
  }

  @FXML private Label lblAnio;
  @FXML private TableView<FilaMes> tablaResumen;
  @FXML private TableColumn<FilaMes, String>  colMes;
  @FXML private TableColumn<FilaMes, String>  colIngresos;
  @FXML private TableColumn<FilaMes, String>  colEgresos;
  @FXML private TableColumn<FilaMes, String>  colBalance;
  @FXML private Label lblTotalIngresos;
  @FXML private Label lblTotalEgresos;
  @FXML private Label lblTotalBalance;

  // Llamado desde DashboardController al abrir la ventana
  public void cargarAnio(int anio) {
    lblAnio.setText("Resumen " + anio);

    colMes.setCellValueFactory(new PropertyValueFactory<>("mes"));
    colIngresos.setCellValueFactory(new PropertyValueFactory<>("ingresos"));
    colEgresos.setCellValueFactory(new PropertyValueFactory<>("egresos"));
    colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));

    tablaResumen.getItems().clear();

    double sumaIngresos = 0;
    double sumaEgresos  = 0;

    try {
      IngresoDao ingresoDao = new IngresoDao();
      TarjetaDao tarjetaDao = new TarjetaDao();
      MovimientoDao movimientoDao = new MovimientoDao();
      CierreTarjetaDao cierreDao  = new CierreTarjetaDao();
      CuotaDao cuotaDao = new CuotaDao();

      List<Ingreso> todosLosIngresos = ingresoDao.listarIngresos();
      List<Tarjeta> tarjetas         = tarjetaDao.findAllActivas();

      // Iteramos los 12 meses del año
      for (Month month : Month.values()) {
        YearMonth ym = YearMonth.of(anio, month);

        // Ingresos del mes
        double ingMes = todosLosIngresos.stream()
              .filter(i -> YearMonth.from(i.getFecha()).equals(ym))
              .map(Ingreso::getMonto)
              .mapToDouble(BigDecimal::doubleValue)
              .sum();


        // Egresos del mes (sumamos todas las tarjetas)

        double egrMes = 0;
        for (Tarjeta t : tarjetas) {
          CierreTarjeta cierreMes = cierreDao.findCierrePorMes(t.getId(), ym);
          if (cierreMes != null) {
            CierreTarjeta anterior = cierreDao.findAnteriorPorTarjeta(
                t.getId(), cierreMes.getFechaCierre());

            LocalDate desde = (anterior != null)
                ? anterior.getFechaCierre().plusDays(1)
                : cierreMes.getMes();
            LocalDate hasta = cierreMes.getFechaCierre();

            List<Movimiento> movs = movimientoDao.findByTarjetaEnRangoPeriodo(
                t.getId(), desde, hasta);

            for (Movimiento m : movs) {
              if ("EGRESO".equals(m.getCategoria())) {
                if (m.getCuotas() == 1) {
                  // Pago único — suma el monto directamente
                  egrMes += m.getMonto().doubleValue();
                } else {
                  // En cuotas — sumamos solo la cuota que vence en el período
                  List<ar.com.gastos.model.Cuota> cuotas =
                      cuotaDao.findByMovimiento(m.getId());
                  for (ar.com.gastos.model.Cuota c : cuotas) {
                    if (!c.getFechaVencimiento().isBefore(desde) &&
                        !c.getFechaVencimiento().isAfter(hasta)) {
                      egrMes += c.getMonto().doubleValue();
                      break;
                    }
                  }
                }
              }
            }
          }
        }

        // Nombre del mes capitalizado — ej: "Enero", "Febrero"
        String nombreMes = month.getDisplayName(TextStyle.FULL, new Locale("es"));
        nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);

        double balMes = ingMes - egrMes;
        tablaResumen.getItems().add(new FilaMes(nombreMes,
              CURRENCY.format(ingMes),
              CURRENCY.format(egrMes),
              CURRENCY.format(balMes)));

        sumaIngresos += ingMes;
        sumaEgresos  += egrMes;
      }

      // Totales al pie de la tabla
      lblTotalIngresos.setText("Total ingresos: " + CURRENCY.format(sumaIngresos));
      lblTotalEgresos.setText("Total egresos: "   + CURRENCY.format(sumaEgresos));
      lblTotalBalance.setText("Balance anual: "   + CURRENCY.format(sumaIngresos - sumaEgresos));

    } catch (Exception ex) {
      logger.error("Error al cargar resumen anual", ex);
    }
  }

  // --- Clase interna para las filas de la TableView ---
  // JavaFX TableView necesita una clase con propiedades para poder usar PropertyValueFactory.
  public static class FilaMes {
    private final String mes;
    private final String ingresos;
    private final String egresos;
    private final String balance;

    public FilaMes(String mes, String ingresos, String egresos, String balance) {
      this.mes      = mes;
      this.ingresos = ingresos;
      this.egresos  = egresos;
      this.balance  = balance;
    }

    public String getMes()      { return mes; }
    public String getIngresos() { return ingresos; }
    public String getEgresos()  { return egresos; }
    public String getBalance()  { return balance; }
  }
}