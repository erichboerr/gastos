package ar.com.gastos.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import ar.com.gastos.model.Ingreso;
import ar.com.gastos.util.Db;

public class IngresoDao {

  public void insertarIngreso(String tipo, BigDecimal monto, LocalDate fecha) throws SQLException {
    String sql = "INSERT INTO ingreso (fecha, tipo, monto, moneda) VALUES (?, ?, ?, ?)";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setDate(1, Date.valueOf(fecha));
      ps.setString(2, tipo);
      ps.setBigDecimal(3, monto.setScale(2));
      ps.setString(4, "ARS");
      ps.executeUpdate();
    }
  }

  public List<Ingreso> listarIngresos() throws SQLException {
    List<Ingreso> ingresos = new ArrayList<>();
    String sql = "SELECT id, fecha, tipo, monto, moneda FROM ingreso ORDER BY fecha DESC";
    try (Connection conn = Db.getDataSource().getConnection();
         Statement st = conn.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
      while (rs.next()) {
        Ingreso i = new Ingreso(
            rs.getInt("id"),
            rs.getDate("fecha").toLocalDate(),
            rs.getString("tipo"),
            rs.getBigDecimal("monto"),
            rs.getString("moneda")
        );
        ingresos.add(i);
      }
    }
    return ingresos;
  }
}
