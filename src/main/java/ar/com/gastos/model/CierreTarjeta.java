package ar.com.gastos.model;

import java.time.LocalDate;

public class CierreTarjeta {

  private int idCierre;
  private int tarjetaId;
  private LocalDate mes;
  private LocalDate fechaCierre;
  private LocalDate fechaVencimiento;

  // constructor
  public CierreTarjeta(int tarjetaId, LocalDate mes,
                       LocalDate fechaCierre, LocalDate fechaVencimiento) {

    this.tarjetaId = tarjetaId;
    this.mes = mes;
    this.fechaCierre = fechaCierre;
    this.fechaVencimiento = fechaVencimiento;
  }

  public CierreTarjeta(int idCierre, int tarjetaId, LocalDate mes,
                       LocalDate fechaCierre, LocalDate fechaVencimiento) {
    this.idCierre = idCierre;
    this.tarjetaId = tarjetaId;
    this.mes = mes;
    this.fechaCierre = fechaCierre;
    this.fechaVencimiento = fechaVencimiento;
  }


  // getters y setters


  public int getIdCierre() {
    return idCierre;
  }

  public void setIdCierre(int idCierre) {
    this.idCierre = idCierre;
  }

  public int getTarjetaId() {
    return tarjetaId;
  }

  public void setTarjetaId(int tarjetaId) {
    this.tarjetaId = tarjetaId;
  }

  public LocalDate getMes() {
    return mes;
  }

  public void setMes(LocalDate mes) {
    this.mes = mes;
  }

  public LocalDate getFechaCierre() {
    return fechaCierre;
  }

  public void setFechaCierre(LocalDate fechaCierre) {
    this.fechaCierre = fechaCierre;
  }

  public LocalDate getFechaVencimiento() {
    return fechaVencimiento;
  }

  public void setFechaVencimiento(LocalDate fechaVencimiento) {
    this.fechaVencimiento = fechaVencimiento;
  }

  @Override
  public String toString() {
    return "CierreTarjeta{" +
        "idCierre=" + idCierre +
        ", tarjetaId=" + tarjetaId +
        ", mes=" + mes +
        ", fechaCierre=" + fechaCierre +
        ", fechaVencimiento=" + fechaVencimiento +
        '}';
  }
}