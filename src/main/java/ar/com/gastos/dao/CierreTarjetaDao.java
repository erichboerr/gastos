package ar.com.gastos.dao;

import ar.com.gastos.model.CierreTarjeta;
import ar.com.gastos.util.Db;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class CierreTarjetaDao {

  // Guarda un nuevo cierre de tarjeta para el mes indicado.
  // La columna "id" en cierres_tarjeta es la FK a tarjeta(id).
  public void save(CierreTarjeta cierre) throws SQLException {
    String sql = "INSERT INTO cierres_tarjeta (id, mes, fecha_cierre, fecha_vencimiento) VALUES (?, ?, ?, ?)";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, cierre.getTarjetaId());
      ps.setDate(2, Date.valueOf(cierre.getMes()));
      ps.setDate(3, Date.valueOf(cierre.getFechaCierre()));
      ps.setDate(4, Date.valueOf(cierre.getFechaVencimiento()));
      ps.executeUpdate();
    }
  }

  // Devuelve el cierre más reciente de una tarjeta (el último registrado).
  public CierreTarjeta findUltimoPorTarjeta(int tarjetaId) throws SQLException {
    String sql = "SELECT * FROM cierres_tarjeta WHERE id = ? ORDER BY fecha_cierre DESC LIMIT 1";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, tarjetaId);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) return mapRow(rs);
    }
    return null;
  }

  // Devuelve el próximo cierre después de una fecha dada.
  // Útil para navegar hacia adelante en el historial de cierres.
  public CierreTarjeta findProximoPorTarjeta(int tarjetaId, LocalDate fechaCierreActual) throws SQLException {
    String sql = "SELECT * FROM cierres_tarjeta WHERE id = ? AND fecha_cierre > ? ORDER BY fecha_cierre ASC LIMIT 1";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, tarjetaId);
      ps.setDate(2, Date.valueOf(fechaCierreActual));
      ResultSet rs = ps.executeQuery();
      if (rs.next()) return mapRow(rs);
    }
    return null;
  }

  // Devuelve el cierre anterior a una fecha dada.
  // El DashboardController lo usa para calcular el inicio del período:
  // desde = fechaCierreAnterior + 1 día hasta = fechaCierreActual
  public CierreTarjeta findAnteriorPorTarjeta(int tarjetaId, LocalDate fechaCierreActual) throws SQLException {
    String sql = "SELECT * FROM cierres_tarjeta WHERE id = ? AND fecha_cierre < ? ORDER BY fecha_cierre DESC LIMIT 1";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, tarjetaId);
      ps.setDate(2, Date.valueOf(fechaCierreActual));
      ResultSet rs = ps.executeQuery();
      if (rs.next()) return mapRow(rs);
    }
    return null;
  }

  // Devuelve el cierre correspondiente a un mes específico.
  // El DashboardController lo llama con YearMonth.now() para mostrar el mes actual.
  public CierreTarjeta findCierrePorMes(int tarjetaId, YearMonth mes) throws SQLException {
    String sql = "SELECT * FROM cierres_tarjeta WHERE id = ? AND mes = ? LIMIT 1";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, tarjetaId);
      // Guardamos el mes como el primer día del mes: 2026-05-01
      ps.setDate(2, Date.valueOf(mes.atDay(1)));
      ResultSet rs = ps.executeQuery();
      if (rs.next()) return mapRow(rs);
    }
    return null;
  }

  // Convierte una fila del ResultSet en un objeto CierreTarjeta.
  // Centralizado acá para no repetir la misma lógica en cada método.
  private CierreTarjeta mapRow(ResultSet rs) throws SQLException {
    return new CierreTarjeta(
        rs.getInt("id_cierre"),
        rs.getInt("id"),
        rs.getDate("mes").toLocalDate(),
        rs.getDate("fecha_cierre").toLocalDate(),
        rs.getDate("fecha_vencimiento").toLocalDate()
    );
  }

  /**
   * Retorna todos los cierres de una tarjeta ordenados por mes DESC
   */
  public List<CierreTarjeta> findByTarjeta(int tarjetaId) throws SQLException {
    List<CierreTarjeta> lista = new ArrayList<>();
    String sql = "SELECT * FROM cierres_tarjeta WHERE id = ? ORDER BY mes DESC";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, tarjetaId);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) lista.add(mapRow(rs));
    }
    return lista;
  }

  /**
   * Actualiza fecha de cierre y vencimiento de un cierre existente
   */
  public void update(CierreTarjeta c) throws SQLException {
    String sql = "UPDATE cierres_tarjeta SET mes=?, fecha_cierre=?, fecha_vencimiento=? WHERE id_cierre=?";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setDate(1, Date.valueOf(c.getMes()));
      ps.setDate(2, Date.valueOf(c.getFechaCierre()));
      ps.setDate(3, Date.valueOf(c.getFechaVencimiento()));
      ps.setInt(4, c.getIdCierre());
      ps.executeUpdate();
    }
  }

  /**
   * Elimina un cierre por su id_cierre
   */
  public void delete(int idCierre) throws SQLException {
    String sql = "DELETE FROM cierres_tarjeta WHERE id_cierre=?";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, idCierre);
      ps.executeUpdate();
    }
  }

  /**
   * Retorna el cierre cuya fecha_vencimiento cae en el mes dado.
   * Usado por el dashboard para mostrar en el mes X lo que se abona en X.
   */
  public CierreTarjeta findCierrePorVencimiento(int tarjetaId, YearMonth mes) throws SQLException {
    // Buscamos cierres cuya fecha_vencimiento esté dentro del mes dado
    String sql = "SELECT * FROM cierres_tarjeta " +
        "WHERE id = ? " +
        "AND EXTRACT(YEAR FROM fecha_vencimiento) = ? " +
        "AND EXTRACT(MONTH FROM fecha_vencimiento) = ? " +
        "LIMIT 1";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, tarjetaId);
      ps.setInt(2, mes.getYear());
      ps.setInt(3, mes.getMonthValue());
      ResultSet rs = ps.executeQuery();
      if (rs.next()) return mapRow(rs);
    }
    return null;
  }

  /**
   * Retorna el cierre cuya fecha_cierre cae en el mes dado
   */
  public CierreTarjeta findCierrePorMesDeCierre(int tarjetaId, YearMonth mes) throws SQLException {
    String sql = "SELECT * FROM cierres_tarjeta " +
        "WHERE id = ? " +
        "AND EXTRACT(YEAR FROM fecha_cierre) = ? " +
        "AND EXTRACT(MONTH FROM fecha_cierre) = ? " +
        "LIMIT 1";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      System.out.println(tarjetaId);
      System.out.println(mes.getYear());
      System.out.println(mes.getMonthValue());
      ps.setInt(1, tarjetaId);
      ps.setInt(2, mes.getYear());
      ps.setInt(3, mes.getMonthValue());
      ResultSet rs = ps.executeQuery();
      if (rs.next()) return mapRow(rs);
    }
    return null;
  }

  /**
   * Retorna todos los cierres de una tarjeta ordenados por fecha_cierre ASC.
   * Usado para calcular en qué número de cuota está un movimiento en un período dado.
   */
  public List<CierreTarjeta> findAllPorTarjeta(int tarjetaId) throws SQLException {
    List<CierreTarjeta> lista = new ArrayList<>();
    String sql = "SELECT * FROM cierres_tarjeta WHERE id = ? ORDER BY fecha_cierre ASC";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, tarjetaId);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) lista.add(mapRow(rs));
    }
    return lista;
  }

  /**
   * Dado un movimiento con fecha de compra y un período (desde-hasta),
   * calcula qué número de cuota corresponde a ese período.
   * <p>
   * Lógica: la cuota 1 está en el período donde cae la fecha de compra.
   * Cada período siguiente es la cuota siguiente.
   * Retorna -1 si la fecha de compra no cae en ningún período conocido.
   */
  public int calcularNroCuota(int tarjetaId, LocalDate fechaCompra, LocalDate desde, LocalDate hasta)
      throws SQLException {
    List<CierreTarjeta> cierres = findAllPorTarjeta(tarjetaId);

    // Encontramos el índice del período donde cae la fecha de compra (cuota 1)
    int indiceCuota1 = -1;
    int indicePeriodoActual = -1;

    for (int i = 0; i < cierres.size(); i++) {
      CierreTarjeta cierre = cierres.get(i);
      CierreTarjeta anterior = i > 0 ? cierres.get(i - 1) : null;

      LocalDate periodoDesde = (anterior != null)
          ? anterior.getFechaCierre().plusDays(1)
          : cierre.getMes();
      LocalDate periodoHasta = cierre.getFechaCierre();

      // ¿La fecha de compra cae en este período? → cuota 1
      if (!fechaCompra.isBefore(periodoDesde) && !fechaCompra.isAfter(periodoHasta)) {
        indiceCuota1 = i;
      }

      // ¿El período actual (desde-hasta) es este?
      if (periodoDesde.equals(desde) && periodoHasta.equals(hasta)) {
        indicePeriodoActual = i;
      }
    }

    if (indiceCuota1 == -1 || indicePeriodoActual == -1) return -1;

    // El número de cuota es la diferencia de índices + 1
    return indicePeriodoActual - indiceCuota1 + 1;
  }
}