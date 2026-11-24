package ar.com.gastos.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Properties;

public class Db {

  private static final HikariDataSource ds;

  static {
    // Carga de configuracion centralizada en ConfigLoader (compartida con BackupService).
    // Busca config.properties junto al .jar -> directorio de trabajo -> ~/.gastos -> classpath.
    // Ver ConfigLoader.java para el detalle de cada opcion.
    Properties props = ConfigLoader.cargarConfiguracion();

    HikariConfig cfg = new HikariConfig();

    // Leemos cada valor desde el archivo de configuracion.
    // Si falta alguna clave, lanzamos un error claro en lugar de fallar silenciosamente.
    cfg.setJdbcUrl(ConfigLoader.requerido(props, "db.url"));
    cfg.setUsername(ConfigLoader.requerido(props, "db.user"));
    cfg.setPassword(ConfigLoader.requerido(props, "db.password"));

    // Tamano del pool: para una app de escritorio de uso personal, 5 conexiones es mas que suficiente.
    cfg.setMaximumPoolSize(5);
    cfg.setMinimumIdle(1);
    cfg.setPoolName("gastos-pool");

    ds = new HikariDataSource(cfg);
  }

  public static DataSource getDataSource() {
    return ds;
  }
}
