package ar.com.gastos;

import ar.com.gastos.util.Db;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;

public class GastosApplication extends Application {
  @Override
  public void start(Stage stage) throws IOException {
    try (Connection c = Db.getDataSource().getConnection()) {
      System.out.println("Conexión OK: " + c.getMetaData().getDatabaseProductName());
    } catch (Exception e) {
      e.printStackTrace();
    }

    FXMLLoader fxmlLoader = new FXMLLoader(GastosApplication.class.getResource("dashboard.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 1200, 800); // tamaño inicial más grande
    stage.setTitle("Control de Gastos");
    stage.setScene(scene);

    stage.setMaximized(true);   // 🔹 abre maximizado
    // stage.setFullScreen(true); // 🔹 si querés fullscreen total

    stage.show();
  }
}
