-- =============================================================
-- SCHEMA - Control de Gastos
-- =============================================================
-- Para levantar la base en una PC nueva:
--
-- 1) Crear la base (si no existe):
--      CREATE DATABASE gastos;
--
-- 2) Ejecutar este script contra esa base:
--      psql -U postgres -d gastos -f schema.sql
--
--    O desde pgAdmin: conectarse al servidor, seleccionar la base
--    "gastos", abrir el Query Tool, pegar todo este archivo y
--    ejecutar (F5).
--
-- El script es idempotente (usa IF NOT EXISTS), por lo que se
-- puede volver a correr sin romper una base ya creada.
-- =============================================================

-- -------------------------------------------------------------
-- TARJETA
-- Representa cada tarjeta de crédito del usuario.
-- habilitado = FALSE equivale a "dada de baja" sin borrar datos.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tarjeta (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(50), -- ej: "Mastercard", "Visa", "Naranja"
    habilitado BOOLEAN NOT NULL DEFAULT TRUE
);

-- -------------------------------------------------------------
-- COMERCIO
-- Representa cada comercio ingresado. Ingresado para cargar los ComboBox.
-- habilitado = FALSE equivale a "dada de baja" sin borrar datos.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comercio (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    categoria  VARCHAR(50),
    habilitado BOOLEAN NOT NULL DEFAULT TRUE
);

-- -------------------------------------------------------------
-- MOVIMIENTO
-- Representa cada MOVIMIENTO en la tarjeta, equivale a una compra.
--
-- pagado     -> marca si el movimiento ya fue abonado (usado por el
--               botón "Pagar" condicional del dashboard).
-- comentario -> nota libre opcional sobre el movimiento.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movimiento (
    id SERIAL PRIMARY KEY,
    tarjeta_id INTEGER NOT NULL REFERENCES tarjeta(id),
    comercio_id INTEGER REFERENCES comercio(id),
    fecha DATE NOT NULL,
    descripcion  VARCHAR(255),
    monto NUMERIC(12, 2) NOT NULL,
    categoria VARCHAR(20) NOT NULL,
    moneda VARCHAR(10) NOT NULL DEFAULT 'ARS',
    cuotas INTEGER NOT NULL DEFAULT 1,
    pagado BOOLEAN NOT NULL DEFAULT FALSE,
    comentario VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_movimiento_tarjeta_fecha
    ON movimiento(tarjeta_id, fecha);

-- -------------------------------------------------------------
-- CUOTA
-- fecha_vencimiento -> fecha límite de pago de esa cuota puntual.
-- Se completa desde el DAO de cuotas (no forma parte del modelo
-- Cuota.java, pero es un campo NOT NULL vigente en la base).
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cuota (
    id SERIAL PRIMARY KEY,
    movimiento_id INTEGER NOT NULL REFERENCES movimiento(id),
    nro_cuota INTEGER NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    monto NUMERIC(12, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente'
);

CREATE INDEX IF NOT EXISTS idx_cuota_movimiento ON cuota(movimiento_id);

-- -------------------------------------------------------------
-- INGRESO
-- Ingresos del hogar: sueldos, extras, etc.
-- Son independientes de las tarjetas.
-- tipo: etiqueta libre, ej: "Sueldo Erich", "Sueldo Lorena", "Extra"
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ingreso (
    id SERIAL PRIMARY KEY,
    fecha DATE NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    monto NUMERIC(12, 2) NOT NULL,
    moneda VARCHAR(10) NOT NULL DEFAULT 'ARS'
);

-- -------------------------------------------------------------
-- CIERRES_TARJETA
-- Registra las fechas de cierre y vencimiento de cada tarjeta por mes.
-- El dashboard usa esto para calcular qué movimientos corresponden
-- al período de cada resumen mensual.
--
-- Columnas:
--   id_cierre         → PK autoincremental
--   id                → FK a tarjeta (nombre histórico, equivale a tarjeta_id)
--   mes               → primer día del mes del resumen (ej: 2026-05-01)
--   fecha_cierre      → fecha en que cierra el resumen (ej: 2026-05-21)
--   fecha_vencimiento → fecha límite de pago (ej: 2026-06-04)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cierres_tarjeta (
    id_cierre SERIAL PRIMARY KEY,
    id INTEGER NOT NULL REFERENCES tarjeta(id),
    mes DATE NOT NULL,
    fecha_cierre DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    CONSTRAINT uq_cierre_tarjeta_mes UNIQUE (id, mes)
);

CREATE INDEX IF NOT EXISTS idx_cierres_tarjeta_id
    ON cierres_tarjeta(id, fecha_cierre);

-- -------------------------------------------------------------
-- GASTOS_RECURRENTES
-- Plantilla de gasto recurrente. No tiene monto fijo — se ingresa
-- al momento de generar el egreso del mes (pantalla "Generar mes").
-- medio_pago: 'DEBITO', 'CREDITO' o 'EFECTIVO'
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gastos_recurrentes (
    id SERIAL PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL,
    categoria   VARCHAR(50),
    medio_pago  VARCHAR(20) NOT NULL
);
