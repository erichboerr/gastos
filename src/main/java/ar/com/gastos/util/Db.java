package ar.com.gastos.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Db {

    private static final HikariDataSource ds;

    static {
        // Cargamos la configuración desde un archivo externo al proyecto.
        // Este archivo NO debe entrar al repositorio Git (agregalo a .gitignore).
        // Buscamos el archivo en el directorio donde se ejecuta la app.
        Properties props = cargarConfiguracion();

        HikariConfig cfg = new HikariConfig();

        // Leemos cada valor desde el archivo de configuración.
        // Si falta alguna clave, lanzamos un error claro en lugar de fallar silenciosamente.
        cfg.setJdbcUrl(requerido(props, "db.url"));
        cfg.setUsername(requerido(props, "db.user"));
        cfg.setPassword(requerido(props, "db.password"));

        // Tamaño del pool: para una app de escritorio de uso personal, 5 conexiones es más que suficiente.
        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(1);
        cfg.setPoolName("gastos-pool");

        ds = new HikariDataSource(cfg);
    }

    /**
     * Intenta cargar el archivo config.properties desde tres ubicaciones, en orden:
     *   1. Directorio de trabajo actual (donde se lanza la app)
     *   2. Carpeta del usuario: ~/.gastos/config.properties
     *   3. Como recurso interno del classpath (útil durante desarrollo en IntelliJ)
     *
     * La prioridad 1 y 2 son para producción/distribución.
     * La prioridad 3 es para desarrollo local — colocá el archivo en src/main/resources/ar/com/gastos/
     * y agregalo al .gitignore para que no suba al repo.
     */
    private static Properties cargarConfiguracion() {
        Properties props = new Properties();

        // Opción 1: directorio de trabajo (donde se ejecuta el .jar)
        Path localPath = Paths.get("config.properties");
        if (Files.exists(localPath)) {
            try (InputStream in = Files.newInputStream(localPath)) {
                props.load(in);
                return props;
            } catch (IOException e) {
                throw new RuntimeException("Error al leer config.properties en directorio de trabajo", e);
            }
        }

        // Opción 2: carpeta del usuario ~/.gastos/config.properties
        Path userPath = Paths.get(System.getProperty("user.home"), ".gastos", "config.properties");
        if (Files.exists(userPath)) {
            try (InputStream in = Files.newInputStream(userPath)) {
                props.load(in);
                return props;
            } catch (IOException e) {
                throw new RuntimeException("Error al leer config.properties en ~/.gastos/", e);
            }
        }

        // Opción 3: classpath (src/main/resources — solo para desarrollo, NO subir al repo)
        try (InputStream in = Db.class.getResourceAsStream("/ar/com/gastos/config.properties")) {
            if (in != null) {
                props.load(in);
                return props;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al leer config.properties desde classpath", e);
        }

        // Si no se encontró en ningún lado, explicamos claramente qué hacer
        throw new RuntimeException(
              "No se encontró config.properties.\n" +
                    "Creá el archivo con el siguiente contenido y colocalo en una de estas ubicaciones:\n" +
                    "  - Directorio donde ejecutás la app: config.properties\n" +
                    "  - Carpeta del usuario: ~/.gastos/config.properties\n\n" +
                    "Contenido del archivo:\n" +
                    "  db.url=jdbc:postgresql://localhost:5432/gastos\n" +
                    "  db.user=postgres\n" +
                    "  db.password=TU_PASSWORD_ACA\n"
        );
    }

    /**
     * Lee una clave del archivo de configuración.
     * Si la clave no existe o está vacía, lanza un error con un mensaje claro
     * en lugar de pasar un null silencioso a HikariCP.
     */
    private static String requerido(Properties props, String clave) {
        String valor = props.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new RuntimeException(
                  "Falta la clave '" + clave + "' en config.properties"
            );
        }
        return valor.trim();
    }

    public static DataSource getDataSource() {
        return ds;
    }
}
