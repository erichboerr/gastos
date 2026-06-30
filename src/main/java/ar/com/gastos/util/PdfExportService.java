package ar.com.gastos.util;

import ar.com.gastos.model.Movimiento;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Genera un PDF con el detalle de movimientos de una tarjeta para un mes dado.
 */
public class PdfExportService {

  private static final NumberFormat CURRENCY =
      NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  static { CURRENCY.setMaximumFractionDigits(2); }

  public static void exportarDetalle(String nombreTarjeta, String nombreMes,
                                     List<Movimiento> movimientos,
                                     BigDecimal totalConsumos, BigDecimal totalPagos,
                                     BigDecimal saldo, String rutaArchivo) throws IOException {

    try (PDDocument doc = new PDDocument()) {

      PDFont fontBold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
      PDFont fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

      float margin = 40;
      float rowHeight = 16;

      PDPage page = new PDPage(PDRectangle.A4);
      doc.addPage(page);
      float pageHeight = page.getMediaBox().getHeight();
      float pageWidth  = page.getMediaBox().getWidth();
      float y = pageHeight - margin;

      PDPageContentStream cs = new PDPageContentStream(doc, page);

      // --- Título ---
      cs.setFont(fontBold, 16);
      cs.beginText();
      cs.newLineAtOffset(margin, y);
      cs.showText(limpiar("Detalle de " + nombreTarjeta));
      cs.endText();
      y -= 22;

      cs.setFont(fontNormal, 12);
      cs.beginText();
      cs.newLineAtOffset(margin, y);
      cs.showText(limpiar(nombreMes));
      cs.endText();
      y -= 25;

      // --- Resumen ---
      cs.setFont(fontNormal, 11);
      cs.beginText();
      cs.newLineAtOffset(margin, y);
      cs.showText(limpiar("Consumos: " + CURRENCY.format(totalConsumos)
          + "   Pagos: " + CURRENCY.format(totalPagos)
          + "   Saldo: " + CURRENCY.format(saldo)));
      cs.endText();
      y -= 25;

      // --- Encabezado de tabla ---
      float[] colX = {margin, margin + 70, margin + 270, margin + 350, margin + 410};
      String[] headers = {"Fecha", "Descripcion", "Monto", "Cuota", "Comentario"};

      cs.setNonStrokingColor(0.9f, 0.9f, 0.9f);
      cs.addRect(margin, y - 4, pageWidth - 2 * margin, rowHeight);
      cs.fill();
      cs.setNonStrokingColor(0, 0, 0);

      cs.setFont(fontBold, 9);
      for (int i = 0; i < headers.length; i++) {
        cs.beginText();
        cs.newLineAtOffset(colX[i], y);
        cs.showText(headers[i]);
        cs.endText();
      }
      y -= rowHeight + 4;

      // --- Filas ---
      cs.setFont(fontNormal, 9);

      for (Movimiento m : movimientos) {
        if (y < margin + 30) {
          // Salto de página — cerramos el stream actual y abrimos uno nuevo
          cs.close();
          page = new PDPage(PDRectangle.A4);
          doc.addPage(page);
          cs = new PDPageContentStream(doc, page);
          cs.setFont(fontNormal, 9); // ← clave: reseteamos la fuente en la página nueva
          y = pageHeight - margin;
        }

        String fecha  = m.getFecha() != null ? m.getFecha().format(DATE_FMT) : "";
        String desc   = m.getDescripcion() != null ? m.getDescripcion() : "";
        String monto  = CURRENCY.format(m.getMonto());
        String cuota  = m.getCuotaTexto() != null ? m.getCuotaTexto() : "";
        String coment = m.getComentario() != null ? m.getComentario() : "";

        cs.beginText();
        cs.newLineAtOffset(colX[0], y);
        cs.showText(limpiar(fecha));
        cs.endText();

        cs.beginText();
        cs.newLineAtOffset(colX[1], y);
        cs.showText(limpiar(truncar(desc, 35)));
        cs.endText();

        cs.beginText();
        cs.newLineAtOffset(colX[2], y);
        cs.showText(limpiar(monto));
        cs.endText();

        cs.beginText();
        cs.newLineAtOffset(colX[3], y);
        cs.showText(limpiar(cuota));
        cs.endText();

        cs.beginText();
        cs.newLineAtOffset(colX[4], y);
        cs.showText(limpiar(truncar(coment, 25)));
        cs.endText();

        y -= rowHeight;
      }

      cs.close();
      doc.save(rutaArchivo);
    }
  }

  // PDFBox con fuente estándar no soporta tildes/ñ directamente — reemplazamos
  private static String limpiar(String texto) {
    if (texto == null) return "";
    return texto
        .replace("á", "a").replace("é", "e").replace("í", "i")
        .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
        .replace("Á", "A").replace("É", "E").replace("Í", "I")
        .replace("Ó", "O").replace("Ú", "U").replace("Ñ", "N");
  }

  private static String truncar(String texto, int maxLen) {
    if (texto == null) return "";
    return texto.length() > maxLen ? texto.substring(0, maxLen) + "..." : texto;
  }
}