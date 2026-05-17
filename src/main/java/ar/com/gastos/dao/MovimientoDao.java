package ar.com.gastos.dao;

import ar.com.gastos.model.Movimiento;
import ar.com.gastos.model.Cuota;
import ar.com.gastos.util.Db;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MovimientoDao {

    public void save(Movimiento m) throws SQLException {
        String sql = "INSERT INTO movimiento(tarjeta_id, fecha, descripcion, monto, categoria, moneda, cuotas) " +
              "VALUES (?,?,?,?,?,?,?) RETURNING id";

        try (Connection c = Db.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, m.getTarjetaId());
            ps.setDate(2, Date.valueOf(m.getFecha()));
            ps.setString(3, m.getDescripcion());
            ps.setBigDecimal(4, m.getMonto());
            ps.setString(5, m.getCategoria());
            ps.setString(6, m.getMoneda());
            ps.setInt(7, m.getCuotas());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int movimientoId = rs.getInt("id");

                // Generar cuotas si el movimiento tiene más de una
                if (m.getCuotas() > 1) {
                    generarCuotas(c, movimientoId, m);
                }
            }
        }
    }

    /**
     * Genera las cuotas de un movimiento y las inserta en la tabla cuota.
     *
     * BUG CORREGIDO: antes se usaba double para dividir el monto:
     *   double montoPorCuota = m.getMonto().doubleValue() / m.getCuotas();
     *
     * El problema es que double es punto flotante binario y no puede representar
     * exactamente todos los decimales. Por ejemplo:
     *   $100.00 / 3 con double  → 33.333333333333336  (residuo fantasma)
     *   $100.00 / 3 con BigDecimal → 33.33 + 33.33 + 33.34  (correcto)
     *
     * Para dinero SIEMPRE se usa BigDecimal con escala y RoundingMode explícitos.
     *
     * MEJORA ADICIONAL: el residuo de centavos se suma a la última cuota,
     * garantizando que cuota1 + cuota2 + ... + cuotaN == monto total exacto.
     * Ejemplo: $100.00 / 3 → cuota1=33.33, cuota2=33.33, cuota3=33.34
     */
    private void generarCuotas(Connection c, int movimientoId, Movimiento m) throws SQLException {
        String sqlCuota = "INSERT INTO cuota(movimiento_id, nro_cuota, fecha_vencimiento, monto, estado) " +
              "VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = c.prepareStatement(sqlCuota)) {

            int cantidadCuotas = m.getCuotas();

            // Dividimos con HALF_UP (redondeo estándar bancario) y 2 decimales
            BigDecimal montoPorCuota = m.getMonto()
                  .divide(BigDecimal.valueOf(cantidadCuotas), 2, RoundingMode.HALF_UP);

            // Calculamos el residuo: diferencia entre el total original y la suma de todas las cuotas.
            // Ejemplo: $100.00 / 3 = 33.33 * 3 = 99.99 → residuo = 0.01
            BigDecimal sumaCalculada = montoPorCuota.multiply(BigDecimal.valueOf(cantidadCuotas));
            BigDecimal residuo = m.getMonto().subtract(sumaCalculada);

            LocalDate fechaBase = m.getFecha();

            for (int i = 1; i <= cantidadCuotas; i++) {
                // El monto de la última cuota absorbe el residuo de centavos
                // para que la suma exacta de todas las cuotas sea igual al monto original.
                BigDecimal montoEstaCuota = (i == cantidadCuotas)
                      ? montoPorCuota.add(residuo)
                      : montoPorCuota;

                ps.setInt(1, movimientoId);
                ps.setInt(2, i);
                // Cada cuota vence un mes después de la anterior
                ps.setDate(3, Date.valueOf(fechaBase.plusMonths(i - 1)));
                ps.setBigDecimal(4, montoEstaCuota);
                ps.setString(5, "pendiente");
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    public List<Movimiento> findByTarjeta(int tarjetaId) throws SQLException {
        List<Movimiento> list = new ArrayList<>();
        String sql = "SELECT * FROM movimiento WHERE tarjeta_id = ? ORDER BY fecha ASC";

        try (Connection c = Db.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, tarjetaId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Movimiento(
                      rs.getInt("id"),
                      rs.getInt("tarjeta_id"),
                      rs.getDate("fecha").toLocalDate(),
                      rs.getString("descripcion"),
                      rs.getBigDecimal("monto"),
                      rs.getString("categoria"),
                      rs.getString("moneda"),
                      rs.getInt("cuotas")
                ));
            }
        }
        return list;
    }

    public List<Movimiento> findByTarjetaEnRango(int tarjetaId, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT * FROM movimiento WHERE tarjeta_id = ? AND fecha BETWEEN ? AND ?";
        List<Movimiento> lista = new ArrayList<>();

        try (Connection conn = Db.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tarjetaId);
            ps.setDate(2, Date.valueOf(desde));
            ps.setDate(3, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new Movimiento(
                      rs.getInt("id"),
                      rs.getInt("tarjeta_id"),
                      rs.getDate("fecha").toLocalDate(),
                      rs.getString("descripcion"),
                      rs.getBigDecimal("monto"),
                      rs.getString("categoria"),
                      rs.getString("moneda"),
                      rs.getInt("cuotas")
                ));
            }
        }
        return lista;
    }
}