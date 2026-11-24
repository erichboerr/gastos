package ar.com.gastos.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Punto único de acceso a config.properties.
 * <p>
 * Antes, Db.java buscaba el archivo en 3 ubicaciones y BackupService.java solo en el
 * classpath, así que la app conectaba a la base pero el backup fallaba con
 * "No se encontró config.properties". Ahora ambos usan esta clase.
 * <p>
 * Orden de búsqueda (gana la primera que exista):
 * 1. Directorio de trabajo actual (junto al .jar)
 * 2. Carpeta del usuario: ~/.gastos/config.properties
 * 3. Classpath: /ar/com/gastos/config.properties (solo desarrollo, no subir al repo)
 */
public final class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    private static final String NOMBRE = "config.properties";
    private static final String RECURSO_CLASSPATH = "/ar/com/gastos/config.properties";

    // Se carga una sola vez y se reutiliza (Db y BackupService comparten la misma instancia)
    private static Properties cache;

    private Config() {
    }

    /**
     * Devuelve las propiedades ya cargadas. Lanza RuntimeException con instrucciones
     * claras si el archivo no existe en ninguna de las ubicaciones.
     */
    public static synchronized Properties cargar() {
        if (cache == null) {
            cache = leerDesdeUbicaciones();
        }
        return cache;
    }

    /**
     * Lee una clave obligatoria. Si falta o está vacía, falla con un mensaje claro
     * en lugar de propagar un null silencioso.
     */
    public static String requerido(String clave) {
        String valor = cargar().getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new RuntimeException("Falta la clave '" + clave + "' en config.properties");
        }
        return valor.trim();
    }

    // --- Búsqueda en las 3 ubicaciones ---

    private static Properties leerDesdeUbicaciones() {
        Properties props = new Properties();

        // Opción 1: directorio de trabajo (donde se ejecuta el .jar)
        Path local = Paths.get(NOMBRE);
        if (Files.exists(local)) {
            leerArchivo(local, props);
            logger.info("config.properties cargado desde el directorio de trabajo: {}",
                    local.toAbsolutePath());
            return props;
        }

        // Opción 2: carpeta del usuario ~/.gastos/config.properties
        Path usuario = Paths.get(System.getProperty("user.home"), ".gastos", NOMBRE);
        if (Files.exists(usuario)) {
            leerArchivo(usuario, props);
            logger.info("config.properties cargado desde la carpeta del usuario: {}", usuario);
            return props;
        }

        // Opción 3: classpath (src/main/resources — solo para desarrollo)
        try (InputStream in = Config.class.getResourceAsStream(RECURSO_CLASSPATH)) {
            if (in != null) {
                props.load(in);
                logger.info("config.properties cargado desde el classpath");
                return props;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al leer config.properties desde el classpath", e);
        }

        // No está en ningún lado: explicamos qué hacer y dónde buscamos
        throw new RuntimeException(
                "No se encontró config.properties.\n" +
                        "Se buscó en:\n" +
                        "  1. " + local.toAbsolutePath() + "\n" +
                        "  2. " + usuario + "\n" +
                        "  3. classpath: " + RECURSO_CLASSPATH + "\n\n" +
                        "Creá el archivo con este contenido y colocalo en una de esas ubicaciones:\n" +
                        "  db.url=jdbc:postgresql://localhost:5432/gastos\n" +
                        "  db.user=postgres\n" +
                        "  db.password=TU_PASSWORD_ACA\n" +
                        "  backup.dir=C:/Db_Backups/gastos\n"
        );
    }

    // Carga un archivo del disco dentro de props, con el path en el mensaje de error
    private static void leerArchivo(Path ruta, Properties props) {
        try (InputStream in = Files.newInputStream(ruta)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error al leer " + ruta.toAbsolutePath(), e);
        }
    }
}