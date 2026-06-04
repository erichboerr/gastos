package ar.com.gastos.dao;

import ar.com.gastos.model.Comercio;
import ar.com.gastos.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla comercio.
 * Gestiona el catálogo centralizado de comercios compartido por todas las tarjetas.
 */
public class ComercioDao {

  // --- Consultas ---

  /**
   * Retorna todos los comercios habilitados ordenados alfabéticamente
   */
  public List<Comercio> findAllActivos() throws SQLException {
    List<Comercio> lista = new ArrayList<>();
    String sql = "SELECT id, nombre, categoria, habilitado FROM comercio " +
        "WHERE habilitado = TRUE ORDER BY nombre ASC";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        lista.add(mapRow(rs));
      }
    }
    return lista;
  }

  /**
   * Retorna todos los comercios incluyendo los dados de baja
   */
  public List<Comercio> findAll() throws SQLException {
    List<Comercio> lista = new ArrayList<>();
    String sql = "SELECT id, nombre, categoria, habilitado FROM comercio ORDER BY nombre ASC";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        lista.add(mapRow(rs));
      }
    }
    return lista;
  }

  /**
   * Busca un comercio por nombre exacto sin importar si está habilitado
   */
  public Comercio findByNombre(String nombre) throws SQLException {
    String sql = "SELECT id, nombre, categoria, habilitado FROM comercio WHERE UPPER(nombre) = ?";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, nombre.toUpperCase().trim());
      ResultSet rs = ps.executeQuery();
      if (rs.next()) return mapRow(rs);
    }
    return null;
  }

  /**
   * Retorna las categorías distintas ya registradas
   */
  public List<String> findCategoriasDistintas() throws SQLException {
    List<String> lista = new ArrayList<>();
    String sql = "SELECT DISTINCT categoria FROM comercio " +
        "WHERE categoria IS NOT NULL ORDER BY categoria ASC";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        lista.add(rs.getString("categoria"));
      }
    }
    return lista;
  }

  // --- Alta ---

  /**
   * Inserta un nuevo comercio — nombre normalizado a mayúsculas
   */
  public void save(Comercio comercio) throws SQLException {
    String sql = "INSERT INTO comercio(nombre, categoria, habilitado) VALUES(?, ?, ?)";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, comercio.getNombre().toUpperCase().trim());
      ps.setString(2, comercio.getCategoria() != null
          ? comercio.getCategoria().toUpperCase().trim() : null);
      ps.setBoolean(3, comercio.isHabilitado());
      ps.executeUpdate();
    }
  }

  // --- Modificación ---

  /**
   * Actualiza nombre y categoría de un comercio existente
   */
  public void update(Comercio comercio) throws SQLException {
    String sql = "UPDATE comercio SET nombre=?, categoria=? WHERE id=?";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, comercio.getNombre().toUpperCase().trim());
      ps.setString(2, comercio.getCategoria() != null
          ? comercio.getCategoria().toUpperCase().trim() : null);
      ps.setInt(3, comercio.getId());
      ps.executeUpdate();
    }
  }

  // --- Baja soft ---

  /**
   * Deshabilita un comercio sin eliminarlo — los movimientos históricos quedan intactos
   */
  public void darDeBaja(int id) throws SQLException {
    String sql = "UPDATE comercio SET habilitado = FALSE WHERE id = ?";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, id);
      ps.executeUpdate();
    }
  }

  /**
   * Reactiva un comercio dado de baja
   */
  public void reactivar(int id) throws SQLException {
    String sql = "UPDATE comercio SET habilitado = TRUE WHERE id = ?";
    try (Connection c = Db.getDataSource().getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, id);
      ps.executeUpdate();
    }
  }

  // --- Helper ---

  private Comercio mapRow(ResultSet rs) throws SQLException {
    return new Comercio(
        rs.getInt("id"),
        rs.getString("nombre"),
        rs.getString("categoria"),
        rs.getBoolean("habilitado")
    );
  }
}