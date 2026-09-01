Set objShell = CreateObject("Wscript.Shell")
objShell.Run "java --module-path ""C:\javafx-sdk-21.0.6\lib"" --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.swing -jar ""C:\Users\Erich Desarrollo\Documents\Proyectos_Java\gastos\build\libs\gastos-1.0-SNAPSHOT-all.jar""", 0, True
