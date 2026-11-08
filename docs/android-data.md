# Capa de datos Android — etapa 4

Este documento conserva el alcance y la evidencia de la etapa 4. La presentación de autenticación añadida en la etapa 5 reutiliza esta infraestructura; su arquitectura, mockups y verificación se describen en [Autenticación MVVM](authentication.md). En etapa 5 se agregan las dependencias Lifecycle de presentación con la versión 2.6.1 del catálogo y se corrige únicamente Espresso a 3.7.0 por compatibilidad de las pruebas con InputManager. Los resultados de etapa 4 que figuran abajo se conservan como evidencia histórica; la ejecución de etapa 5 registra 67 pruebas JVM y 14 instrumentadas aprobadas, sincronización/compilación/lint finales aprobados y restauración de sesión tras reiniciar el proceso de MainActivity verificada manualmente.

## Alcance e identidad

Paquete `com.upchiapas.ricetta`, un módulo `app`. Se conserva MainActivity, la pantalla temporal, el tema y Navigation Compose. No se implementan ViewModels, pantallas de autenticación/CRUD ni navegación de sesión. El backend de la etapa 3 no se modifica.

## Dependencias

Retrofit y converter oficial kotlinx.serialization 3.0.0; OkHttp/MockWebServer 4.12.0; kotlinx.serialization JSON 1.9.0; coroutines Android/test 1.10.2. Plugin Kotlin Serialization 2.2.10, alineado con el plugin Compose existente. Se conservan AGP 9.5.1, Kotlin, Gradle, Compose, Lifecycle y las demás versiones previas.

Se fijan versiones compatibles con el compilador actual; no se actualiza a las últimas bibliotecas si eso obliga a migrar el stack. JUnit4 y AndroidJUnitRunner existentes siguen siendo los ejecutores de pruebas. No se agrega Hilt, mocking automático, Robolectric ni infraestructura de capturas: no son necesarios para el alcance.

## Responsabilidades

| Paquete | Responsabilidad |
|---|---|
| domain.model | Usuario, Receta, GuardarReceta sin id y enum Dificultad |
| domain.repository | Contratos suspend AuthRepository y RecipeRepository |
| domain.session | SessionStorage y excepción de persistencia |
| domain.result | AppResult.Success/Failure y DataError |
| data.dto | Peticiones y respuestas HTTP @Serializable |
| data.mapper | Conversiones explícitas DTO/dominio |
| data.remote | AuthApi y RecetasApi Retrofit |
| data.repository | Implementaciones, mapeo y persistencia del token |
| data.session | SharedPreferences privado con escrituras en IO |
| core.network | Bearer y traducción centralizada de errores |
| core.di | Construcción y reutilización de dependencias |

Domain no importa Android, Retrofit, OkHttp ni kotlinx.serialization. Las interfaces de repositorio exponen únicamente modelos de dominio y AppResult. No existen casos de uso que solo deleguen ni clases vacías.

Los DTOs requieren todos los campos del contrato. Json se configura con `ignoreUnknownKeys = true`; campos obligatorios ausentes/nulos y respuestas semánticamente inválidas producen errores de serialización, sin rellenar valores ficticios. Los mappers validan id, nombres, cantidades, dificultad, tipo de token y token no vacío. AuthResponseDto se convierte en Usuario; el token no llega al dominio ni a Composables.

## Retornos Retrofit exigidos por el docente

Todas las funciones son `suspend fun x(): T`: AuthResponseDto, List<RecetaDto> o RecetaDto. DELETE usa `suspend fun eliminarReceta(id: Int): Unit` y acepta el 204 sin contenido de FastAPI. No hay `Response<T>`, `Call<T>` ni callbacks en las interfaces API.

Los códigos no exitosos originan HttpException. `apiResult` centraliza su traducción: 401 Unauthorized, 404 NotFound, 409 Conflict, 422 Validation y 5xx Server. Otros códigos son Unknown. SocketTimeoutException e InterruptedIOException se manejan antes de IOException como Timeout; IOException se traduce a NoInternet (conectividad no disponible, sin afirmar un diagnóstico exacto de la red). También se distinguen Serialization, Storage y Unknown.

CancellationException se relanza como la misma instancia. No se usa runCatching que oculte cancelación, GlobalScope o runBlocking en producción. Retrofit suspend ya es main-safe; no se envuelve en Dispatchers.IO. Las escrituras bloqueantes de SharedPreferences sí usan IO.

## Sesión y privacidad

SessionStorage ofrece getToken, hasSession, saveToken y clearToken. `hasSession` consulta en IO si hay token; no valida su expiración remota. `getToken` es síncrono para el hilo de trabajo de OkHttp y no se debe usar para restaurar sesión en el hilo UI.

SharedPreferencesSessionStorage utiliza el archivo privado `ricetta_session.xml`. Las escrituras y borrados usan commit en IO y comprueban su resultado; no se usa la extensión KTX porque oculta ese booleano. Un fallo de persistencia es Storage. No se guardan contraseñas.

Después de registrar/iniciar sesión, AuthRepository valida y mapea la respuesta antes de guardar el token. Un login fallido conserva la sesión anterior. Cerrar sesión elimina el token. La etapa 5 añade coordinación de restauración y limpieza de navegación desde presentación, sin duplicar esta persistencia.

El archivo de sesión se excluye de Auto Backup, cloud-backup y device-transfer. No hay logs HTTP de cuerpos, contraseñas ni tokens. El almacenamiento es SharedPreferences privado según el requisito académico, sin añadir un mecanismo de cifrado distinto.

AuthInterceptor lee SessionStorage y agrega Bearer solo en las rutas de recetas. Login/register son públicos y no reciben tokens, aunque exista sesión. No hay token manual en las firmas Retrofit.

## Inyección manual

RicettaApplication, registrada en Manifest, mantiene un único AppContainer lazy. El contenedor reutiliza un OkHttpClient, un Retrofit, SessionStorage y repositorios; los expone mediante interfaces. La creación diferida evita construir red o cargar preferencias durante la pantalla temporal.

En la etapa 4 se prepararon interfaces para los futuros ViewModels, sin crear factories artificiales. La etapa 5 incorpora LoginViewModel, RegisterViewModel y SessionViewModel por constructor y los crea con `viewModelFactory { initializer { ... } }` mediante AuthViewModelFactories. No hay frameworks automáticos de DI ni acceso global indiscriminado a dependencias.

## BASE_URL y transporte

En debug, BuildConfig.BASE_URL predeterminada es `http://10.0.2.2:8000/`. Las rutas Retrofit son relativas, sin / inicial.

Cambiar la URL por proyecto:

```powershell
.\gradlew.bat assembleDebug -Pricetta.debugBaseUrl=http://10.0.2.2:8000/
```

Debe ser absoluta, sin credenciales/query/fragment y terminar en /. La configuración debug permite HTTP únicamente a 10.0.2.2, 127.0.0.1 y localhost, con includeSubdomains=false. Otros hosts requieren HTTPS o una revisión explícita de la lista debug. INTERNET es un permiso normal y no necesita diálogo runtime.

Release no contiene la excepción debug y mantiene usesCleartextTraffic=false. No existe URL de producción: BASE_URL release queda vacía y el contenedor falla con un mensaje explícito si se intenta usar la API sin configurarla. Una URL release debe ser HTTPS:

```powershell
.\gradlew.bat assembleRelease -Pricetta.releaseBaseUrl=URL_HTTPS_REAL_TERMINADA_EN_BARRA
```

El marcador debe sustituirse por una URL real; no es un endpoint proporcionado por este proyecto. No se desactiva validación TLS ni se inventa un backend público.

Para un backend manual, seguir backend/README.md: ejecutar Uvicorn escuchando en 0.0.0.0:8000 para el alias del emulador. La conectividad depende también de la red/firewall del host.

Referencias: [configuración de seguridad de red Android](https://developer.android.com/privacy-and-security/security-config) y [Retrofit](https://square.github.io/retrofit/).

## Estrategia y comandos de pruebas

Pruebas JVM: mappers, JSON obligatorio/camelCase, contrato de las siete operaciones, 204/Unit, interceptores, repositorios, errores HTTP, desconexión, timeouts reales acotados y cancelación. MockWebServer es evidencia de contrato local; no demuestra por sí mismo conexión a FastAPI. Los fakes de sesión y APIs existen únicamente en tests.

Pruebas instrumentadas: SharedPreferences real (vacío, guardar, reemplazar, borrar y persistir entre instancias) y FastApiIntegrationTest opt-in. La prueba real usa repositorios, Retrofit, interceptor y almacenamiento real; verifica registro, login, logout y CRUD sin crear pantallas.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --console=plain
.\gradlew.bat connectedDebugAndroidTest --console=plain
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-android-api.ps1 -UseAdbReverse
```

Sin fastApiBaseUrl, FastApiIntegrationTest se omite expresamente. El script inicia Uvicorn con clave aleatoria y SQLite temporal, verifica /docs y OpenAPI y ejecuta todas las pruebas instrumentadas. Al finalizar elimina la base temporal, detiene su servidor y retira su túnel ADB, sin modificar backend/.env ni la base de desarrollo.

Por defecto el script intenta 10.0.2.2:18080. `-UseAdbReverse` usa 127.0.0.1:18080 por un túnel ADB temporal, útil cuando el alias del emulador no conecta. Acepta -Port, -Serial y -AdbPath. Requiere emulador/dispositivo autorizado y backend/.venv instalado. El switch ExecutionPolicy solo afecta ese proceso PowerShell.

Resultado de etapa 4 (8 de noviembre de 2026): 35 pruebas JVM aprobadas (34 nuevas y una heredada), 5 instrumentadas aprobadas, 0 fallidas y 0 omitidas en la ejecución con FastAPI real por adb reverse. Sincronización, assembleDebug, assembleDebugAndroidTest y lintDebug aprobados; Manifest y BuildConfig release generados correctamente. No se compiló ni publicó un APK release de producción.

El primer lint falló por includeSubdomains ausente; se corrigió explícitamente a false. El primer intento de integración directa a 10.0.2.2 terminó en Timeout; la causa de red del host no quedó determinada. Se verificó la conexión real mediante adb reverse/127.0.0.1 sin modificar firewall ni backend. La ruta directa predeterminada sigue pendiente de verificación en este entorno. Lint conserva advertencias sobre versiones disponibles, recursos de plantilla y la sugerencia KTX; no se actualiza el stack ni se altera el diseño por ellas.

La suite pytest de la etapa 3 tiene evidencia previa de 90 aprobadas; no se cuenta como ejecutada nuevamente en la etapa 4.

## Pendientes

PDF oficiales ausentes; se usan los requisitos transcritos. Registro del dominio con el docente pendiente. La etapa 5 incorpora Login/Registro desde los SVG entregados, validaciones en ViewModels, StateFlow y navegación de sesión; su verificación final está en [Autenticación MVVM](authentication.md). Home, detalle y CRUD visual siguen pendientes. Ningún criterio C1–C5 se declara LOGRADO por esta infraestructura.
