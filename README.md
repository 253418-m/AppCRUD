# Ricetta

Aplicación Android de la Unidad 3 de Desarrollo de Aplicaciones Móviles. Dominio académico: Recetario. Recurso principal: Receta. Objetivo final: autenticación y CRUD contra una API REST, con MVVM y Clean Architecture ligera.

## Avance por etapas
Etapa 2: normalización de identidad. Paquete principal y applicationId: `com.upchiapas.ricetta`. Un módulo `app`, MainActivity, Navigation Compose y pantalla temporal. Tema Material 3 conservado. En esa etapa se conservó la pantalla temporal y aún estaban pendientes los mockups Figma y las pantallas funcionales.

Etapa 3: backend REST real en `backend/`, con FastAPI, SQLite, SQLAlchemy 2, registro, login JWT HS256 y CRUD de recetas por usuario. Consulte [instalación, contrato HTTP, pruebas y compatibilidad Retrofit](backend/README.md). El módulo Android no se modifica en esta etapa.

Etapa 4: capa Android de dominio/datos, Retrofit 3, DTOs/mappers, repositorios, sesión persistente, interceptor Bearer e inyección manual. La interfaz permaneció temporal durante esa etapa. Consulte [arquitectura, BASE_URL y pruebas Android](docs/android-data.md).

Etapa 5: Login y Registro Compose basados en los SVG locales, ViewModels/UiState/StateFlow, factories manuales, restauración de sesión, navegación protegida y confirmación de logout. Aprobaron las pruebas instrumentadas, la comparación visual manual, la restauración de sesión tras reiniciar el proceso y la compilación/lint final. Consulte [autenticación, mockups y evidencias](docs/authentication.md). No se implementa todavía CRUD visual.

## Tecnologías y ejecución
Kotlin integrado en AGP 9.5.1; plugin Compose 2.2.10; BOM 2026.02.01; Navigation Compose 2.10.2; Gradle Wrapper 9.6.0. minSdk 26; compile/target 37. Java del terminal 21; daemon Gradle configurado para JDK 25.

Abrir en Android Studio, configurar SDK en `local.properties` (excluido de Git), sincronizar y ejecutar `app`.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest --console=plain
.\gradlew.bat lintDebug --console=plain
```

## Organización
- `presentation`: Login/Registro, ViewModels, UiStates, factories, coordinación de sesión, navegación, componentes y tema.
- `core`: red, manejo de errores y AppContainer.
- `domain`: modelos, contratos de repositorios/sesión y resultados tipados.
- `data`: servicios Retrofit, DTOs, mappers, sesión y repositorios concretos.

Se conservan las pruebas iniciales y se añaden pruebas de contrato, repositorios, sesión y autenticación MVVM. En la etapa 5 aprobaron 67 pruebas JVM y 14 instrumentadas (0 fallidas y 0 omitidas), incluyendo una prueba de UI contra FastAPI real con SQLite temporal. También aprobaron sincronización, compilación debug/APK de pruebas y lint. La revisión manual de MainActivity verificó sesión persistente al reiniciar el proceso, cancelación y confirmación de logout, nuevo inicio sin sesión y botón Atrás sin volver al destino protegido.

## Limitaciones
PDF oficiales ausentes: se usan los requisitos transcritos. Ningún criterio académico se declara LOGRADO. Verificación de etapa 2: sincronización, assembleDebug, testDebugUnitTest y lintDebug aprobados. AVD Pixel_10_Pro_XL: `unauthorized` se resolvió reiniciando el servidor con `adb kill-server` y `adb start-server`. Instalación, apertura y pantalla temporal verificadas en vertical y horizontal, sin cierre observado. No se borraron datos del AVD.

El registro previo del daemon Kotlin señala conflicto de caché incremental (`lookups.tab` ya registrado), sin evidencia suficiente para atribuirlo a memoria o JDK. No se reprodujo durante esta etapa; compilación Kotlin y verificaciones posteriores aprobaron sin modificar versiones ni configuración del daemon.

La API REST y su contrato JSON están implementados en `backend/`. Se mantiene pendiente confirmar el registro del dominio con el docente. La infraestructura de integración Android está implementada en la etapa 4. La autenticación visual y sus ViewModels están implementados y verificados en la etapa 5; las diferencias visuales y límites de la evidencia se detallan en su documentación. La etapa 6, Home y detalle de recetas, requiere revisión y autorización expresa.
