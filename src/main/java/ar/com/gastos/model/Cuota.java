package ar.com.gastos.model;

import java.math.BigDecimal;

public class Cuota {

  private int id;
  private int movimientoId;
  private int nroCuota;
  private BigDecimal monto;
  private String estado;

  public Cuota(int id, int movimientoId, int nroCuota, BigDecimal monto, String estado) {
    this.id = id;
    this.movimientoId = movimientoId;
    this.nroCuota = nroCuota;
    this.monto = monto;
    this.estado = estado;
  }

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