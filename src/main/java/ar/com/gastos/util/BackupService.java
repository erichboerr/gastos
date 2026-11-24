package ar.com.gastos.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

/**
 * Servicio de backup de la base de datos usando pg_dump.
 * La carpeta destino se configura en config.properties (backup.dir), y se resuelve
 * con ConfigLoader.resolverBackupDir(): si es una ruta relativa, queda al lado del
 * .jar (no del directorio de trabajo), asi que viaja junto con la app al copiarla
 * a otra PC sin quedar atada a una unidad fija ni a un usuario.
 * Si ya existe un backup del mismo dia lo reemplaza.
 */
public class BackupService {

  private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Ejecuta pg_dump y guarda el resultado en la carpeta configurada.
   * Retorna el path del archivo generado o lanza excepcion si falla.
   */
  public static String ejecutarBackup() throws Exception {

    // Antes: BackupService leia su propia copia de config.properties SOLO desde el classpath
    // (el archivo empaquetado dentro del .jar al compilar), completamente distinta de la logica
    // de Db.java. Ahora ambos usan ConfigLoader, con el mismo orden de busqueda.
    Properties props = ConfigLoader.cargarConfiguracion();
    String dbUrl = ConfigLoader.requerido(props, "db.url");
    String dbUser = ConfigLoader.requerido(props, "db.user");
    String dbPass = ConfigLoader.requerido(props, "db.password");

    // Extraemos el nombre de la DB de la URL - ej: jdbc:postgresql://localhost:5432/gastos -> gastos
    String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);

    // Resuelve la carpeta de backups: si backup.dir es relativo (o no esta definido),
    // queda al lado del .jar; si es absoluto, se respeta tal cual.
    Path dirPath = ConfigLoader.resolverBackupDir(props);
    if (!Files.exists(dirPath)) {
      Files.createDirectories(dirPath);
      logger.info("Carpeta de backup creada: {}", dirPath);
    }

    // Nombre del archivo - un backup por dia, se reemplaza si ya existe
    String fecha = LocalDate.now().format(DATE_FMT);
    String fileName = "gastos_backup_" + fecha + ".sql";
    String filePath = dirPath.resolve(fileName).toString();

    // Armamos el comando pg_dump
    ProcessBuilder pb = new ProcessBuilder(
        "pg_dump",
        "-U", dbUser,
        "-h", "localhost",
        "-d", dbName,
        "-f", filePath,
        "--no-password",
        "--clean",        // incluye DROP antes de cada CREATE en el .sql
        "--if-exists"     // no falla si el objeto no existe al borrar
    );

    // Pasamos la contrasena via variable de entorno para no exponerla en el proceso
    pb.environment().put("PGPASSWORD", dbPass);
    pb.redirectErrorStream(true);

    logger.info("Ejecutando backup: {}", fileName);
    Process process;
    try {
      process = pb.start();
    } catch (IOException e) {
      // Este es el error tipico cuando pg_dump no esta en el PATH de la PC nueva.
      throw new Exception(
          "No se pudo ejecutar pg_dump. Verifica que la carpeta bin de PostgreSQL "
              + "este agregada al PATH del sistema en esta PC. Detalle: " + e.getMessage(), e);
    }

    // Capturamos la salida por si hay errores
    String output = new String(process.getInputStream().readAllBytes());
    int exitCode = process.waitFor();

    if (exitCode != 0) {
      logger.error("pg_dump fallo con codigo {}: {}", exitCode, output);
      throw new Exception("pg_dump fallo: " + output);
    }

    logger.info("Backup completado: {}", filePath);
    return filePath;
  }

  /**
   * Restaura la base de datos desde un archivo .sql de backup.
   * ADVERTENCIA: borra todos los datos actuales antes de restaurar.
   * Usa psql para ejecutar el script SQL.
   */
  public static void restaurarBackup(String filePath) throws Exception {

    Properties props = ConfigLoader.cargarConfiguracion();
    String dbUrl = ConfigLoader.requerido(props, "db.url");
    String dbUser = ConfigLoader.requerido(props, "db.user");
    String dbPass = ConfigLoader.requerido(props, "db.password");
    String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);

    // Verificamos que el archivo existe
    if (!Files.exists(Paths.get(filePath))) {
      throw new Exception("El archivo de backup no existe: " + filePath);
    }

    // Usamos --clean/--if-exists (ya incluido en el .sql generado) para limpiar antes de recrear
    ProcessBuilder pb = new ProcessBuilder(
        "psql",
        "-U", dbUser,
        "-h", "localhost",
        "-d", dbName,
        "-f", filePath,
        "-v", "ON_ERROR_STOP=1"
    );

    pb.environment().put("PGPASSWORD", dbPass);
    pb.redirectErrorStream(true);

    logger.info("Restaurando backup: {}", filePath);
    Process process;
    try {
      process = pb.start();
    } catch (IOException e) {
      throw new Exception(
          "No se pudo ejecutar psql. Verifica que la carpeta bin de PostgreSQL "
              + "este agregada al PATH del sistema en esta PC. Detalle: " + e.getMessage(), e);
    }

    String output = new String(process.getInputStream().readAllBytes());
    int exitCode = process.waitFor();

    if (exitCode != 0) {
      logger.error("psql fallo con codigo {}: {}", exitCode, output);
      throw new Exception("Error al restaurar: " + output);
    }

    logger.info("Restauracion completada desde: {}", filePath);
  }

  /**
   * Lista todos los archivos .sql de backup en la carpeta configurada,
   * ordenados por fecha descendente (el mas reciente primero).
   */
  public static List<File> listarBackups() throws Exception {
    Properties props = ConfigLoader.cargarConfiguracion();
    Path dirPath = ConfigLoader.resolverBackupDir(props);

    if (!Files.exists(dirPath)) return new java.util.ArrayList<>();

    return Files.list(dirPath)
        .filter(p -> p.toString().endsWith(".sql"))
        .map(Path::toFile)
        .sorted((a, b) -> b.getName().compareTo(a.getName())) // mas reciente primero
        .collect(java.util.stream.Collectors.toList());
  }
}
