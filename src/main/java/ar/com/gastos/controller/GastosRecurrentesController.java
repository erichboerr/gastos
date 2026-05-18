package ar.com.gastos.controller;

import ar.com.gastos.dao.GastoRecurrenteDao;
import ar.com.gastos.model.GastoRecurrente;
import ar.com.gastos.util.Toast;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class GastosRecurrentesController {

  private static final Logger logger = LoggerFactory.getLogger(GastosRecurrentesController.class);

  @FXML private TextField txtDescripcion;
  @FXML private TextField txtCategoria;
  @FXML private ComboBox<String> cmbMedioPago;
  @FXML private ListView<GastoRecurrente> lstRecurrentes;

  // Cuando estamos editando, guardamos el id aquí. -1 = modo alta.
  private int idEditando = -1;

  @FXML
  public void initialize() {
    // Opciones fijas de medio de pago
    cmbMedioPago.getItems().addAll("DEBITO", "CREDITO", "EFECTIVO");
    cmbMedioPago.setValue("DEBITO");

    cargarLista();
  }

  // --- Carga la lista desde la DB ---

  private void cargarLista() {
    lstRecurrentes.getItems().clear();
    try {
      GastoRecurrenteDao dao = new GastoRecurrenteDao();
      List<GastoRecurrente> lista = dao.findAll();
      lstRecurrentes.getItems().addAll(lista);
    } catch (SQLException ex) {
      logger.error("Error al cargar gastos recurrentes", ex);
    }
  }

  // --- Alta / Modificación ---

  @FXML
  private void guardar() {
    String descripcion = txtDescripcion.getText().trim();
    String categoria   = txtCategoria.getText().trim();
    String medioPago   = cmbMedioPago.getValue();

    if (descripcion.isEmpty() || medioPago == null) {
      Toast.show(getStage(), "Descripción y medio de pago son obligatorios");
      return;
    }

    try {
      GastoRecurrenteDao dao = new GastoRecurrenteDao();

      if (idEditando == -1) {
        // Modo alta
        GastoRecurrente nuevo = new GastoRecurrente(descripcion, categoria, medioPago);
        dao.save(nuevo);
        logger.info("Gasto recurrente creado: {}", descripcion);
        Toast.show(getStage(), "Recurrente guardado");
      } else {
        // Modo edición
        GastoRecurrente editado = new GastoRecurrente(descripcion, categoria, medioPago);
        editado.setId(idEditando);
        dao.update(editado);
        logger.info("Gasto recurrente actualizado: {}", descripcion);
        Toast.show(getStage(), "Recurrente actualizado");
      }

      limpiar();
      cargarLista();

    } catch (SQLException ex) {
      Toast.show(getStage(), "Error al guardar");
      logger.error("Error al guardar gasto recurrente", ex);
    }
  }

  // --- Carga el seleccionado en el formulario para editar ---

  @FXML
  private void editar() {
    GastoRecurrente seleccionado = lstRecurrentes.getSelectionModel().getSelectedItem();
    if (seleccionado == null) {
      Toast.show(getStage(), "Seleccione un recurrente para editar");
      return;
    }
    idEditando = seleccionado.getId();
    txtDescripcion.setText(seleccionado.getDescripcion());
    txtCategoria.setText(seleccionado.getCategoria() != null ? seleccionado.getCategoria() : "");
    cmbMedioPago.setValue(seleccionado.getMedioPago());
  }

  // --- Baja ---

  @FXML
  private void eliminar() {
    GastoRecurrente seleccionado = lstRecurrentes.getSelectionModel().getSelectedItem();
    if (seleccionado == null) {
      Toast.show(getStage(), "Seleccione un recurrente para eliminar");
      return;
    }
    try {
      GastoRecurrenteDao dao = new GastoRecurrenteDao();
      dao.delete(seleccionado.getId());
      logger.info("Gasto recurrente eliminado: {}", seleccionado.getDescripcion());
      Toast.show(getStage(), "Recurrente eliminado");
      limpiar();
      cargarLista();
    } catch (SQLException ex) {
      Toast.show(getStage(), "Error al eliminar");
      logger.error("Error al eliminar gasto recurrente", ex);
    }
  }

  // --- Limpia el formulario y vuelve a modo alta ---

  @FXML
  private void limpiar() {
    idEditando = -1;
    txtDescripcion.clear();
    txtCategoria.clear();
    cmbMedioPago.setValue("DEBITO");
  }

  // --- Helper para obtener el Stage desde cualquier control ---

  private Stage getStage() {
    return (Stage) txtDescripcion.getScene().getWindow();
  }
}