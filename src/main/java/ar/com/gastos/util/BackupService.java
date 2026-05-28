package ar.com.gastos.util;

import ar.com.gastos.util.Db;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Servicio de backup de la base de datos usando pg_dump.
 * La carpeta destino se configura en config.properties (backup.dir).
 * Si ya existe un backup del mismo día lo reemplaza.
 */
public class BackupService {

  private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /**
   * Ejecuta pg_dump y guarda el resultado en la carpeta configurada.
   * Retorna el path del archivo generado o lanza excepción si falla.
   */
  public static String ejecutarBackup() throws Exception {

    // Leemos la configuración
    Properties props = cargarProperties();
    String backupDir = props.getProperty("backup.dir");
    String dbUrl     = props.getProperty("db.url");
    String dbUser    = props.getProperty("db.user");
    String dbPass    = props.getProperty("db.password");

    if (backupDir == null || backupDir.isBlank()) {
      throw new Exception("No está configurada la carpeta de backup (backup.dir) en config.properties");
    }

    // Extraemos el nombre de la DB de la URL — ej: jdbc:postgresql://localhost:5432/gastos → gastos
    String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);

    // Creamos la carpeta si no existe
    Path dirPath = Paths.get(backupDir);
    if (!Files.exists(dirPath)) {
      Files.createDirectories(dirPath);
      logger.info("Carpeta de backup creada: {}", backupDir);
    }

    // Nombre del archivo — un backup por día, se reemplaza si ya existe
    String fecha    = LocalDate.now().format(DATE_FMT);
    String fileName = "gastos_backup_" + fecha + ".sql";
    String filePath = backupDir + File.separator + fileName;

    // Armamos el comando pg_dump
    ProcessBuilder pb = new ProcessBuilder(
        "pg_dump",
        "-U", dbUser,
        "-h", "localhost",
        "-d", dbName,
        "-f", filePath,
        "--no-password"
    );

    // Pasamos la contraseña via variable de entorno para no exponerla en el proceso
    pb.environment().put("PGPASSWORD", dbPass);
    pb.redirectErrorStream(true);

    logger.info("Ejecutando backup: {}", fileName);
    Process process = pb.start();

    // Capturamos la salida por si hay errores
    String output = new String(process.getInputStream().readAllBytes());
    int exitCode  = process.waitFor();

    if (exitCode != 0) {
      logger.error("pg_dump falló con código {}: {}", exitCode, output);
      throw new Exception("pg_dump falló: " + output);
    }

    logger.info("Backup completado: {}", filePath);
    return filePath;
  }

  // --- Lee el config.properties desde el classpath ---

  private static Properties cargarProperties() throws Exception {
    Properties props = new Properties();
    try (InputStream is = BackupService.class.getResourceAsStream(
        "/ar/com/gastos/config.properties")) {
      if (is == null) throw new Exception("No se encontró config.properties");
      props.load(is);
    }
    return props;
  }
}