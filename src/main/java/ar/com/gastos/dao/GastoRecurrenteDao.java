package ar.com.gastos.dao;

import ar.com.gastos.model.GastoRecurrente;
import ar.com.gastos.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla gastos_recurrentes.
 * Gestiona las plantillas de gastos que se repiten mensualmente.
 * El monto no se almacena aquí — se ingresa al generar el egreso real.
 */
public class GastoRecurrenteDao {

  // --- Consulta ---

  /**
   * Retorna todos los recurrentes ordenados alfabéticamente
   */
  public List<GastoRecurrente> findAll() throws SQLException {
    List<GastoRecurrente> lista = new ArrayList<>();
    String sql = "SELECT id, descripcion, categoria, medio_pago FROM gastos_recurrentes ORDER BY descripcion";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        GastoRecurrente g = new GastoRecurrente();
        g.setId(rs.getInt("id"));
        g.setDescripcion(rs.getString("descripcion"));
        g.setCategoria(rs.getString("categoria"));
        g.setMedioPago(rs.getString("medio_pago"));
        lista.add(g);
      }
    }
    return lista;
  }

  // --- Alta ---

  /**
   * Inserta una nueva plantilla de gasto recurrente
   */
  public void save(GastoRecurrente g) throws SQLException {
    String sql = "INSERT INTO gastos_recurrentes (descripcion, categoria, medio_pago) VALUES (?, ?, ?)";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, g.getDescripcion().toUpperCase().trim());
      ps.setString(2, g.getCategoria().toUpperCase().trim());
      ps.setString(3, g.getMedioPago());
      ps.executeUpdate();
    }
  }

  // --- Modificación ---

  /**
   * Actualiza descripcion, categoria y medio_pago de un recurrente existente
   */
  public void update(GastoRecurrente g) throws SQLException {
    String sql = "UPDATE gastos_recurrentes SET descripcion=?, categoria=?, medio_pago=? WHERE id=?";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, g.getDescripcion().toUpperCase().trim());
      ps.setString(2, g.getCategoria().toUpperCase().trim());
      ps.setString(3, g.getMedioPago());
      ps.setInt(4, g.getId());
      ps.executeUpdate();
    }
  }

  // --- Baja ---

  /**
   * Elimina una plantilla por su id
   */
  public void delete(int id) throws SQLException {
    String sql = "DELETE FROM gastos_recurrentes WHERE id=?";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, id);
      ps.executeUpdate();
    }
  }
}