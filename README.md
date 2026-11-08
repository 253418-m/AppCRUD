# Ricetta

Aplicación Android de la Unidad 3 de Desarrollo de Aplicaciones Móviles. Dominio académico: Recetario. Recurso principal: Receta. Objetivo final: autenticación y CRUD contra una API REST, con MVVM y Clean Architecture ligera.

## Base actual
Etapa 2: normalización de identidad. Paquete principal y applicationId: `com.upchiapas.ricetta`. Un módulo `app`, MainActivity, Navigation Compose y pantalla temporal. Tema Material 3 conservado. La base Android conserva su alcance temporal y no integra aún autenticación ni CRUD. Diseño definitivo pendiente de mockups Figma.

Etapa 3: backend REST real en `backend/`, con FastAPI, SQLite, SQLAlchemy 2, registro, login JWT HS256 y CRUD de recetas por usuario. Consulte [instalación, contrato HTTP, pruebas y compatibilidad Retrofit](backend/README.md). El módulo Android no se modifica en esta etapa.

## Tecnologías y ejecución
Kotlin integrado en AGP 9.5.1; plugin Compose 2.2.10; BOM 2026.02.01; Navigation Compose 2.10.2; Gradle Wrapper 9.6.0. minSdk 26; compile/target 37. Java del terminal 21; daemon Gradle configurado para JDK 25.

Abrir en Android Studio, configurar SDK en `local.properties` (excluido de Git), sincronizar y ejecutar `app`.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest --console=plain
.\gradlew.bat lintDebug --console=plain
```

## Organización
- `presentation`: navegación, pantalla temporal y tema.
- `core` (futuro): configuración, red, sesión y contenedor.
- `domain` (futuro): modelos y contratos de repositorios.
- `data` (futuro): servicios, DTOs, mappers y repositorios concretos.

Las capas futuras se documentan sin crear clases ficticias. Los tests existentes son los de la plantilla; el test aritmético no demuestra requisitos funcionales.

## Limitaciones
PDF oficiales ausentes: se usan los requisitos transcritos. Ningún criterio académico se declara LOGRADO. Verificación de etapa 2: sincronización, assembleDebug, testDebugUnitTest y lintDebug aprobados. AVD Pixel_10_Pro_XL: `unauthorized` se resolvió reiniciando el servidor con `adb kill-server` y `adb start-server`. Instalación, apertura y pantalla temporal verificadas en vertical y horizontal, sin cierre observado. No se borraron datos del AVD.

El registro previo del daemon Kotlin señala conflicto de caché incremental (`lookups.tab` ya registrado), sin evidencia suficiente para atribuirlo a memoria o JDK. No se reprodujo durante esta etapa; compilación Kotlin y verificaciones posteriores aprobaron sin modificar versiones ni configuración del daemon.

La API REST y su contrato JSON están implementados en `backend/`. Se mantiene pendiente confirmar el registro del dominio con el docente. La integración Android requiere revisión de la etapa 3 y autorización explícita para la etapa 4; todavía no se implementan Retrofit, repositorios, sesión ni ViewModels.
