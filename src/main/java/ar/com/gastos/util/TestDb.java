package ar.com.gastos.util;

import java.sql.Connection;

public class TestDb {
  public static void main(String[] args) {
    try (Connection c = Db.getDataSource().getConnection()) {
      System.out.println("Conexión OK: " + c.getMetaData().getDatabaseProductName());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
