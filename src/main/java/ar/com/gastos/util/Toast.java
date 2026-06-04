package ar.com.gastos.util;

import javafx.animation.FadeTransition;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Toast {

  public static void show(Stage ownerStage, String message) {
    Popup popup = new Popup();
    Label label = new Label(message);
    label.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 5;");
    popup.getContent().add(label);

    // Mostrar en el centro de la ventana
    Scene scene = ownerStage.getScene();
    double x = ownerStage.getX() + scene.getWidth() / 2 - 100;
    double y = ownerStage.getY() + scene.getHeight() / 2 - 20;
    popup.show(ownerStage, x, y);

    // Animación de fade out
    FadeTransition fade = new FadeTransition(Duration.seconds(2), label);
    fade.setFromValue(1.0);
    fade.setToValue(0.0);
    fade.setOnFinished(e -> popup.hide());
    fade.play();
  }
}
