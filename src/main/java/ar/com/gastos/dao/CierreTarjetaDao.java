package ar.com.gastos.dao;

import ar.com.gastos.model.CierreTarjeta;
import ar.com.gastos.util.Db;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;

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
}