package ar.com.gastos.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MovimientoEventBus {
  private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

  public static void subscribe(Consumer<String> listener) {
    listeners.add(listener);
  }

  public static void unsubscribe(Consumer<String> listener) {
    listeners.remove(listener);
  }

  public static void publish(String tarjetaNombre) {
    for (Consumer<String> l : listeners) {
      try {
        l.accept(tarjetaNombre);
      } catch (Exception ex) {
        // opcional: podés loggear la excepción si tenés logger accesible
        ex.printStackTrace();
      }
    }
  }
}
