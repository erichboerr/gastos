# Plan de mejora: Resumen anual

## Objetivo

Convertir el resumen anual en una vista financiera completa del hogar. Debe permitir entender cuánto ingresó, cuánto se gastó, cuánto se pagó de tarjetas y qué deuda quedó pendiente, sin mezclar conceptos.

El resumen debe distinguir entre:

- Gasto realizado en débito o efectivo.
- Consumo realizado con tarjeta de crédito.
- Pago de deuda de tarjeta.
- Deuda pendiente.
- Ingresos del mes.

## Estado actual

La pantalla tiene dos pestañas:

1. Resumen anual por tarjeta.
2. Resumen por categoría.

El cálculo actual depende de que exista un cierre registrado para cada mes. Si falta un cierre, algunos movimientos pueden aparecer como cero aunque existan en la base.

El resumen por categoría excluye débito cuando se seleccionan todas las tarjetas y utiliza un `JOIN` con comercio que puede dejar afuera movimientos sin comercio.

## Prioridad 1: corregir la base del cálculo

### 1. Definir el criterio mensual

Para débito y efectivo:

- El período siempre es del día 1 al último día del mes.
- Debe respetar el año seleccionado.
- No debe depender de que exista un cierre de tarjeta.

Para crédito:

- El período debe utilizar los cierres configurados para la tarjeta.
- El consumo debe asignarse al resumen correspondiente.
- Las cuotas deben computarse según el período de cada cuota.
- Si falta un cierre, debe mostrarse una advertencia y no presentarse como cero sin explicación.

### 2. Separar gasto, pago y deuda

El sistema debe manejar estas métricas por separado:

- `Ingresos`: suma de la tabla `ingreso` por fecha calendario.
- `Débito/Efectivo`: egresos de esas tarjetas dentro del mes calendario.
- `Consumo crédito`: consumos asignados al período de cierre de cada tarjeta.
- `Pago crédito`: movimientos de categoría `PAGO` realizados durante el mes.
- `Deuda crédito`: deuda acumulada pendiente.
- `Saldo mensual`: ingresos menos gastos de débito/efectivo menos pagos de crédito.

El pago de una tarjeta no debe contarse nuevamente como consumo del mes. Es el pago de una deuda anterior.

### 3. Evitar que los datos faltantes parezcan cero

Cuando un crédito no tenga cierre para un mes, la pantalla debe mostrar una indicación como:

```text
Sin cierre configurado
```

El sistema no debe ocultar la falta de configuración convirtiéndola silenciosamente en un importe cero.

## Prioridad 2: nuevo resumen mensual

La primera pestaña debería mostrar una fila por mes con estas columnas:

| Mes | Ingresos | Débito/Efectivo | Consumo crédito | Pagos crédito | Total egresos | Saldo |
|---|---:|---:|---:|---:|---:|---:|

### Reglas

- `Total egresos` debe aclarar si incluye consumo de crédito o solamente movimientos pagados.
- Se recomienda mostrar dos valores separados:
  - `Egresos del mes`: débito/efectivo más pagos de crédito.
  - `Consumo del mes`: débito/efectivo más consumo de crédito.
- `Saldo disponible` debe calcularse con los egresos efectivamente pagados.
- La deuda de crédito debe aparecer separada del saldo disponible.

### Totales anuales

Debajo de la tabla deben mostrarse:

- Total de ingresos.
- Total de débito/efectivo.
- Total consumido con crédito.
- Total pagado a tarjetas.
- Deuda final.
- Saldo anual.

## Prioridad 3: mejorar el resumen por categoría

### Filtros

Agregar filtros por:

- Todas las formas de pago.
- Débito/efectivo.
- Crédito.
- Tarjeta específica.
- Mes específico.
- Año completo.

### Correcciones de datos

La consulta debe usar `LEFT JOIN comercio` para no perder movimientos sin comercio.

Debe incluir categorías como:

- Categoría registrada.
- `Sin categoría`.
- `Sin comercio`, cuando corresponda.

### Información adicional

Mostrar por categoría:

- Total.
- Porcentaje sobre el total filtrado.
- Cantidad de movimientos.
- Promedio por movimiento.
- Variación respecto del año anterior, cuando exista información.

## Prioridad 4: gráficos e indicadores

Agregar gráficos después de corregir los cálculos.

### Gráfico mensual

Mostrar por mes:

- Ingresos.
- Egresos pagados.
- Consumo con crédito.
- Saldo.

### Gráfico por categoría

Mostrar las categorías con mayor gasto, ordenadas de mayor a menor.

### Indicadores destacados

Mostrar:

- Mes con mayor gasto.
- Mes con mayor consumo de crédito.
- Categoría con mayor gasto.
- Promedio mensual de egresos.
- Promedio mensual de ingresos.

## Prioridad 5: resumen específico de tarjetas

Agregar una sección por tarjeta de crédito con:

- Consumo anual.
- Total pagado.
- Deuda pendiente.
- Cantidad de cuotas pendientes.
- Próximo cierre.
- Próximo vencimiento.
- Deuda estimada por mes.

Esta sección debe contemplar tarjetas cuyo cierre y vencimiento caen en el mismo mes y tarjetas cuyo vencimiento cae en el mes siguiente. No se debe asumir que todas funcionan igual.

## Diseño técnico recomendado

### Crear un servicio de resumen

El cálculo no debería quedar concentrado en `ResumenAnualController`.

Crear una capa similar a:

```text
service/ResumenAnualService.java
model/ResumenMensual.java
model/ResumenCategoria.java
model/ResumenTarjeta.java
```

El controlador debería encargarse de:

- Leer filtros.
- Invocar al servicio.
- Mostrar los resultados.

El servicio debería encargarse de:

- Calcular períodos.
- Consultar ingresos y movimientos.
- Procesar cuotas.
- Calcular deuda y saldos.
- Devolver resultados listos para la pantalla.

### Mejorar las consultas

Evitar consultar la base repetidamente dentro de cada mes y tarjeta. Se recomienda:

1. Obtener los ingresos del año en una consulta.
2. Obtener los movimientos del año en una consulta.
3. Obtener las cuotas relacionadas en una consulta.
4. Obtener los cierres del año en una consulta.
5. Agrupar en memoria mediante `YearMonth` y tarjeta.

Esto hará que el resumen sea más rápido y facilitará las pruebas.

## Validaciones y pruebas

Antes de publicar los cambios, probar estos casos:

1. Año con ingresos y gastos de débito.
2. Año con consumos de crédito en cuotas.
3. Año con pagos de tarjetas.
4. Mes sin cierre de crédito.
5. Débito sin cierre configurado.
6. Movimiento sin comercio.
7. Movimiento sin categoría.
8. Tarjeta que cierra y vence en el mismo mes.
9. Tarjeta que cierra en un mes y vence en el siguiente.
10. Año bisiesto.
11. Año sin movimientos.
12. Varias tarjetas de crédito con fechas de cierre diferentes.

Cada prueba debe verificar los importes mostrados y no solamente que la pantalla abra.

## Orden recomendado de implementación

1. Corregir el cálculo de débito y efectivo por mes calendario.
2. Separar consumo, pago y deuda de crédito.
3. Corregir la consulta por categoría y agregar todos los medios de pago.
4. Crear modelos y servicio para sacar la lógica del controlador.
5. Reemplazar la tabla actual por el resumen mensual completo.
6. Agregar filtros por medio de pago, tarjeta y mes.
7. Agregar el resumen específico de tarjetas.
8. Agregar gráficos e indicadores.
9. Crear pruebas con datos representativos de la base real.

## Resultado esperado

Al finalizar, la pantalla debe responder claramente:

- Cuánto dinero ingresó cada mes.
- Cuánto se gastó en débito y efectivo.
- Cuánto se consumió con crédito.
- Cuánto se pagó de tarjetas.
- Cuánto quedó pendiente.
- En qué categorías se concentraron los gastos.
- Qué tarjeta requiere atención y cuándo vence.

La prioridad es que los números sean correctos y explicables. Los gráficos deben agregarse después de estabilizar los cálculos.
