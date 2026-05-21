package ar.com.gastos.controller;

import ar.com.gastos.dao.MovimientoDao;
import ar.com.gastos.dao.TarjetaDao;
import ar.com.gastos.model.Movimiento;
import ar.com.gastos.model.Tarjeta;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PagoController {

    private static final Logger logger = LoggerFactory.getLogger(PagoController.class);

    @FXML private ComboBox<String> cmbTarjeta;
    @FXML private DatePicker txtFecha;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtMonto;

    @FXML
    public void initialize() {
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

    @FXML
    private void guardarMovimiento() {
        String tarjetaNombre = cmbTarjeta.getValue();
        if (tarjetaNombre == null || tarjetaNombre.isEmpty()) {
            Toast.show(getStage(), "Debe seleccionar una tarjeta");
            return;
        }

        try {
            TarjetaDao tarjetaDao = new TarjetaDao();
            Tarjeta tarjeta = tarjetaDao.findByNombre(tarjetaNombre);
            if (tarjeta == null) {
                Toast.show(getStage(), "Tarjeta no encontrada en la base");
                return;
            }

            LocalDate fecha = txtFecha.getValue();
            if (fecha == null) {
                Toast.show(getStage(), "Debe seleccionar una fecha");
                return;
            }

            String descripcion = txtDescripcion.getText().trim();
            if (descripcion.isEmpty()) {
                Toast.show(getStage(), "Debe ingresar una descripción");
                return;
            }

            BigDecimal monto = new BigDecimal(txtMonto.getText()).setScale(2);

            // Constructor de PAGO — sin comercio_id, con descripción libre
            Movimiento movimiento = new Movimiento(
                tarjeta.getId(),
                fecha,
                descripcion,
                monto,
                "ARS"
            );

            new MovimientoDao().save(movimiento);

            Toast.show(getStage(), "Pago guardado en " + tarjetaNombre + " por $" + monto);
            MovimientoEventBus.publish(tarjetaNombre);
            logger.info("Pago guardado en tarjeta {} por {}", tarjetaNombre, monto);

        } catch (SQLException ex) {
            Toast.show(getStage(), "Error al guardar pago");
            logger.error("Error al guardar pago", ex);
        } catch (NumberFormatException ex) {
            Toast.show(getStage(), "Monto inválido");
            logger.error("Error en formato de monto", ex);
        }
    }

    private Stage getStage() {
        return (Stage) cmbTarjeta.getScene().getWindow();
    }
}