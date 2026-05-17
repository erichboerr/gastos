package ar.com.gastos.controller;

import ar.com.gastos.dao.EgresoDao;
import ar.com.gastos.dao.TarjetaDao;
import ar.com.gastos.model.Movimiento;
import ar.com.gastos.model.Tarjeta;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class EgresoController {

    private static final Logger logger = LoggerFactory.getLogger(EgresoController.class);

    @FXML private ComboBox<String> cmbTarjeta;
    @FXML private ComboBox<String> cmbDescripcion;
    @FXML private DatePicker txtFecha;
    @FXML private TextField txtMonto;
    @FXML private TextField txtCuotas;

    @FXML
    public void initialize() {
        cargarTarjetas();
        cargarDescripciones();
    }

    // Carga las tarjetas activas en el ComboBox de tarjetas
    private void cargarTarjetas() {
        try {
            TarjetaDao tarjetaDao = new TarjetaDao();
            List<Tarjeta> tarjetas = tarjetaDao.findAllActivas();
            cmbTarjeta.getItems().clear();
            for (Tarjeta t : tarjetas) {
                cmbTarjeta.getItems().add(t.getNombre());
            }
        } catch (Exception e) {
            logger.error("Error al cargar tarjetas", e);
        }
    }

    // Carga las descripciones distintas que ya existen en movimientos de tipo EGRESO.
    // Como todas se guardan en mayúsculas, no hay duplicados por capitalización.
    private void cargarDescripciones() {
        try {
            EgresoDao egresoDao = new EgresoDao();
            List<String> descripciones = egresoDao.findDescripcionesDistintas();
            cmbDescripcion.getItems().clear();
            cmbDescripcion.getItems().addAll(descripciones);

            // Permitimos que el usuario escriba en el combo si quiere filtrar
            cmbDescripcion.setEditable(true);
        } catch (Exception e) {
            logger.error("Error al cargar descripciones", e);
        }
    }

    // Abre un diálogo para ingresar una nueva descripción que no existe todavía.
    // La agrega al ComboBox y la selecciona automáticamente — no la guarda en la base
    // hasta que el usuario confirme el egreso con "Guardar".
    @FXML
    private void nuevaDescripcion() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva descripción");
        dialog.setHeaderText(null);
        dialog.setContentText("Ingresá la nueva descripción:");

        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(texto -> {
            // Normalizamos a mayúsculas y sin espacios extra, igual que al guardar
            String nueva = texto.toUpperCase().trim();
            if (!nueva.isEmpty()) {
                // Si ya existe en el combo no la duplicamos
                if (!cmbDescripcion.getItems().contains(nueva)) {
                    cmbDescripcion.getItems().add(nueva);
                }
                // La seleccionamos para que quede lista para usar
                cmbDescripcion.setValue(nueva);
            }
        });
    }

    @FXML
    private void guardarMovimiento() {
        String tarjetaNombre = cmbTarjeta.getValue();
        if (tarjetaNombre == null || tarjetaNombre.isEmpty()) {
            Toast.show((Stage) txtFecha.getScene().getWindow(), "Debe seleccionar una tarjeta");
            return;
        }

        String descripcion = cmbDescripcion.getValue();
        if (descripcion == null || descripcion.isBlank()) {
            Toast.show((Stage) txtFecha.getScene().getWindow(), "Debe ingresar una descripción");
            return;
        }

        try {
            TarjetaDao tarjetaDao = new TarjetaDao();
            Tarjeta tarjeta = tarjetaDao.findByNombre(tarjetaNombre);
            if (tarjeta == null) {
                Toast.show((Stage) txtFecha.getScene().getWindow(), "Tarjeta no encontrada en la base");
                return;
            }

            LocalDate fecha = txtFecha.getValue();
            if (fecha == null) {
                Toast.show((Stage) txtFecha.getScene().getWindow(), "Debe seleccionar una fecha");
                return;
            }

            BigDecimal monto = new BigDecimal(txtMonto.getText()).setScale(2);
            int cuotas = txtCuotas.getText().isEmpty() ? 1 : Integer.parseInt(txtCuotas.getText());

            Movimiento movimiento = new Movimiento(
                  tarjeta.getId(),
                  fecha,
                  descripcion,   // EgresoDao.save() la convierte a mayúsculas antes de insertar
                  monto,
                  "EGRESO",
                  "ARS",
                  cuotas
            );

            EgresoDao dao = new EgresoDao();
            dao.save(movimiento);

            // Recargamos el combo para que la nueva descripción aparezca la próxima vez
            cargarDescripciones();

            Toast.show((Stage) txtFecha.getScene().getWindow(),
                  "Egreso guardado en " + tarjetaNombre + " por $" + monto);

            MovimientoEventBus.publish(tarjetaNombre);
            logger.info("Egreso guardado en tarjeta {} por {}", tarjetaNombre, monto);

        } catch (SQLException ex) {
            Toast.show((Stage) txtFecha.getScene().getWindow(), "Error al guardar egreso");
            logger.error("Error al guardar egreso", ex);
        } catch (NumberFormatException ex) {
            Toast.show((Stage) txtFecha.getScene().getWindow(), "Monto/cuotas inválido");
            logger.error("Error en formato de monto/cuotas", ex);
        }
    }
}