package ar.com.gastos.controller;

import ar.com.gastos.dao.ComercioDao;
import ar.com.gastos.dao.MovimientoDao;
import ar.com.gastos.dao.TarjetaDao;
import ar.com.gastos.model.Comercio;
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

    @FXML private ComboBox<String>   cmbTarjeta;
    @FXML private ComboBox<Comercio> cmbDescripcion;
    @FXML private DatePicker         txtFecha;
    @FXML private TextField          txtMonto;
    @FXML private TextField          txtCuotas;

    @FXML
    public void initialize() {
        cargarTarjetas();
        cargarComercios();
        // Permitimos escribir en el combo para filtrar
        cmbDescripcion.setEditable(true);
    }

    // Carga las tarjetas activas en el ComboBox
    private void cargarTarjetas() {
        try {
            TarjetaDao dao = new TarjetaDao();
            List<Tarjeta> tarjetas = dao.findAllActivas();
            cmbTarjeta.getItems().clear();
            for (Tarjeta t : tarjetas) {
                cmbTarjeta.getItems().add(t.getNombre());
            }
        } catch (Exception e) {
            logger.error("Error al cargar tarjetas", e);
        }
    }

    // Carga el catálogo de comercios habilitados desde la tabla comercio
    private void cargarComercios() {
        try {
            ComercioDao dao = new ComercioDao();
            List<Comercio> comercios = dao.findAllActivos();
            cmbDescripcion.getItems().clear();
            cmbDescripcion.getItems().addAll(comercios);
        } catch (Exception e) {
            logger.error("Error al cargar comercios", e);
        }
    }

    // Abre diálogo para crear un comercio nuevo en el momento
    @FXML
    private void nuevaDescripcion() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuevo comercio");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre del comercio:");

        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(texto -> {
            String nombre = texto.toUpperCase().trim();
            if (nombre.isEmpty()) return;

            try {
                ComercioDao dao = new ComercioDao();

                // Verificamos si ya existe (activo o dado de baja)
                Comercio existente = dao.findByNombre(nombre);
                if (existente != null) {
                    // Ya existe — lo seleccionamos directamente
                    cmbDescripcion.setValue(existente);
                    return;
                }

                // Creamos el comercio nuevo sin categoría por ahora
                Comercio nuevo = new Comercio(nombre, null);
                dao.save(nuevo);

                // Recargamos y seleccionamos el nuevo
                cargarComercios();
                cmbDescripcion.getItems().stream()
                    .filter(c -> c.getNombre().equals(nombre))
                    .findFirst()
                    .ifPresent(cmbDescripcion::setValue);

                logger.info("Comercio creado: {}", nombre);

            } catch (SQLException ex) {
                logger.error("Error al crear comercio", ex);
            }
        });
    }

    @FXML
    private void guardarMovimiento() {
        String tarjetaNombre = cmbTarjeta.getValue();
        if (tarjetaNombre == null || tarjetaNombre.isEmpty()) {
            Toast.show(getStage(), "Debe seleccionar una tarjeta");
            return;
        }

        // El ComboBox editable puede tener un objeto Comercio seleccionado
        // o un String escrito a mano — manejamos ambos casos
        Comercio comercio = null;
        Object valor = cmbDescripcion.getValue();
        if (valor instanceof Comercio) {
            comercio = (Comercio) valor;
        } else if (valor instanceof String) {
            // El usuario escribió un nombre — buscamos o creamos el comercio
            String nombre = ((String) valor).toUpperCase().trim();
            if (!nombre.isEmpty()) {
                try {
                    ComercioDao dao = new ComercioDao();
                    comercio = dao.findByNombre(nombre);
                    if (comercio == null) {
                        // Lo creamos on the fly
                        Comercio nuevo = new Comercio(nombre, null);
                        dao.save(nuevo);
                        cargarComercios();
                        comercio = dao.findByNombre(nombre);
                    }
                } catch (SQLException ex) {
                    logger.error("Error al buscar/crear comercio", ex);
                }
            }
        }

        if (comercio == null) {
            Toast.show(getStage(), "Debe seleccionar o ingresar un comercio");
            return;
        }

        LocalDate fecha = txtFecha.getValue();
        if (fecha == null) {
            Toast.show(getStage(), "Debe seleccionar una fecha");
            return;
        }

        try {
            TarjetaDao tarjetaDao = new TarjetaDao();
            Tarjeta tarjeta = tarjetaDao.findByNombre(tarjetaNombre);
            if (tarjeta == null) {
                Toast.show(getStage(), "Tarjeta no encontrada");
                return;
            }

            BigDecimal monto = new BigDecimal(txtMonto.getText()).setScale(2);
            int cuotas = txtCuotas.getText().isEmpty() ? 1 : Integer.parseInt(txtCuotas.getText());

            Movimiento movimiento = new Movimiento(
                tarjeta.getId(),
                comercio.getId(),
                fecha,
                monto,
                "ARS",
                cuotas
            );

            new MovimientoDao().save(movimiento);

            cargarComercios();
            Toast.show(getStage(), "Egreso guardado en " + tarjetaNombre + " por $" + monto);
            MovimientoEventBus.publish(tarjetaNombre);
            logger.info("Egreso guardado: {} en {} por ${}", comercio.getNombre(), tarjetaNombre, monto);

        } catch (SQLException ex) {
            Toast.show(getStage(), "Error al guardar egreso");
            logger.error("Error al guardar egreso", ex);
        } catch (NumberFormatException ex) {
            Toast.show(getStage(), "Monto/cuotas inválido");
            logger.error("Error en formato de monto/cuotas", ex);
        }
    }

    private Stage getStage() {
        return (Stage) cmbTarjeta.getScene().getWindow();
    }
}