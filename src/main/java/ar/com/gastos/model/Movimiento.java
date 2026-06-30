package ar.com.gastos.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Movimiento {

  private int id;
  private int tarjetaId;
  private int comercioId;      // 0 = sin comercio (pagos)
  private LocalDate fecha;
  private String descripcion;  // solo para pagos y recurrentes
  private BigDecimal monto;
  private String categoria;
  private String moneda;
  private int cuotas;
  private String cuotaTexto;   // campo calculado, no se persiste
  private boolean pagado; // persiste en DB
  private String comentario; // opcional, persiste en DB

  // --- Constructor completo — usado al leer desde la DB ---
  public Movimiento(int id, int tarjetaId, int comercioId, LocalDate fecha,
                    String descripcion, BigDecimal monto, String categoria,
                    String moneda, int cuotas, boolean pagado, String comentario) {
    this.id = id;
    this.tarjetaId = tarjetaId;
    this.comercioId = comercioId;
    this.fecha = fecha;
    this.descripcion = descripcion;
    this.monto = monto;
    this.categoria = categoria;
    this.moneda = moneda;
    this.cuotas = cuotas;
    this.cuotaTexto = "";
    this.pagado = pagado;
    this.comentario = comentario;
  }

  // --- Constructor para EGRESO — con comercio, sin descripción libre ---
  public Movimiento(int tarjetaId, int comercioId, LocalDate fecha,
                    BigDecimal monto, String moneda, int cuotas) {
    this(0, tarjetaId, comercioId, fecha, null, monto, "EGRESO", moneda, cuotas, false, null);
  }

  // --- Constructor para PAGO — sin comercio, con descripción libre ---
  public Movimiento(int tarjetaId, LocalDate fecha, String descripcion,
                    BigDecimal monto, String moneda) {
    this(0, tarjetaId, 0, fecha, descripcion, monto, "PAGO", moneda, 1, false, null);
  }

  // --- Constructor para RECURRENTES — con comercio, cuota única ---
  public Movimiento(int tarjetaId, int comercioId, LocalDate fecha, BigDecimal monto, String moneda) {
    this(0, tarjetaId, comercioId, fecha, null, monto, "EGRESO", moneda, 1, false, null);
  }

  // --- Constructor para recurrentes — descripción libre, categoría EGRESO, cuota única ---
  public Movimiento(int tarjetaId, LocalDate fecha, String descripcion,
                    BigDecimal monto, String categoria, String moneda) {
    this(0, tarjetaId, 0, fecha, descripcion, monto, categoria, moneda, 1, false, null);
  }

  // --- Getters y Setters ---

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

  public int getComercioId() {
    return comercioId;
  }

  public void setComercioId(int comercioId) {
    this.comercioId = comercioId;
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
    return cuotaTexto;
  }

  public void setCuotaTexto(String cuotaTexto) {
    this.cuotaTexto = cuotaTexto;
  }

  public boolean isPagado() { return pagado; }

  public void setPagado(boolean pagado) { this.pagado = pagado; }

  public String getComentario() { return comentario; }

  public void setComentario(String comentario) { this.comentario = comentario; }
}