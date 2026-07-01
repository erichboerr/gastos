package ar.com.gastos.controller;

import ar.com.gastos.dao.*;
import ar.com.gastos.model.*;
import ar.com.gastos.util.Db;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

public class ResumenAnualController {

  private static final Logger logger = LoggerFactory.getLogger(ResumenAnualController.class);
  private static final Locale LOCALE_ES = new Locale("es", "AR");
  private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(LOCALE_ES);

  static { CURRENCY.setMaximumFractionDigits(2); }

  // --- Tab 1: Resumen mensual ---
  @FXML private Label lblAnio;
  @FXML private TableView<FilaMes> tablaResumen;
  @FXML private TableColumn<FilaMes, String> colMes;
  @FXML private TableColumn<FilaMes, String> colIngresos;
  @FXML private TableColumn<FilaMes, String> colEgresos;
  @FXML private TableColumn<FilaMes, String> colBalance;
  @FXML private Label lblTotalIngresos;
  @FXML private Label lblTotalEgresos;
  @FXML private Label lblTotalBalance;

  // --- Tab 2: Por tarjeta ---
  @FXML private ComboBox<String> cmbTarjeta;
  @FXML private TableView<FilaGeneral> tablaTarjeta;
  @FXML private Label lblTotTarjeta1;
  @FXML private Label lblTotTarjeta2;
  @FXML private Label lblTotTarjeta3;
  @FXML private Label lblTotTarjeta4;

  // --- Tab 3: Por categoría ---
  @FXML private TableView<FilaCategoria> tablaCategoria;
  @FXML private TableColumn<FilaCategoria, String> colCategoria;
  @FXML private TableColumn<FilaCategoria, String> colTotalCat;
  @FXML private TableColumn<FilaCategoria, String> colPorcentaje;
  @FXML private Label lblTotalCategoria;

  private int anioActual;
  private List<Tarjeta> tarjetas;

  // --- Entrada desde DashboardController ---
  public void cargarAnio(int anio) {
    this.anioActual = anio;
    lblAnio.setText("Resumen " + anio);

    try {
      TarjetaDao tarjetaDao = new TarjetaDao();
      tarjetas = tarjetaDao.findAllActivas();

      cargarResumenMensual();
      cargarSelectorTarjeta();
      cargarCategoria();

    } catch (Exception ex) {
      logger.error("Error al cargar resumen anual", ex);
    }
  }

  // =========================================================
  // TAB 1 — Resumen mensual
  // =========================================================

  private void cargarResumenMensual() throws Exception {
    colMes.setCellValueFactory(new PropertyValueFactory<>("col0"));
    colIngresos.setCellValueFactory(new PropertyValueFactory<>("col1"));
    colEgresos.setCellValueFactory(new PropertyValueFactory<>("col2"));
    colBalance.setCellValueFactory(new PropertyValueFactory<>("col3"));

    tablaResumen.getItems().clear();

    IngresoDao ingresoDao       = new IngresoDao();
    MovimientoDao movimientoDao = new MovimientoDao();
    CierreTarjetaDao cierreDao  = new CierreTarjetaDao();
    CuotaDao cuotaDao           = new CuotaDao();
    List<Ingreso> todosIngresos = ingresoDao.listarIngresos();

    double sumaIngresos = 0, sumaEgresos = 0;

    for (Month month : Month.values()) {
      YearMonth ym = YearMonth.of(anioActual, month);

      double ingMes = todosIngresos.stream()
          .filter(i -> YearMonth.from(i.getFecha()).equals(ym))
          .map(Ingreso::getMonto)
          .mapToDouble(BigDecimal::doubleValue).sum();

      double egrMes = calcularEgresosMes(ym, tarjetas, movimientoDao, cierreDao, cuotaDao);

      String nombreMes = capitalizar(month.getDisplayName(TextStyle.FULL, LOCALE_ES));
      double balMes = ingMes - egrMes;

      /*tablaResumen.getItems().add(new FilaGeneral(
          nombreMes,
          CURRENCY.format(ingMes),
          CURRENCY.format(egrMes),
          CURRENCY.format(balMes), "", ""));*/

      tablaResumen.getItems().add(new FilaMes(
          nombreMes,
          CURRENCY.format(ingMes),
          CURRENCY.format(egrMes),
          CURRENCY.format(balMes)
      ));

      sumaIngresos += ingMes;
      sumaEgresos  += egrMes;
    }

    lblTotalIngresos.setText("Total ingresos: " + CURRENCY.format(sumaIngresos));
    lblTotalEgresos .setText("Total egresos: "  + CURRENCY.format(sumaEgresos));
    lblTotalBalance .setText("Balance anual: "  + CURRENCY.format(sumaIngresos - sumaEgresos));
  }

  // =========================================================
  // TAB 2 — Por tarjeta
  // =========================================================

  private void cargarSelectorTarjeta() {
    cmbTarjeta.getItems().clear();
    cmbTarjeta.getItems().add("Todas");
    for (Tarjeta t : tarjetas) {
      cmbTarjeta.getItems().add(t.getNombre());
    }
    cmbTarjeta.setValue("Todas");
    cargarTablaTarjeta("Todas");
  }

  @FXML
  private void onTarjetaSeleccionada() {
    String seleccion = cmbTarjeta.getValue();
    if (seleccion == null) return;
    cargarTablaTarjeta(seleccion);
  }

  private void cargarTablaTarjeta(String seleccion) {
    tablaTarjeta.getColumns().clear();
    tablaTarjeta.getItems().clear();
    tablaTarjeta.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

    try {
      MovimientoDao movimientoDao = new MovimientoDao();
      CierreTarjetaDao cierreDao  = new CierreTarjetaDao();
      CuotaDao cuotaDao           = new CuotaDao();

      if ("Todas".equals(seleccion)) {
        // Columnas: Mes | Débito | Crédito Consumo | Crédito Pago | Crédito Deuda
        agregarColumna(tablaTarjeta, "Mes",             "col0", 80);
        agregarColumna(tablaTarjeta, "Débito",          "col1", 140);
        agregarColumna(tablaTarjeta, "Crédito Consumo", "col2", 160);
        agregarColumna(tablaTarjeta, "Crédito Pago",    "col3", 140);
        agregarColumna(tablaTarjeta, "Crédito Deuda",   "col4", 140);

        double totDeb = 0, totCons = 0, totPago = 0;

        for (Month month : Month.values()) {
          YearMonth ym = YearMonth.of(anioActual, month);
          double deb  = 0, cons = 0, pago = 0;

          for (Tarjeta t : tarjetas) {
            if ("DEBITO".equals(t.getTipo())) {
              deb += calcularEgresosMesTarjeta(ym, t, movimientoDao, cierreDao, cuotaDao);
            } else {
              double[] cp = calcularConsumoPagoCredito(ym, t, movimientoDao, cierreDao, cuotaDao);
              cons += cp[0];
              pago += cp[1];
            }
          }

          double deuda = cons - pago;
          String nombreMes = capitalizar(month.getDisplayName(TextStyle.FULL, LOCALE_ES));

          tablaTarjeta.getItems().add(new FilaGeneral(
              nombreMes,
              CURRENCY.format(deb),
              CURRENCY.format(cons),
              CURRENCY.format(pago),
              CURRENCY.format(deuda), ""));

          totDeb  += deb;
          totCons += cons;
          totPago += pago;
        }

        double totDeuda = totCons - totPago;
        lblTotTarjeta1.setText("Débito: "          + CURRENCY.format(totDeb));
        lblTotTarjeta2.setText("Crédito Consumo: " + CURRENCY.format(totCons));
        lblTotTarjeta3.setText("Crédito Pago: "    + CURRENCY.format(totPago));
        lblTotTarjeta4.setText("Crédito Deuda: "   + CURRENCY.format(totDeuda));

      } else {
        // Tarjeta específica
        Tarjeta tarjeta = tarjetas.stream()
            .filter(t -> t.getNombre().equals(seleccion))
            .findFirst().orElse(null);
        if (tarjeta == null) return;

        if ("DEBITO".equals(tarjeta.getTipo())) {
          // Solo columna de gasto
          agregarColumna(tablaTarjeta, "Mes",   "col0", 100);
          agregarColumna(tablaTarjeta, "Gasto", "col1", 200);

          double totGasto = 0;
          for (Month month : Month.values()) {
            YearMonth ym = YearMonth.of(anioActual, month);
            double gasto = calcularEgresosMesTarjeta(ym, tarjeta, movimientoDao, cierreDao, cuotaDao);
            String nombreMes = capitalizar(month.getDisplayName(TextStyle.FULL, LOCALE_ES));
            tablaTarjeta.getItems().add(new FilaGeneral(
                nombreMes, CURRENCY.format(gasto), "", "", "", ""));
            totGasto += gasto;
          }

          lblTotTarjeta1.setText("Total gasto débito: " + CURRENCY.format(totGasto));
          lblTotTarjeta2.setText("");
          lblTotTarjeta3.setText("");
          lblTotTarjeta4.setText("");

        } else {
          // Crédito: Mes | Consumo | Pago | Deuda
          agregarColumna(tablaTarjeta, "Mes",     "col0", 100);
          agregarColumna(tablaTarjeta, "Consumo", "col1", 160);
          agregarColumna(tablaTarjeta, "Pago",    "col2", 160);
          agregarColumna(tablaTarjeta, "Deuda",   "col3", 160);

          double totCons = 0, totPago = 0;
          for (Month month : Month.values()) {
            YearMonth ym = YearMonth.of(anioActual, month);
            double[] cp = calcularConsumoPagoCredito(ym, tarjeta, movimientoDao, cierreDao, cuotaDao);
            double cons = cp[0], pago = cp[1], deuda = cons - pago;
            String nombreMes = capitalizar(month.getDisplayName(TextStyle.FULL, LOCALE_ES));
            tablaTarjeta.getItems().add(new FilaGeneral(
                nombreMes,
                CURRENCY.format(cons),
                CURRENCY.format(pago),
                CURRENCY.format(deuda), "", ""));
            totCons += cons;
            totPago += pago;
          }

          lblTotTarjeta1.setText("Consumo: " + CURRENCY.format(totCons));
          lblTotTarjeta2.setText("Pago: "    + CURRENCY.format(totPago));
          lblTotTarjeta3.setText("Deuda: "   + CURRENCY.format(totCons - totPago));
          lblTotTarjeta4.setText("");
        }
      }
    } catch (Exception ex) {
      logger.error("Error al cargar tabla por tarjeta", ex);
    }
  }

  // =========================================================
  // TAB 3 — Por categoría
  // =========================================================

  private void cargarCategoria() throws Exception {
    colCategoria .setCellValueFactory(new PropertyValueFactory<>("categoria"));
    colTotalCat  .setCellValueFactory(new PropertyValueFactory<>("total"));
    colPorcentaje.setCellValueFactory(new PropertyValueFactory<>("porcentaje"));

    tablaCategoria.getItems().clear();

    // Consulta directa: suma egresos de crédito agrupados por categoría del comercio
    String sql =
        "SELECT COALESCE(co.categoria, 'Sin categoría') AS cat, " +
            "       SUM(m.monto) AS total " +
            "FROM movimiento m " +
            "JOIN comercio co ON co.id = m.comercio_id " +
            "JOIN tarjeta t ON t.id = m.tarjeta_id " +
            "WHERE m.categoria = 'EGRESO' " +
            "  AND t.tipo != 'DEBITO' " +
            "  AND EXTRACT(YEAR FROM m.fecha) = ? " +
            "GROUP BY cat " +
            "ORDER BY total DESC";

    Map<String, Double> mapa = new LinkedHashMap<>();
    double totalAnual = 0;

    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, anioActual);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        String cat = rs.getString("cat");
        double tot = rs.getDouble("total");
        mapa.put(cat, tot);
        totalAnual += tot;
      }
    }

    for (Map.Entry<String, Double> e : mapa.entrySet()) {
      double pct = totalAnual > 0 ? (e.getValue() / totalAnual) * 100 : 0;
      tablaCategoria.getItems().add(new FilaCategoria(
          e.getKey(),
          CURRENCY.format(e.getValue()),
          String.format("%.1f%%", pct)
      ));
    }

    lblTotalCategoria.setText("Total egresos crédito: " + CURRENCY.format(totalAnual));
  }

  // =========================================================
  // Helpers de cálculo
  // =========================================================

  /** Suma egresos de todas las tarjetas en un mes dado */
  private double calcularEgresosMes(YearMonth ym, List<Tarjeta> tarjetas,
                                    MovimientoDao movDao, CierreTarjetaDao cierreDao, CuotaDao cuotaDao) throws Exception {
    double total = 0;
    for (Tarjeta t : tarjetas) {
      total += calcularEgresosMesTarjeta(ym, t, movDao, cierreDao, cuotaDao);
    }
    return total;
  }

  /** Suma egresos de una tarjeta en un mes dado */
  private double calcularEgresosMesTarjeta(YearMonth ym, Tarjeta t,
                                           MovimientoDao movDao, CierreTarjetaDao cierreDao, CuotaDao cuotaDao) throws Exception {

    CierreTarjeta cierre = cierreDao.findCierrePorMesDeCierre(t.getId(), ym);
    if (cierre == null) return 0;

    CierreTarjeta anterior = cierreDao.findAnteriorPorTarjeta(t.getId(), cierre.getFechaCierre());
    LocalDate desde = (anterior != null)
        ? anterior.getFechaCierre().plusDays(1)
        : cierre.getMes();
    LocalDate hasta = cierre.getFechaCierre();

    double total = 0;
    for (Movimiento m : movDao.findByTarjetaEnRangoPeriodo(t.getId(), desde, hasta)) {
      if (!"EGRESO".equals(m.getCategoria())) continue;
      if (m.getCuotas() == 1) {
        total += m.getMonto().doubleValue();
      } else {
        int nro = cierreDao.calcularNroCuota(t.getId(), m.getFecha(), desde, hasta);
        if (nro >= 1 && nro <= m.getCuotas()) {
          for (Cuota c : cuotaDao.findByMovimiento(m.getId())) {
            if (c.getNroCuota() == nro) { total += c.getMonto().doubleValue(); break; }
          }
        }
      }
    }
    return total;
  }

  /** Retorna [consumo, pago] para una tarjeta de crédito en un mes */
  private double[] calcularConsumoPagoCredito(YearMonth ym, Tarjeta t,
                                              MovimientoDao movDao, CierreTarjetaDao cierreDao, CuotaDao cuotaDao) throws Exception {

    CierreTarjeta cierre = cierreDao.findCierrePorMesDeCierre(t.getId(), ym);
    if (cierre == null) return new double[]{0, 0};

    CierreTarjeta anterior = cierreDao.findAnteriorPorTarjeta(t.getId(), cierre.getFechaCierre());
    LocalDate desde = (anterior != null)
        ? anterior.getFechaCierre().plusDays(1)
        : cierre.getMes();
    LocalDate hasta = cierre.getFechaCierre();

    double consumo = 0, pago = 0;
    for (Movimiento m : movDao.findByTarjetaEnRangoPeriodo(t.getId(), desde, hasta)) {
      if ("PAGO".equals(m.getCategoria())) {
        pago += m.getMonto().doubleValue();
      } else if ("EGRESO".equals(m.getCategoria())) {
        if (m.getCuotas() == 1) {
          consumo += m.getMonto().doubleValue();
        } else {
          int nro = cierreDao.calcularNroCuota(t.getId(), m.getFecha(), desde, hasta);
          if (nro >= 1 && nro <= m.getCuotas()) {
            for (Cuota c : cuotaDao.findByMovimiento(m.getId())) {
              if (c.getNroCuota() == nro) { consumo += c.getMonto().doubleValue(); break; }
            }
          }
        }
      }
    }
    return new double[]{consumo, pago};
  }

  // =========================================================
  // Helpers UI
  // =========================================================

  @SuppressWarnings("unchecked")
  private void agregarColumna(TableView<FilaGeneral> tabla, String texto, String prop, double ancho) {
    TableColumn<FilaGeneral, String> col = new TableColumn<>(texto);
    col.setCellValueFactory(new PropertyValueFactory<>(prop));
    col.setPrefWidth(ancho);
    tabla.getColumns().add(col);
  }

  private String capitalizar(String s) {
    if (s == null || s.isEmpty()) return s;
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  // =========================================================
  // Modelos de fila
  // =========================================================

  /** Fila genérica de hasta 6 columnas — reutilizada en Tab 1 y Tab 2 */
  public static class FilaGeneral {
    private final String col0, col1, col2, col3, col4, col5;
    public FilaGeneral(String c0, String c1, String c2, String c3, String c4, String c5) {
      this.col0 = c0; this.col1 = c1; this.col2 = c2;
      this.col3 = c3; this.col4 = c4; this.col5 = c5;
    }
    public String getCol0() { return col0; }
    public String getCol1() { return col1; }
    public String getCol2() { return col2; }
    public String getCol3() { return col3; }
    public String getCol4() { return col4; }
    public String getCol5() { return col5; }
  }

  /** Mantenemos FilaMes por compatibilidad con las PropertyValueFactory del Tab 1 */
  public static class FilaMes extends FilaGeneral {
    public FilaMes(String mes, String ing, String egr, String bal) {
      super(mes, ing, egr, bal, "", "");
    }
    public String getMes()      { return getCol0(); }
    public String getIngresos() { return getCol1(); }
    public String getEgresos()  { return getCol2(); }
    public String getBalance()  { return getCol3(); }
  }

  /** Fila para Tab 3 — categorías */
  public static class FilaCategoria {
    private final String categoria, total, porcentaje;
    public FilaCategoria(String cat, String tot, String pct) {
      this.categoria = cat; this.total = tot; this.porcentaje = pct;
    }
    public String getCategoria()  { return categoria; }
    public String getTotal()      { return total; }
    public String getPorcentaje() { return porcentaje; }
  }
}
