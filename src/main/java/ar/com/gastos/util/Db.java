package ar.com.gastos.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Pool de conexiones a PostgreSQL (HikariCP).
 * La configuración se lee desde config.properties a través de Config,
 * que es la misma fuente que usa BackupService.
 */
public class Db {

  private static final HikariDataSource ds;

  static {
    HikariConfig cfg = new HikariConfig();

    // Config.requerido falla con un mensaje claro si falta alguna clave,
    // en lugar de pasar un null silencioso a HikariCP.
    cfg.setJdbcUrl(Config.requerido("db.url"));
    cfg.setUsername(Config.requerido("db.user"));
    cfg.setPassword(Config.requerido("db.password"));

    // Tamaño del pool: para una app de escritorio de uso personal, 5 conexiones es más que suficiente.
    cfg.setMaximumPoolSize(5);
    cfg.setMinimumIdle(1);
    cfg.setPoolName("gastos-pool");

    ds = new HikariDataSource(cfg);
  }

  public static DataSource getDataSource() {
    return ds;
  }
}