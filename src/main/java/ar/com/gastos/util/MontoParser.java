package ar.com.gastos.util;

import java.math.BigDecimal;

/**
 * Convierte importes escritos por el usuario a BigDecimal sin depender
 * de la configuración regional del teclado o del sistema operativo.
 */
public final class MontoParser {

  private MontoParser() {
  }

  /**
   * Acepta formatos como 1250,50, 1250.50, 1.250,50 y 1,250.50.
   */
  public static BigDecimal parse(String texto) {
    if (texto == null || texto.trim().isEmpty()) {
      throw new NumberFormatException("Monto vacío");
    }

    String valor = texto.trim()
        .replace("$", "")
        .replace(" ", "");

    int ultimaComa = valor.lastIndexOf(',');
    int ultimoPunto = valor.lastIndexOf('.');

    if (ultimaComa >= 0 && ultimoPunto >= 0) {
      // El último separador se interpreta como decimal y el otro como miles.
      if (ultimaComa > ultimoPunto) {
        valor = valor.replace(".", "").replace(',', '.');
      } else {
        valor = valor.replace(",", "");
      }
    } else if (ultimaComa >= 0) {
      valor = valor.replace(',', '.');
    }

    return new BigDecimal(valor).setScale(2);
  }
}
