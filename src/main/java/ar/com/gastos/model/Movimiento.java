package ar.com.gastos.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Movimiento {
  private int id;
  private int tarjetaId;
  private LocalDate fecha;
  private String descripcion;
  private BigDecimal monto;
  private String categoria;
  private String moneda;
  private int cuotas;
  private String cuotaTexto;

  public Movimiento(int id, int tarjetaId, LocalDate fecha, String descripcion,
                    BigDecimal monto, String categoria, String moneda, int cuotas) {
    this.id = id;
    this.tarjetaId = tarjetaId;
    this.fecha = fecha;
    this.descripcion = descripcion;
    this.monto = monto;
    this.categoria = categoria;
    this.moneda = moneda;
    this.cuotas = cuotas;
    this.cuotaTexto = "";
  }

  // Constructor para insertar
  public Movimiento(int tarjetaId, LocalDate fecha, String descripcion,
                    BigDecimal monto, String categoria, String moneda, int cuotas) {
    this(0, tarjetaId, fecha, descripcion, monto, categoria, moneda, cuotas);
  }

  // Getters y setters...

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getTarjetaId() {
    return tarjetaId;
  }

  public void setTarjetaId(int tarjetaId) {
    this.tarjetaId = tarjetaId;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public void setFecha(LocalDate fecha) {
    this.fecha = fecha;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public BigDecimal getMonto() {
    return monto;
  }

  public void setMonto(BigDecimal monto) {
    this.monto = monto;
  }

  public String getCategoria() {
    return categoria;
  }

  public void setCategoria(String categoria) {
    this.categoria = categoria;
  }

  public String getMoneda() {
    return moneda;
  }

  public void setMoneda(String moneda) {
    this.moneda = moneda;
  }

  public int getCuotas() {
    return cuotas;
  }

  public void setCuotas(int cuotas) {
    this.cuotas = cuotas;
  }

  public String getCuotaTexto() {
    return cuotaTexto;  }

  public void setCuotaTexto(String cuotaTexto) {
    this.cuotaTexto = cuotaTexto;
  }

}

