package ar.com.gastos.dao;

import ar.com.gastos.model.Movimiento;
import ar.com.gastos.model.Cuota;
import ar.com.gastos.util.Db;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MovimientoDao {

  public void save(Movimiento m) throws SQLException {
    String sql = "INSERT INTO movimiento(tarjeta_id, fecha, descripcion, monto, categoria, moneda, cuotas) " +
        "VALUES (?,?,?,?,?,?,?) RETURNING id";

    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setInt(1, m.getTarjetaId());
      ps.setDate(2, Date.valueOf(m.getFecha()));
      ps.setString(3, m.getDescripcion());
      ps.setBigDecimal(4, m.getMonto());
      ps.setString(5, m.getCategoria());
      ps.setString(6, m.getMoneda());
      ps.setInt(7, m.getCuotas());

      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        int movimientoId = rs.getInt("id");

        // Generar cuotas si el movimiento tiene más de una
        if (m.getCuotas() > 1) {
          generarCuotasPublic(c, movimientoId, m);
        }
      }
    }
  }

  /**
   * Genera las cuotas de un movimiento y las inserta en la tabla cuota.
   * <p>
   * BUG CORREGIDO: antes se usaba double para dividir el monto:
   * double montoPorCuota = m.getMonto().doubleValue() / m.getCuotas();
   * <p>
   * El problema es que double es punto flotante binario y no puede representar
   * exactamente todos los decimales. Por ejemplo:
   * $100.00 / 3 con double  → 33.333333333333336  (residuo fantasma)
   * $100.00 / 3 con BigDecimal → 33.33 + 33.33 + 33.34  (correcto)
   * <p>
   * Para dinero SIEMPRE se usa BigDecimal con escala y RoundingMode explícitos.
   * <p>
   * MEJORA ADICIONAL: el residuo de centavos se suma a la última cuota,
   * garantizando que cuota1 + cuota2 + ... + cuotaN == monto total exacto.
   * Ejemplo: $100.00 / 3 → cuota1=33.33, cuota2=33.33, cuota3=33.34
   */
  public void generarCuotasPublic(Connection c, int movimientoId, Movimiento m) throws SQLException {
    String sqlCuota = "INSERT INTO cuota(movimiento_id, nro_cuota, fecha_vencimiento, monto, estado) " +
        "VALUES (?,?,?,?,?)";

    try (PreparedStatement ps = c.prepareStatement(sqlCuota)) {

      int cantidadCuotas = m.getCuotas();

      // Dividimos con HALF_UP (redondeo estándar bancario) y 2 decimales
      BigDecimal montoPorCuota = m.getMonto()
          .divide(BigDecimal.valueOf(cantidadCuotas), 2, RoundingMode.HALF_UP);

      // Calculamos el residuo: diferencia entre el total original y la suma de todas las cuotas.
      // Ejemplo: $100.00 / 3 = 33.33 * 3 = 99.99 → residuo = 0.01
      BigDecimal sumaCalculada = montoPorCuota.multiply(BigDecimal.valueOf(cantidadCuotas));
      BigDecimal residuo = m.getMonto().subtract(sumaCalculada);

      LocalDate fechaBase = m.getFecha();

      for (int i = 1; i <= cantidadCuotas; i++) {
        // El monto de la última cuota absorbe el residuo de centavos
        // para que la suma exacta de todas las cuotas sea igual al monto original.
        BigDecimal montoEstaCuota = (i == cantidadCuotas)
            ? montoPorCuota.add(residuo)
            : montoPorCuota;

        ps.setInt(1, movimientoId);
        ps.setInt(2, i);
        // Cada cuota vence un mes después de la anterior
        ps.setDate(3, Date.valueOf(fechaBase.plusMonths(i - 1)));
        ps.setBigDecimal(4, montoEstaCuota);
        ps.setString(5, "pendiente");
        ps.addBatch();
      }

      ps.executeBatch();
    }
  }

  public List<Movimiento> findByTarjeta(int tarjetaId) throws SQLException {
    List<Movimiento> list = new ArrayList<>();
    String sql = "SELECT * FROM movimiento WHERE tarjeta_id = ? ORDER BY fecha ASC";

    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, tarjetaId);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        list.add(new Movimiento(
            rs.getInt("id"),
            rs.getInt("tarjeta_id"),
            rs.getDate("fecha").toLocalDate(),
            rs.getString("descripcion"),
            rs.getBigDecimal("monto"),
            rs.getString("categoria"),
            rs.getString("moneda"),
            rs.getInt("cuotas")
        ));
      }
    }
    return list;
  }

  /**
   * Para el período dado retorna:
   * - Movimientos de cuota única cuya fecha cae en el período
   * - Movimientos en cuotas que tienen al menos una cuota con
   *   fecha_vencimiento dentro del período
   */
  public List<Movimiento> findByTarjetaEnRangoPeriodo(int tarjetaId, LocalDate desde, LocalDate hasta)
      throws SQLException {
    String sql =
        "SELECT DISTINCT m.* FROM movimiento m " +
            "WHERE m.tarjeta_id = ? AND (" +
            "  (m.cuotas = 1 AND m.fecha BETWEEN ? AND ?) " +
            "  OR " +
            "  (m.cuotas > 1 AND EXISTS (" +
            "    SELECT 1 FROM cuota c " +
            "    WHERE c.movimiento_id = m.id " +
            "    AND c.fecha_vencimiento BETWEEN ? AND ?" +
            "  ))" +
            ")";

    List<Movimiento> lista = new ArrayList<>();
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, tarjetaId);
      ps.setDate(2, Date.valueOf(desde));
      ps.setDate(3, Date.valueOf(hasta));
      ps.setDate(4, Date.valueOf(desde));
      ps.setDate(5, Date.valueOf(hasta));
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        lista.add(new Movimiento(
            rs.getInt("id"),
            rs.getInt("tarjeta_id"),
            rs.getDate("fecha").toLocalDate(),
            rs.getString("descripcion"),
            rs.getBigDecimal("monto"),
            rs.getString("categoria"),
            rs.getString("moneda"),
            rs.getInt("cuotas")
        ));
      }
    }
    return lista;
  }

// --- Eliminación con cuotas en transacción ---

  /**
   * Elimina un movimiento y todas sus cuotas asociadas en una sola transacción.
   * Las cuotas se borran primero para respetar la FK, luego el movimiento.
   */
  public void delete(int movimientoId) throws SQLException {
    String sqlCuotas = "DELETE FROM cuota WHERE movimiento_id = ?";
    String sqlMovimiento = "DELETE FROM movimiento WHERE id = ?";

    try (Connection c = Db.getDataSource().getConnection()) {
      c.setAutoCommit(false);
      try {
        // Primero borramos las cuotas
        try (PreparedStatement ps = c.prepareStatement(sqlCuotas)) {
          ps.setInt(1, movimientoId);
          ps.executeUpdate();
        }
        // Luego el movimiento
        try (PreparedStatement ps = c.prepareStatement(sqlMovimiento)) {
          ps.setInt(1, movimientoId);
          ps.executeUpdate();
        }
        c.commit();
      } catch (SQLException ex) {
        c.rollback();
        throw ex;
      } finally {
        c.setAutoCommit(true);
      }
    }
  }

// --- Edición con recálculo de cuotas ---

  /**
   * Actualiza fecha, descripcion, monto y cuotas de un movimiento.
   * Si cambiaron las cuotas o el monto, borra las cuotas viejas y genera las nuevas.
   */
  public void update(Movimiento m) throws SQLException {
    String sqlUpdate = "UPDATE movimiento SET fecha=?, descripcion=?, monto=?, cuotas=? WHERE id=?";

    try (Connection c = Db.getDataSource().getConnection()) {
      c.setAutoCommit(false);
      try {
        // Actualizamos el movimiento
        try (PreparedStatement ps = c.prepareStatement(sqlUpdate)) {
          ps.setDate(1, Date.valueOf(m.getFecha()));
          ps.setString(2, m.getDescripcion());
          ps.setBigDecimal(3, m.getMonto());
          ps.setInt(4, m.getCuotas());
          ps.setInt(5, m.getId());
          ps.executeUpdate();
        }

        // Borramos cuotas viejas y regeneramos si corresponde
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM cuota WHERE movimiento_id = ?")) {
          ps.setInt(1, m.getId());
          ps.executeUpdate();
        }

        if (m.getCuotas() > 1) {
          generarCuotasPublic(c, m.getId(), m);
        }

        c.commit();
      } catch (SQLException ex) {
        c.rollback();
        throw ex;
      } finally {
        c.setAutoCommit(true);
      }
    }
  }

}