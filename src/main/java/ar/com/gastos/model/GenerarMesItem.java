package ar.com.gastos.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

/**
 * Modelo auxiliar para la pantalla "Generar mes".
 * Representa una fila: checkbox + descripción + fecha individual + monto.
 * Arranca deseleccionado — el usuario elige cuáles generar.
 * No se persiste — es solo para la UI.
 */
public class GenerarMesItem {

  // Arranca en falso — el usuario selecciona los que necesita
  private final BooleanProperty seleccionado = new SimpleBooleanProperty(false);

  // Fecha individual por ítem
  private final ObjectProperty<LocalDate> fecha =
      new SimpleObjectProperty<>(LocalDate.now());

  // Monto ingresado por el usuario
  private final StringProperty monto = new SimpleStringProperty("");

  private final GastoRecurrente gastoRecurrente;

  public GenerarMesItem(GastoRecurrente gastoRecurrente) {
    this.gastoRecurrente = gastoRecurrente;
  }

  public GastoRecurrente getGastoRecurrente() { return gastoRecurrente; }

  public boolean isSeleccionado() { return seleccionado.get(); }
  public BooleanProperty seleccionadoProperty() { return seleccionado; }
  public void setSeleccionado(boolean v) { seleccionado.set(v); }

  public LocalDate getFecha() { return fecha.get(); }
  public ObjectProperty<LocalDate> fechaProperty() { return fecha; }
  public void setFecha(LocalDate v) { fecha.set(v); }

  public String getMonto() { return monto.get(); }
  public StringProperty montoProperty() { return monto; }
  public void setMonto(String v) { monto.set(v); }
}