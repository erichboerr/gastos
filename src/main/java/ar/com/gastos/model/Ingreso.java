package ar.com.gastos.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Ingreso {
  private int id;
  private LocalDate fecha;
  private String tipo;
  private BigDecimal monto;
  private String moneda;

  public Ingreso(int id, LocalDate fecha, String tipo, BigDecimal monto, String moneda) {
    this.id = id;
    this.fecha = fecha;
    this.tipo = tipo;
    this.monto = monto;
    this.moneda = moneda;
  }

  // getters y setters...
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public void setFecha(LocalDate fecha) {
    this.fecha = fecha;
  }

  public String getTipo() {
    return tipo;
  }

  public void setTipo(String tipo) {
    this.tipo = tipo;
  }

  public BigDecimal getMonto() {
    return monto;
  }

  public void setMonto(BigDecimal monto) {
    this.monto = monto;
  }

  public String getMoneda() {
    return moneda;
  }

  public void setMoneda(String moneda) {
    this.moneda = moneda;
  }
}
