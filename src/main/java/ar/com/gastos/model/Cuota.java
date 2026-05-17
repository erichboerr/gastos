package ar.com.gastos.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Cuota {
  private int id;
  private int movimientoId;
  private int nroCuota;
  private LocalDate fechaVencimiento;
  private BigDecimal monto;
  private String estado;

  public Cuota(int id, int movimientoId, int nroCuota, LocalDate fechaVencimiento, BigDecimal monto, String estado) {
    this.id = id;
    this.movimientoId = movimientoId;
    this.nroCuota = nroCuota;
    this.fechaVencimiento = fechaVencimiento;
    this.monto = monto;
    this.estado = estado;
  }

  // Getters y setters...


  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getMovimientoId() {
    return movimientoId;
  }

  public void setMovimientoId(int movimientoId) {
    this.movimientoId = movimientoId;
  }

  public int getNroCuota() {
    return nroCuota;
  }

  public void setNroCuota(int nroCuota) {
    this.nroCuota = nroCuota;
  }

  public LocalDate getFechaVencimiento() {
    return fechaVencimiento;
  }

  public void setFechaVencimiento(LocalDate fechaVencimiento) {
    this.fechaVencimiento = fechaVencimiento;
  }

  public BigDecimal getMonto() {
    return monto;
  }

  public void setMonto(BigDecimal monto) {
    this.monto = monto;
  }

  public String getEstado() {
    return estado;
  }

  public void setEstado(String estado) {
    this.estado = estado;
  }
}
