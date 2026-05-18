package ar.com.gastos.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Modelo auxiliar para la pantalla "Generar mes".
 * Representa una fila en la lista: checkbox + descripción + monto a ingresar.
 * No se persiste — es solo para la UI.
 */
public class GenerarMesItem {

  // Si está marcado se generará el egreso al confirmar
  private final BooleanProperty seleccionado = new SimpleBooleanProperty(true);

  // Monto ingresado por el usuario en esa fila
  private final StringProperty monto = new SimpleStringProperty("");

  // Referencia al recurrente original
  private final GastoRecurrente gastoRecurrente;

  public GenerarMesItem(GastoRecurrente gastoRecurrente) {
    this.gastoRecurrente = gastoRecurrente;
  }

  public GastoRecurrente getGastoRecurrente() { return gastoRecurrente; }

  public boolean isSeleccionado() { return seleccionado.get(); }
  public BooleanProperty seleccionadoProperty() { return seleccionado; }
  public void setSeleccionado(boolean v) { seleccionado.set(v); }

  public String getMonto() { return monto.get(); }
  public StringProperty montoProperty() { return monto; }
  public void setMonto(String v) { monto.set(v); }
}