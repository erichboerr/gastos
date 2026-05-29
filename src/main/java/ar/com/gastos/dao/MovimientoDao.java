package ar.com.gastos.dao;

import ar.com.gastos.model.Comercio;
import ar.com.gastos.model.Cuota;
import ar.com.gastos.model.Movimiento;
import ar.com.gastos.util.Db;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MovimientoDao {

  // --- Alta ---

  /**
   * Inserta un movimiento en la DB.
   * Si tiene cuotas > 1 genera las filas en la tabla cuota en la misma transacción.
   */
  public void save(Movimiento m) throws SQLException {
    String sql = "INSERT INTO movimiento(tarjeta_id, comercio_id, fecha, descripcion, monto, categoria, moneda, cuotas) " +
        "VALUES (?,?,?,?,?,?,?,?) RETURNING id";

    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setInt(1, m.getTarjetaId());

      // comercio_id es NULL para pagos
      if (m.getComercioId() > 0) {
        ps.setInt(2, m.getComercioId());
      } else {
        ps.setNull(2, Types.INTEGER);
      }

      ps.setDate(3, Date.valueOf(m.getFecha()));
      ps.setString(4, m.getDescripcion() != null
          ? m.getDescripcion().toUpperCase().trim() : null);
      ps.setBigDecimal(5, m.getMonto().setScale(2));
      ps.setString(6, m.getCategoria());
      ps.setString(7, m.getMoneda());
      ps.setInt(8, m.getCuotas());

      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        int movimientoId = rs.getInt("id");
        if (m.getCuotas() > 1) {
          generarCuotasPublic(c, movimientoId, m);
        }
      }
    }
  }

  // --- Generación de cuotas ---

  /**
   * Genera las cuotas de un movimiento y las inserta en la tabla cuota.
   *
   * BUG CORREGIDO: antes se usaba double para dividir el monto.
   * Para dinero SIEMPRE se usa BigDecimal con escala y RoundingMode explícitos.
   *
   * MEJORA: el residuo de centavos se suma a la última cuota,
   * garantizando que cuota1 + ... + cuotaN == monto total exacto.
   * Ejemplo: $100.00 / 3 → cuota1=33.33, cuota2=33.33, cuota3=33.34
   */
  public void generarCuotasPublic(Connection c, int movimientoId, Movimiento m) throws SQLException {
    String sql = "INSERT INTO cuota(movimiento_id, nro_cuota, fecha_vencimiento, monto, estado) " +
        "VALUES (?,?,?,?,?)";

    try (PreparedStatement ps = c.prepareStatement(sql)) {
      int cantidad = m.getCuotas();

      // Dividimos con HALF_UP (redondeo estándar bancario) y 2 decimales
      BigDecimal montoPorCuota = m.getMonto()
          .divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);

      // Residuo: diferencia entre el total original y la suma de cuotas redondeadas
      BigDecimal residuo = m.getMonto()
          .subtract(montoPorCuota.multiply(BigDecimal.valueOf(cantidad)));

      LocalDate fechaBase = m.getFecha();

      for (int i = 1; i <= cantidad; i++) {
        // La última cuota absorbe el residuo de centavos
        BigDecimal montoEstaCuota = (i == cantidad)
            ? montoPorCuota.add(residuo)
            : montoPorCuota;

        // Cuota 1: fecha de compra. Siguientes: día 1 del mes correspondiente.
        // Garantiza que ninguna cuota se salte un período por caer después del cierre.
        LocalDate fechaCuota = (i == 1)
            ? fechaBase
            // ✅ Ahora — día 1 del mes correspondiente
            : fechaBase.plusMonths(i - 1).withDayOfMonth(1).plusMonths(1);

        ps.setInt(1, movimientoId);
        ps.setInt(2, i);
        ps.setDate(3, Date.valueOf(fechaCuota));
        ps.setBigDecimal(4, montoEstaCuota);
        ps.setString(5, "pendiente");
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  // --- Consultas ---

  /** Retorna todos los movimientos de una tarjeta ordenados por fecha ASC */
  public List<Movimiento> findByTarjeta(int tarjetaId) throws SQLException {
    List<Movimiento> lista = new ArrayList<>();
    String sql = "SELECT m.*, COALESCE(co.nombre, m.descripcion) AS label " +
        "FROM movimiento m " +
        "LEFT JOIN comercio co ON co.id = m.comercio_id " +
        "WHERE m.tarjeta_id = ? ORDER BY m.fecha ASC";

    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, tarjetaId);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        Movimiento mov = mapRow(rs);
        // Usamos el nombre del comercio como descripción para mostrar en la UI
        mov.setDescripcion(rs.getString("label"));
        lista.add(mov);
      }
    }
    return lista;
  }

  /**
   * Para el período dado retorna:
   * - Movimientos de cuota única cuya fecha cae en el período
   * - Movimientos en cuotas que tienen al menos una cuota con
   *   fecha_vencimiento dentro del período
   * Incluye el nombre del comercio como descripción via JOIN.
   */
  public List<Movimiento> findByTarjetaEnRangoPeriodo(int tarjetaId, LocalDate desde, LocalDate hasta)
      throws SQLException {
    String sql =
        "SELECT DISTINCT m.*, COALESCE(co.nombre, m.descripcion) AS label " +
            "FROM movimiento m " +
            "LEFT JOIN comercio co ON co.id = m.comercio_id " +
            "WHERE m.tarjeta_id = ? AND (" +
            "  (m.cuotas = 1 AND m.fecha BETWEEN ? AND ?) " +
            "  OR " +
            "  (m.cuotas > 1 AND EXISTS (" +
            "    SELECT 1 FROM cuota cu " +
            "    WHERE cu.movimiento_id = m.id " +
            "    AND cu.fecha_vencimiento BETWEEN ? AND ?" +
            "  ))" +
            ") ORDER BY m.fecha ASC";

    List<Movimiento> lista = new ArrayList<>();
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, tarjetaId);
      ps.setDate(2, Date.valueOf(desde));
      ps.setDate(3, Date.valueOf(hasta));
      ps.setDate(4, Date.valueOf(desde));
      ps.setDate(5, Date.valueOf(hasta));
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        Movimiento mov = mapRow(rs);
        mov.setDescripcion(rs.getString("label"));
        lista.add(mov);
      }
    }
    return lista;
  }

  /** Verifica si ya existen movimientos con descripción libre para una tarjeta en una fecha dada */
  public boolean existeRecurrenteEnMes(int tarjetaId, LocalDate fecha) throws SQLException {
    String sql = "SELECT COUNT(*) FROM movimiento " +
        "WHERE tarjeta_id = ? AND fecha = ? AND categoria = 'EGRESO' AND comercio_id IS NULL";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, tarjetaId);
      ps.setDate(2, Date.valueOf(fecha));
      ResultSet rs = ps.executeQuery();
      if (rs.next()) return rs.getInt(1) > 0;
    }
    return false;
  }

  // --- Eliminación con cuotas en transacción ---

  /**
   * Elimina un movimiento y todas sus cuotas en una sola transacción.
   * Las cuotas se borran primero para respetar la FK.
   */
  public void delete(int movimientoId) throws SQLException {
    try (Connection c = Db.getDataSource().getConnection()) {
      c.setAutoCommit(false);
      try {
        try (PreparedStatement ps = c.prepareStatement(
            "DELETE FROM cuota WHERE movimiento_id = ?")) {
          ps.setInt(1, movimientoId);
          ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
            "DELETE FROM movimiento WHERE id = ?")) {
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
   * Actualiza fecha, comercio_id, descripcion, monto y cuotas de un movimiento.
   * Borra las cuotas viejas y regenera si corresponde.
   */
  public void update(Movimiento m) throws SQLException {
    String sql = "UPDATE movimiento SET fecha=?, comercio_id=?, descripcion=?, monto=?, cuotas=? WHERE id=?";

    try (Connection c = Db.getDataSource().getConnection()) {
      c.setAutoCommit(false);
      try {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
          ps.setDate(1, Date.valueOf(m.getFecha()));
          if (m.getComercioId() > 0) {
            ps.setInt(2, m.getComercioId());
          } else {
            ps.setNull(2, Types.INTEGER);
          }
          ps.setString(3, m.getDescripcion() != null
              ? m.getDescripcion().toUpperCase().trim() : null);
          ps.setBigDecimal(4, m.getMonto());
          ps.setInt(5, m.getCuotas());
          ps.setInt(6, m.getId());
          ps.executeUpdate();
        }

        // Borramos cuotas viejas y regeneramos
        try (PreparedStatement ps = c.prepareStatement(
            "DELETE FROM cuota WHERE movimiento_id = ?")) {
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

  // --- Helper ---

  private Movimiento mapRow(ResultSet rs) throws SQLException {
    return new Movimiento(
        rs.getInt("id"),
        rs.getInt("tarjeta_id"),
        rs.getInt("comercio_id"),
        rs.getDate("fecha").toLocalDate(),
        rs.getString("descripcion"),
        rs.getBigDecimal("monto"),
        rs.getString("categoria"),
        rs.getString("moneda"),
        rs.getInt("cuotas")
    );
  }
}