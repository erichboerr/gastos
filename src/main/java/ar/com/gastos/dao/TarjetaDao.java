package ar.com.gastos.dao;

import ar.com.gastos.model.Tarjeta;
import ar.com.gastos.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TarjetaDao {

  public Tarjeta findByNombre(String nombre) throws SQLException {
    String sql = "SELECT id, nombre, tipo, habilitado FROM tarjeta WHERE nombre = ?";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, nombre);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return new Tarjeta(
            rs.getInt("id"),
            rs.getString("nombre"),
            rs.getString("tipo"),
            rs.getBoolean("habilitado")
        );
      }
    }
    return null;
  }

  // Guarda una nueva tarjeta.
  // El nombre se normaliza a mayúsculas para consistencia,
  // igual que las descripciones de egresos.
  public void save(Tarjeta t) throws SQLException {
    String sql = "INSERT INTO tarjeta(nombre, tipo, habilitado) VALUES(?,?,?)";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, t.getNombre().toUpperCase().trim());
      ps.setString(2, t.getTipo().toUpperCase().trim());
      ps.setBoolean(3, t.isHabilitado());
      ps.executeUpdate();
    }
  }

  public List<Tarjeta> findAllActivas() throws SQLException {
    List<Tarjeta> list = new ArrayList<>();
    String sql = "SELECT id, nombre, tipo, habilitado FROM tarjeta WHERE habilitado = TRUE";

    try (Connection c = Db.getDataSource().getConnection();
         Statement st = c.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
      while (rs.next()) {
        list.add(new Tarjeta(
            rs.getInt("id"),
            rs.getString("nombre"),
            rs.getString("tipo"),
            rs.getBoolean("habilitado")
        ));
      }
    }
    return list;
  }

  // Devuelve los tipos de tarjeta distintos ya registrados en la base.
  // El ComboBox de tipo en NuevaTarjetaController los usa para no tener
  // que escribir siempre lo mismo — mismo patrón que descripciones de egresos.
  public List<String> findTiposDistintos() throws SQLException {
    List<String> tipos = new ArrayList<>();
    String sql = "SELECT DISTINCT tipo FROM tarjeta WHERE tipo IS NOT NULL ORDER BY tipo ASC";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        tipos.add(rs.getString("tipo"));
      }
    }
    return tipos;
  }

  /** Retorna la primera tarjeta activa que coincide con el tipo dado */
  public Tarjeta findByTipo(String tipo) throws SQLException {
    String sql = "SELECT id, nombre, tipo, habilitado FROM tarjeta WHERE tipo = ? AND habilitado = TRUE LIMIT 1";
    try (Connection conn = Db.getDataSource().getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, tipo);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new Tarjeta(
              rs.getInt("id"),
              rs.getString("nombre"),
              rs.getString("tipo"),
              rs.getBoolean("habilitado")
          );
        }
      }
    }
    return null;
  }

  public void darDeBaja(int id) throws SQLException {
    String sql = "UPDATE tarjeta SET habilitado = FALSE WHERE id = ?";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, id);
      ps.executeUpdate();
    }
  }
}
