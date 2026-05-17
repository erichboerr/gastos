package ar.com.gastos.dao;

import ar.com.gastos.model.Movimiento;
import ar.com.gastos.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PagoDao {

  // Guardar un pago (inserta en movimiento)
  public void save(Movimiento m) throws SQLException {
    String sql = "INSERT INTO movimiento(tarjeta_id, fecha, descripcion, monto, categoria, moneda, cuotas) " +
        "VALUES (?,?,?,?,?,?,?)";

    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setInt(1, m.getTarjetaId());
      ps.setDate(2, java.sql.Date.valueOf(m.getFecha()));
      ps.setString(3, m.getDescripcion());
      ps.setBigDecimal(4, m.getMonto().setScale(2));
      ps.setString(5, "PAGO"); // 🔹 categoría fija
      ps.setString(6, m.getMoneda());
      ps.setInt(7, 1); // ingresos siempre son pago único

      ps.executeUpdate();
    }
  }

  // Listar todos los ingresos de una tarjeta
  public List<Movimiento> findByTarjeta(int tarjetaId) throws SQLException {
    List<Movimiento> list = new ArrayList<>();
    String sql = "SELECT * FROM movimiento WHERE tarjeta_id = ? AND categoria = 'INGRESO' ORDER BY fecha DESC";

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
