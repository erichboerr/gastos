package ar.com.gastos.controller;

import ar.com.gastos.dao.ComercioDao;
import ar.com.gastos.dao.MovimientoDao;
import ar.com.gastos.dao.TarjetaDao;
import ar.com.gastos.model.Comercio;
import ar.com.gastos.model.Movimiento;
import ar.com.gastos.model.Tarjeta;
import ar.com.gastos.util.MovimientoEventBus;
import ar.com.gastos.util.Toast;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.stream.Collectors;

public class EgresoController {

    private static final Logger logger = LoggerFactory.getLogger(EgresoController.class);

    @FXML private ComboBox<String>   cmbTarjeta;
    @FXML private ComboBox<Comercio> cmbDescripcion;
    @FXML private DatePicker         txtFecha;
    @FXML private TextField          txtMonto;
    @FXML private TextField          txtCuotas;

    // Lista completa de comercios — usada como base para el filtro
    private ObservableList<Comercio> todosLosComercios = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cargarTarjetas();
        cargarComercios();
        configurarFiltroComercios();
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

    // Carga el catálogo completo de comercios habilitados
    private void cargarComercios() {
        try {
            ComercioDao dao = new ComercioDao();
            List<Comercio> comercios = dao.findAllActivos();
            todosLosComercios.setAll(comercios);
            cmbDescripcion.setItems(FXCollections.observableArrayList(comercios));
        } catch (Exception e) {
            logger.error("Error al cargar comercios", e);
        }
    }

    /**
     * Configura el filtro automático del ComboBox mientras el usuario escribe.
     * Cada vez que cambia el texto del editor, filtramos la lista de comercios
     * mostrando solo los que contienen el texto ingresado.
     */
    private void configurarFiltroComercios() {
        cmbDescripcion.setEditable(true);

        cmbDescripcion.getEditor().textProperty().addListener((obs, anterior, nuevo) -> {
            // Si el texto cambió porque el usuario seleccionó un item, no filtramos
            Comercio seleccionado = cmbDescripcion.getValue();
            if (seleccionado != null && seleccionado.getNombre().equalsIgnoreCase(nuevo)) return;

            String filtro = nuevo == null ? "" : nuevo.toUpperCase().trim();

            if (filtro.isEmpty()) {
                // Sin texto — mostramos todos
                cmbDescripcion.setItems(FXCollections.observableArrayList(todosLosComercios));
            } else {
                // Filtramos los que contienen el texto en cualquier posición
                List<Comercio> filtrados = todosLosComercios.stream()
                    .filter(c -> c.getNombre().toUpperCase().contains(filtro))
                    .collect(Collectors.toList());
                cmbDescripcion.setItems(FXCollections.observableArrayList(filtrados));
            }

            // Mostramos el popup con los resultados filtrados
            if (!cmbDescripcion.isShowing()) {
                cmbDescripcion.show();
            }
        });
    }

    // Abre diálogo para crear un comercio nuevo en el momento
    @FXML
    private void nuevaDescripcion() {
        // Tomamos el texto que ya escribió el usuario como valor inicial del diálogo
        String textoActual = cmbDescripcion.getEditor().getText().toUpperCase().trim();

        TextInputDialog dialog = new TextInputDialog(textoActual);
        dialog.setTitle("Nuevo comercio");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre del comercio:");

        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(texto -> {
            String nombre = texto.toUpperCase().trim();
            if (nombre.isEmpty()) return;

            try {
                ComercioDao dao = new ComercioDao();

                Comercio existente = dao.findByNombre(nombre);
                if (existente != null) {
                    cmbDescripcion.setValue(existente);
                    return;
                }

                Comercio nuevo = new Comercio(nombre, null);
                dao.save(nuevo);

                cargarComercios();

                // Seleccionamos el recién creado
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

        Comercio comercio = null;
        Object valor = cmbDescripcion.getValue();
        if (valor instanceof Comercio) {
            comercio = (Comercio) valor;
        } else if (valor instanceof String) {
            String nombre = ((String) valor).toUpperCase().trim();
            if (!nombre.isEmpty()) {
                try {
                    ComercioDao dao = new ComercioDao();
                    comercio = dao.findByNombre(nombre);
                    if (comercio == null) {
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

            // Reseteamos el ComboBox para la próxima carga
            cmbDescripcion.getEditor().clear();
            cmbDescripcion.setValue(null);
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