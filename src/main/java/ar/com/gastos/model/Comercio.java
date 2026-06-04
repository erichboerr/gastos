package ar.com.gastos.model;

/**
 * Representa un comercio del catálogo centralizado.
 * Reemplaza la descripción libre en movimientos de tipo EGRESO.
 * Los pagos no usan comercio — tienen descripción libre.
 */
public class Comercio {

  private int id;
  private String nombre;
  private String categoria;
  private boolean habilitado;

  // --- Constructores ---

  /**
   * Constructor para alta — sin id todavía
   */
  public Comercio(String nombre, String categoria) {
    this.nombre = nombre;
    this.categoria = categoria;
    this.habilitado = true;
  }

  /**
   * Constructor completo — usado al leer desde la DB
   */
  public Comercio(int id, String nombre, String categoria, boolean habilitado) {
    this.id = id;
    this.nombre = nombre;
    this.categoria = categoria;
    this.habilitado = habilitado;
  }

  // --- Getters y Setters ---

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getCategoria() {
    return categoria;
  }

  public void setCategoria(String categoria) {
    this.categoria = categoria;
  }

  public boolean isHabilitado() {
    return habilitado;
  }

  public void setHabilitado(boolean habilitado) {
    this.habilitado = habilitado;
  }

  // Usado por ComboBox para mostrar el nombre directamente
  @Override
  public String toString() {
    return nombre;
  }
}