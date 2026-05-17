package ar.com.gastos;

import javafx.application.Application;

/**
 * Clase de arranque separada de GastosApplication.
 *
 * ¿Por qué existe este archivo? JavaFX tiene una restricción técnica:
 * cuando se empaqueta la app como .jar sin sistema de módulos (module-info),
 * la clase con el main() no puede ser la misma que extiende Application.
 * Este Launcher actúa como puente y delega el arranque a GastosApplication.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(GastosApplication.class, args);
    }
}