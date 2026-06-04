package ar.com.gastos.model;

/**
 * Modelo que representa una plantilla de gasto recurrente.
 * No tiene monto fijo — se ingresa al momento de generar el egreso del mes.
 */
public class GastoRecurrente {

  private int id;
  private String descripcion;  // Ej: "ABSA", "Combustible", "Celular"
  private String categoria;    // Ej: "Servicios", "Transporte"
  private String medioPago;    // "DEBITO", "CREDITO" o "EFECTIVO"

  // --- Constructores ---

  public GastoRecurrente() {
  }

  public GastoRecurrente(String descripcion, String categoria, String medioPago) {
    this.descripcion = descripcion;
    this.categoria = categoria;
    this.medioPago = medioPago;
  }

  // --- Getters y Setters ---

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public String getCategoria() {
    return categoria;
  }

  public void setCategoria(String categoria) {
    this.categoria = categoria;
  }

  public String getMedioPago() {
    return medioPago;
  }

  public void setMedioPago(String medioPago) {
    this.medioPago = medioPago;
  }

  // Usado por ComboBox y ListView para mostrar el nombre directamente
  @Override
  public String toString() {
    return descripcion;
  }
}