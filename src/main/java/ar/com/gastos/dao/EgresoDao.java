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
          new MovimientoDao().generarCuotasPublic(c, movimientoId, m);
        }
      }
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