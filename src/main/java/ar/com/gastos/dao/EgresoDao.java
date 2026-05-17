package ar.com.gastos.dao;

import ar.com.gastos.model.Movimiento;
import ar.com.gastos.util.Db;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EgresoDao {

  // Guardar un egreso (inserta en movimiento y genera cuotas si corresponde)
  public void save(Movimiento m) throws SQLException {
    String sql = "INSERT INTO movimiento(tarjeta_id, fecha, descripcion, monto, categoria, moneda, cuotas) " +
          "VALUES (?,?,?,?,?,?,?) RETURNING id";

    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setInt(1, m.getTarjetaId());
      ps.setDate(2, java.sql.Date.valueOf(m.getFecha()));

      // MEJORA: guardamos la descripción siempre en mayúsculas.
      // Así "nueva varela", "Nueva Varela" y "NUEVA VARELA" se tratan como
      // el mismo concepto — evita duplicados en el ComboBox de descripciones.
      ps.setString(3, m.getDescripcion().toUpperCase().trim());

      ps.setBigDecimal(4, m.getMonto().setScale(2));
      ps.setString(5, "EGRESO");
      ps.setString(6, m.getMoneda());
      ps.setInt(7, m.getCuotas());

      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        int movimientoId = rs.getInt("id");
        if (m.getCuotas() > 1) {
          generarCuotas(c, movimientoId, m);
        }
      }
    }
  }

  // Genera las cuotas del movimiento con BigDecimal y manejo de residuo de centavos
  private void generarCuotas(Connection c, int movimientoId, Movimiento m) throws SQLException {
    String sqlCuota = "INSERT INTO cuota(movimiento_id, nro_cuota, fecha_vencimiento, monto, estado) " +
          "VALUES (?,?,?,?,?)";

    try (PreparedStatement ps = c.prepareStatement(sqlCuota)) {
      int cantidadCuotas = m.getCuotas();

      BigDecimal montoPorCuota = m.getMonto()
            .divide(BigDecimal.valueOf(cantidadCuotas), 2, RoundingMode.HALF_UP);

      // Residuo: diferencia entre el total y la suma de todas las cuotas.
      // Se suma a la última cuota para que el total sea exacto.
      BigDecimal residuo = m.getMonto()
            .subtract(montoPorCuota.multiply(BigDecimal.valueOf(cantidadCuotas)));

      LocalDate fechaBase = m.getFecha();

      for (int i = 1; i <= cantidadCuotas; i++) {
        BigDecimal montoEstaCuota = (i == cantidadCuotas)
              ? montoPorCuota.add(residuo)
              : montoPorCuota;

        ps.setInt(1, movimientoId);
        ps.setInt(2, i);
        ps.setDate(3, java.sql.Date.valueOf(fechaBase.plusMonths(i - 1)));
        ps.setBigDecimal(4, montoEstaCuota);
        ps.setString(5, "pendiente");
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  // Devuelve todas las descripciones distintas guardadas en movimientos de tipo EGRESO.
  // El EgresoController las usa para cargar el ComboBox de descripción.
  public List<String> findDescripcionesDistintas() throws SQLException {
    List<String> lista = new ArrayList<>();
    String sql = "SELECT DISTINCT descripcion FROM movimiento " +
          "WHERE categoria = 'EGRESO' ORDER BY descripcion ASC";

    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        lista.add(rs.getString("descripcion"));
      }
    }
    return lista;
  }

  // Listar todos los egresos de una tarjeta
  public List<Movimiento> findByTarjeta(int tarjetaId) throws SQLException {
    List<Movimiento> list = new ArrayList<>();
    String sql = "SELECT * FROM movimiento WHERE tarjeta_id = ? AND categoria = 'EGRESO' ORDER BY fecha DESC";

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
}