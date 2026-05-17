package ar.com.gastos.controller;

import ar.com.gastos.dao.PagoDao;
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
            logger.error("Error al cargar tarjetas desde la base", e);
        }
    }

    @FXML
    private void guardarMovimiento() {
        String tarjetaNombre = cmbTarjeta.getValue();
        if (tarjetaNombre == null || tarjetaNombre.isEmpty()) {
            Toast.show((Stage) txtFecha.getScene().getWindow(), "Debe seleccionar una tarjeta");
            logger.error("Debe seleccionar una tarjeta");
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
            String descripcion = txtDescripcion.getText();
            BigDecimal monto = new BigDecimal(txtMonto.getText()).setScale(2);

            Movimiento movimiento = new Movimiento(
                tarjeta.getId(),
                fecha,
                descripcion,
                monto,
                "PAGO", // 🔹 categoría fija
                "ARS",
                1 // siempre son pago único
            );

            PagoDao dao = new PagoDao();
            dao.save(movimiento);

            Toast.show((Stage) txtFecha.getScene().getWindow(),
                "Pago guardado en " + tarjetaNombre + " por $" + monto);

            MovimientoEventBus.publish(tarjetaNombre);
            logger.info("Pago guardado en tarjeta {} por {}", tarjetaNombre, monto);

        } catch (SQLException ex) {
            Toast.show((Stage) txtFecha.getScene().getWindow(), "Error al guardar pago en " + tarjetaNombre);
            logger.error("Error al guardar pago", ex);
        } catch (NumberFormatException ex) {
            Toast.show((Stage) txtFecha.getScene().getWindow(), "Monto inválido");
            logger.error("Error en formato de monto", ex);
        }
    }
}
