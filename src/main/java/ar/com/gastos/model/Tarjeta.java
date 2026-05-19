package ar.com.gastos.model;

public class Tarjeta {

  // --- Campos persistidos en la base de datos ---
  private int id;
  private String nombre;
  private String tipo;
  private boolean habilitado;

  // --- Campos calculados, solo se usan en el Dashboard ---
  // No vienen de la base, se calculan en DashboardController y se setean después.
  // En una app más grande estos irían en un objeto separado (ej: TarjetaResumen),
  // pero para este proyecto está bien tenerlos acá.
  private double totalGastado;
  private double totalPagado;
  private double restaAbonar;

  /**
   * Constructor principal — usado al leer tarjetas desde la base de datos.
   */
  public Tarjeta(int id, String nombre, String tipo, boolean habilitado) {
    this.id = id;
    this.nombre = nombre;
    this.tipo = tipo;
    this.habilitado = habilitado;
    // Los campos del dashboard arrancan en 0 hasta que el DashboardController los calcule
    this.totalGastado = 0.0;
    this.totalPagado = 0.0;
    this.restaAbonar = 0.0;
  }

  /**
   * Constructor auxiliar — usado cuando se construye una Tarjeta ya con sus totales calculados.
   * <p>
   * BUG CORREGIDO: el constructor anterior recibía totalPagado y restaAbonar como parámetros
   * pero nunca los asignaba a los campos del objeto. Quedaban en 0.0 silenciosamente.
   */
  public Tarjeta(String nombre, String tipo, double totalGastado, double totalPagado, double restaAbonar) {
    this.nombre = nombre;
    this.tipo = tipo;
    this.totalGastado = totalGastado;
    this.totalPagado = totalPagado;      // ← esto faltaba
    this.restaAbonar = restaAbonar;      // ← esto faltaba
  }


  // --- Getters y setters ---

  public int getId() {
    return id;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getNombre() {
    return nombre;
  }

  public void setTipo(String tipo) {
    this.tipo = tipo;
  }

  public String getTipo() {
    return tipo;
  }

  public void setHabilitado(boolean habilitado) {
    this.habilitado = habilitado;
  }

  public boolean getHabilitado() {
    return habilitado;
  }

  public double getTotalGastado() {
    return totalGastado;
  }

  public void setTotalGastado(double totalGastado) {
    this.totalGastado = totalGastado;
  }

  public double getTotalPagado() {
    return totalPagado;
  }

  public void setTotalPagado(double totalPagado) {
    this.totalPagado = totalPagado;
  }

  public void setRestaAbonar(double restaAbonar) {
    this.restaAbonar = restaAbonar;
  }

  public double getRestaAbonar() {
    return restaAbonar;
  }
}