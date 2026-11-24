package ar.com.gastos.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Carga centralizada de config.properties, compartida por Db.java y BackupService.java.
 * ANTES: Db.java buscaba el archivo externo (directorio de trabajo / ~/.gastos / classpath),
 * pero BackupService.java SOLO leia el classpath (el que queda empaquetado dentro del .jar
 * al compilar). Eso hacia que backup.dir quedara fijo para siempre en lo que se compilo en
 * la PC original, sin poder pisarlo desde afuera en una PC nueva. Ahora ambas clases usan
 * esta misma logica.
 * <p>
 * Orden de busqueda de config.properties externo:
 * 1. Carpeta donde esta el .jar (getAppDir()) -> config.properties
 * 2. Directorio de trabajo actual (compatibilidad con versiones viejas)
 * 3. Carpeta del usuario: ~/.gastos/config.properties
 * 4. Classpath (src/main/resources) -> solo como fallback para desarrollo en IntelliJ
 */
public final class ConfigLoader {

  private static Properties cache;

  private ConfigLoader() {
  }

  /**
   * Devuelve la carpeta donde esta ubicado el .jar en ejecucion.
   * Esto NO depende del directorio de trabajo (que puede variar segun como se
   * lance la app: doble click, acceso directo, .vbs, gradlew run, etc.), asi que
   * es la referencia mas confiable para ubicar archivos "al lado del .jar"
   * (config.properties, carpeta de backups) sin depender de un usuario ni de una
   * unidad fija como C:\.
   * <p>
   * Si la app corre desde IntelliJ (clases sueltas, no dentro de un .jar), devuelve
   * el directorio de trabajo actual como fallback razonable para desarrollo.
   */
  public static Path getAppDir() {
    try {
      Path jarPath = Paths.get(
          ConfigLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      // Si estamos corriendo desde un .jar, el padre del .jar es la carpeta de la app.
      // Si estamos corriendo desde carpetas de clases sueltas (IDE), usamos el directorio actual.
      if (Files.isRegularFile(jarPath)) {
        return jarPath.getParent();
      }
    } catch (URISyntaxException | NullPointerException e) {
      // Sigue al fallback de abajo
    }
    return Paths.get("").toAbsolutePath();
  }

  /**
   * Carga config.properties (con cache en memoria, se lee una sola vez por ejecucion).
   */
  public static synchronized Properties cargarConfiguracion() {
    if (cache != null) {
      return cache;
    }
    cache = buscarYCargar();
    return cache;
  }

  private static Properties buscarYCargar() {
    Properties props = new Properties();

    // Opcion 1: al lado del .jar (la mas confiable, no depende de como se lanzo la app)
    Path appDirPath = getAppDir().resolve("config.properties");
    if (Files.exists(appDirPath)) {
      return leer(appDirPath, "junto al .jar (" + appDirPath + ")");
    }

    // Opcion 2: directorio de trabajo actual (compatibilidad hacia atras)
    Path localPath = Paths.get("config.properties");
    if (Files.exists(localPath)) {
      return leer(localPath, "directorio de trabajo (" + localPath.toAbsolutePath() + ")");
    }

    // Opcion 3: carpeta del usuario ~/.gastos/config.properties
    Path userPath = Paths.get(System.getProperty("user.home"), ".gastos", "config.properties");
    if (Files.exists(userPath)) {
      return leer(userPath, "~/.gastos/ (" + userPath + ")");
    }

    // Opcion 4: classpath (src/main/resources) - solo para desarrollo, NO deberia usarse en produccion
    try (InputStream in = ConfigLoader.class.getResourceAsStream("/ar/com/gastos/config.properties")) {
      if (in != null) {
        props.load(in);
        return props;
      }
    } catch (IOException e) {
      throw new RuntimeException("Error al leer config.properties desde el classpath", e);
    }

    throw new RuntimeException(
        "No se encontro config.properties.\n" +
            "Crea el archivo con el siguiente contenido y colocalo en una de estas ubicaciones:\n" +
            "  - Junto al .jar (recomendado): " + getAppDir() + "\\config.properties\n" +
            "  - Carpeta del usuario: ~/.gastos/config.properties\n\n" +
            "Contenido del archivo:\n" +
            "  db.url=jdbc:postgresql://localhost:5432/gastos\n" +
            "  db.user=postgres\n" +
            "  db.password=TU_PASSWORD_ACA\n" +
            "  backup.dir=backups\n"
    );
  }

  private static Properties leer(Path path, String origen) {
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      props.load(in);
      return props;
    } catch (IOException e) {
      throw new RuntimeException("Error al leer config.properties " + origen, e);
    }
  }

  /**
   * Lee una clave obligatoria. Si no existe o esta vacia, lanza un error con mensaje claro.
   */
  public static String requerido(Properties props, String clave) {
    String valor = props.getProperty(clave);
    if (valor == null || valor.isBlank()) {
      throw new RuntimeException("Falta la clave '" + clave + "' en config.properties");
    }
    return valor.trim();
  }

  /**
   * Resuelve backup.dir de forma que NUNCA quede atado a un usuario ni a una unidad fija:
   * - Si backup.dir esta definido en config.properties y es una ruta ABSOLUTA (ej: "C:/Db_Backups/gastos"),
   *   se respeta tal cual (por si alguien quiere elegir explicitamente otra ubicacion, p.ej. un disco compartido).
   * - Si backup.dir esta definido y es una ruta RELATIVA (ej: "backups"), se resuelve relativa a la
   *   carpeta del .jar (getAppDir()), no al directorio de trabajo.
   * - Si backup.dir no esta definido, se usa por defecto "backups" al lado del .jar.
   * Asi, la carpeta de backups siempre viaja junto con la app al copiarla a otra PC.
   */
  public static Path resolverBackupDir(Properties props) {
    String backupDir = props.getProperty("backup.dir");
    if (backupDir == null || backupDir.isBlank()) {
      backupDir = "backups";
    }
    Path path = Paths.get(backupDir);
    if (path.isAbsolute()) {
      return path;
    }
    return getAppDir().resolve(path);
  }
}
