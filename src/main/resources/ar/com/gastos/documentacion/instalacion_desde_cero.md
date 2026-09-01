# Instalación del proyecto "gastos" desde cero

Guía para reinstalar todo el entorno cuando se clona el repositorio en una PC nueva (o se reinstala Windows).

---

## 1. Requisitos previos

- **JDK 21** (IntelliJ puede descargarlo solo desde `File → Project Structure → SDK → Download JDK`)
- **PostgreSQL** instalado (incluye `psql` y `pgAdmin`)
- **JavaFX SDK 21.0.6** (tipo **SDK**, no *jmods*) — descargar de https://gluonhq.com/products/javafx/
  - OS: Windows | Arquitectura: x64 | Type: **SDK**
  - Descomprimir en: `C:\javafx-sdk-21.0.6\`
  - Verificar que exista `C:\javafx-sdk-21.0.6\lib\javafx.web.jar` (los `.dll` van en `bin`, los `.jar` van en `lib`)

---

## 2. Clonar y abrir en IntelliJ

1. `git clone <url-del-repo>`
2. Abrir la carpeta en IntelliJ IDEA
3. Verificar SDK del proyecto: `File → Project Structure → Project → SDK` → JDK 21
4. Esperar a que sincronice Gradle solo, o forzarlo desde la pestaña **Gradle** (ícono elefante) → 🔄 *Reload All Gradle Projects*

---

## 3. Restaurar la base de datos

1. Crear la base vacía:
   ```
   createdb -U postgres gastos
   ```
   (o desde pgAdmin: click derecho en `Databases → Create → Database...`, name `gastos`, owner `postgres`)

2. Restaurar el backup más reciente (`.sql`) con `psql`:
   ```
   psql -U postgres -d gastos -f "C:\ruta\al\backup.sql"
   ```

   > Si `psql` no se reconoce como comando, agregar `C:\Program Files\PostgreSQL\<version>\bin` al PATH del sistema (Variables de entorno → Variables del sistema → `Path` → Nuevo), y abrir una consola nueva.

3. Verificar en pgAdmin que las tablas y datos estén cargados (`gastos → Schemas → public → Tables`).

---

## 4. Crear `config.properties`

Este archivo **no está en el repo** (por seguridad, está en `.gitignore`). Hay que crearlo manualmente en:

```
src/main/resources/ar/com/gastos/config.properties
```

Contenido:
```properties
db.url=jdbc:postgresql://localhost:5432/gastos
db.user=postgres
db.password=TU_PASSWORD_ACA
backup.dir=C:/Db_Backups/gastos
```

> Notas:
> - `db.url` / `db.user` / `db.password` los usa `Db.java` (conexión principal).
> - `backup.dir` lo usa `BackupService.java` para saber dónde guardar los backups. Usar `/` en la ruta, no `\`.
> - `Db.java` también acepta este archivo en el directorio de trabajo (`config.properties` junto al `.jar`)
> - o en `~/.gastos/config.properties`, pero `BackupService.java` **solo** lo lee desde `src/main/resources` (classpath).

---

## 5. Probar en modo desarrollo (dentro de IntelliJ)

Desde la terminal integrada:
```
.\gradlew run
```
o desde la pestaña Gradle: `gastos → Tasks → application → run`.

Si tira error de `config.properties` o de módulos JavaFX, revisar los pasos 1 y 4.

---

## 6. Compilar el ejecutable

```
.\gradlew shadowJar
```

Genera:
```
build\libs\gastos-1.0-SNAPSHOT-all.jar
```

> ⚠️ Como `config.properties` vive en `src/main/resources`, queda empaquetado dentro de este jar (incluida la password). Para uso personal no es grave, pero tenerlo en cuenta si se comparte el `.jar`.

---

## 7. Lanzador `gastos.vbs`

Crear (o verificar que exista) `gastos.vbs` en la carpeta del proyecto, con:

```vbs
Set objShell = CreateObject("Wscript.Shell")
objShell.Run "java --module-path ""C:\javafx-sdk-21.0.6\lib"" --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.swing -jar ""C:\Users\Erich Desarrollo\Documents\Proyectos_JAVA\gastos\build\libs\gastos-1.0-SNAPSHOT-all.jar""", 0, True
```

Doble click en `gastos.vbs` para abrir la app sin ventana de consola. Opcional: crear acceso directo en el escritorio apuntando a este archivo, con ícono propio.

---

## Flujo resumido (para reinstalar todo de nuevo)

1. Instalar JDK 21, PostgreSQL, JavaFX SDK 21.0.6
2. `git clone` + abrir en IntelliJ + sync Gradle
3. `createdb` + `psql -f backup.sql`
4. Crear `config.properties` (db + backup.dir)
5. `.\gradlew shadowJar`
6. Doble click en `gastos.vbs`

## Flujo resumido (para actualizar después de cambios de código)

1. `.\gradlew shadowJar`
2. Doble click en `gastos.vbs`