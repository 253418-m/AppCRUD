# Recetario

Aplicación Android académica de la Unidad 3. Etapa 1: base Compose y pantalla temporal con Navigation Compose. No implementa todavía autenticación, backend ni CRUD.

## Entorno
Un módulo `app`, Kotlin integrado en AGP 9.5.1, plugin Compose 2.2.10, BOM Compose 2026.02.01, Navigation Compose 2.10.2, Gradle Wrapper 9.6.0. SDK mínimo 26; compile/target 37. Java del terminal 21; el daemon Gradle solicita toolchain JDK 25 mediante gradle/gradle-daemon-jvm.properties.

## Ejecutar
Abrir en Android Studio, sincronizar Gradle y ejecutar app. Configurar el SDK en local.properties (no versionado).
Verificación: `./gradlew.bat assembleDebug testDebugUnitTest`.

## Organización
- presentation: navegación y pantalla temporal.
- core: configuración compartida, red, sesión y contenedor en etapas posteriores.
- domain: modelos y contratos en etapas posteriores.
- data: servicios, DTOs, mappers y repositorios en etapas posteriores.

No se crean clases vacías ni archivos de infraestructura futura. Se conservan los recursos válidos de la plantilla. Las rutas físicas heredadas de la plantilla no determinan el namespace de Kotlin.

## Alcance y pendientes
Los PDF oficiales no se encontraron en el workspace; se usan los requisitos transcritos por el usuario.
Ningún criterio de la rúbrica está declarado LOGRADO. El test aritmético heredado solo comprueba la infraestructura JUnit.
Antes de implementar el dominio se requiere verificar su registro con el docente.
La etapa 2 requiere revisión del informe y autorización explícita.
