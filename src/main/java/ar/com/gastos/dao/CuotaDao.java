package ar.com.gastos.dao;

import ar.com.gastos.model.Cuota;
import ar.com.gastos.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CuotaDao {

  /**
   * Retorna todas las cuotas de un movimiento ordenadas por nro_cuota
   */
  public List<Cuota> findByMovimiento(int movimientoId) throws SQLException {
    List<Cuota> list = new ArrayList<>();
    String sql = "SELECT * FROM cuota WHERE movimiento_id = ? ORDER BY nro_cuota";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, movimientoId);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        list.add(new Cuota(
            rs.getInt("id"),
            rs.getInt("movimiento_id"),
            rs.getInt("nro_cuota"),
            rs.getBigDecimal("monto"),
            rs.getString("estado")
        ));
      }
    }
    return list;
  }

  /**
   * Marca una cuota como pagada
   */
  public void marcarPagada(int cuotaId) throws SQLException {
    String sql = "UPDATE cuota SET estado = 'pagada' WHERE id = ?";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, cuotaId);
      ps.executeUpdate();
    }
  }
}