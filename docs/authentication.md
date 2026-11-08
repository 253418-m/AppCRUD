# Autenticación MVVM — etapa 5

## Alcance

Login, Registro, restauración local de sesión, navegación protegida y cierre de sesión con confirmación. Se reutilizan AuthRepository, SessionStorage, AppResult/DataError, Retrofit y el AppContainer de la etapa 4. No se modifica el backend ni se implementan listado, detalle, formulario o eliminación visual de recetas.

Esta etapa conserva Navigation Compose. No se migra a Navigation 3, no se añade un contenedor nuevo y no se introduce Hilt, Dagger o Koin.

## Dependencias de presentación y pruebas

Se añaden lifecycle-viewmodel-compose, lifecycle-viewmodel-ktx y lifecycle-runtime-compose usando la versión 2.6.1 ya existente en Version Catalog. AGP, Kotlin, Compose, Navigation, Retrofit, OkHttp y corrutinas mantienen sus versiones previas.

Espresso pasa de 3.5.1 a 3.7.0 como corrección mínima de la incompatibilidad observada en el emulador: esa versión evita la reflexión sobre InputManager.getInstance y utiliza getSystemService. La corrección está descrita en las [notas oficiales de Espresso 3.7.0](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0). No se migra el conjunto completo de herramientas por este fallo.

## Referencias originales e inventario visual

La carpeta local `Mockups/` contiene seis SVG y ningún PNG, TTF u OTF proporcionado por el usuario:

| Archivo | Dimensiones/viewBox | Uso actual |
|---|---|---|
| Iniciar sesión.svg | 390 × 884 | Login |
| Crear cuenta.svg | 406 × 909; contenido de 390 × 902 desplazado (8, 7) | Registro |
| Diálogos de alerta - no es una pantalla.svg | 390 × 1452 | Solo confirmación de logout y lenguaje de errores |
| Ver mis recetas.svg | Referencia disponible | Reservado para etapa 6 |
| Ver receta.svg | Referencia disponible | Reservado para etapa 6 |
| Crear receta o Editar.svg | Referencia disponible | Reservado para CRUD visual posterior |

Los originales permanecen intactos y `/Mockups/` está excluido de Git. Solo se incorporan recursos derivados necesarios para la aplicación. Las copias PNG generadas para inspeccionar los SVG son renderizados temporales, no capturas PNG originales del usuario.

Los textos del SVG fueron convertidos a trazados; no hay una declaración recuperable de fuente ni archivos de fuente suministrados. Se utiliza `FontFamily.SansSerif` del sistema, sin descargar tipografías. Por esa razón no se afirma igualdad tipográfica exacta.

## Geometría, color y recursos

| Componente | Referencia aplicada |
|---|---|
| Login | Tarjeta máxima de 350 dp, radio de 24 dp y encabezado de 214 dp |
| Login: campos y botón | 310 × 48 en la referencia; radio de 12 dp |
| Registro: barra superior | 390 × 80 en el SVG, anchura adaptable |
| Registro: encabezado | 350 × 122, radio de 16 dp |
| Registro: formulario | Máximo 350 dp, radio de 24 dp y padding horizontal de 24 dp |
| Registro: campos | Referencia 302 × 46; mínimo implementado 48 dp por accesibilidad |
| Logout | Tarjeta de máximo 350 dp, radio de 24 dp, icono de perfil de 40 dp y botón verde suave |

Se conservan proporciones y anchos máximos mediante layouts y desplazamiento, no mediante coordenadas absolutas de pantalla. Insets del sistema, teclado, errores y tamaño de fuente pueden aumentar la altura necesaria. Las sombras se aproximan con elevación de Material 3; los filtros SVG no se copian como una imagen de fondo.

Paleta reutilizada: fondo `#FFF8F3`, verde `#204E3B`, menta `#8EBEA6`, tinta `#221A0E`, texto `#414944`, campos `#FFF2E1` y texto secundario `#717973`. Logout usa verde `#033726`, verde suave `#CDEBC5`, beige `#FBECD7` y texto `#082009`. Errores usan `#93000A` sobre `#EFE0CC`.

Los drawables `auth_mail`, `auth_lock`, `auth_eye`, `auth_arrow`, `auth_back`, `auth_book`, `auth_profile` y `auth_logout` convierten trazados seleccionados de los SVG a VectorDrawable, conservando la geometría original con viewport y traslación apropiados. Los formularios, textos, campos, enlaces y botones son componentes Compose reales. No se utiliza un PNG de pantalla completa para simular la interfaz.

No se crea una pantalla de Estados y Alertas. El diálogo de logout conserva el texto sobre “notas culinarias” por fidelidad al mockup; esa frase queda señalada para revisión de contenido y no introduce una función de notas.

## MVVM y validación

`presentation.auth.login` y `presentation.auth.register` contienen Screen, UiState y ViewModel. Las Routes en RicettaApp observan mediante collectAsStateWithLifecycle y pasan estado/callbacks a las pantallas. Los Composables no ejecutan HTTP ni validan reglas de negocio.

LoginViewModel y RegisterViewModel reciben AuthRepository por constructor. Mantienen MutableStateFlow privado y exponen StateFlow mediante asStateFlow. Sus data classes de UiState son inmutables e incluyen entradas, errores por campo, carga, error general, visibilidad y usuario autenticado.

Las validaciones se ejecutan desde los ViewModels con AuthValidation: correo obligatorio y formato válido, nombre no vacío, contraseña obligatoria, mínimo de ocho caracteres al registrar y confirmación obligatoria/coincidente. Se respetan los máximos de nombre, correo y contraseña del contrato. Nombre/correo se normalizan antes de enviar; la contraseña nunca se recorta. confirmPassword no forma parte de ninguna llamada al repositorio o DTO.

submit establece loading antes de lanzar la corrutina y rechaza envíos duplicados. Las operaciones usan viewModelScope; CancellationException se relanza y finally restablece carga. Los errores recuperables conservan las entradas. Un correo duplicado se asocia solo a correoError, sin repetir el mismo mensaje en un banner.

Las contraseñas y su confirmación solo viven en memoria del ViewModel durante el formulario: no se guardan en preferencias, disco, SavedStateHandle o rememberSaveable. Se vacían después de autenticar correctamente. El ViewModel conserva entradas durante recomposición y rotación; después de muerte del proceso se solicita volver a introducir las contraseñas.

AuthViewModelFactories crea ViewModelProvider.Factory usando viewModelFactory e initializer para LoginViewModel, RegisterViewModel y SessionViewModel. MainActivity recibe los repositorios y almacenamiento del AppContainer existente y los pasa a RicettaApp; no se construyen dependencias de red en Composables.

## Sesión, navegación y logout

Rutas: startup, login, register y home_provisional. SessionViewModel inicia en Checking, consulta SessionStorage.hasSession y muestra exclusivamente startup mientras espera; no presenta Login antes de conocer el resultado. Un fallo de lectura permite reintentar.

La existencia local de token permite restaurar SignedIn, pero no demuestra que el token siga vigente en el servidor. No se agregan endpoints para validar sesión. onProtectedError(Unauthorized) solicita borrar sesión mediante AuthRepository; cambia a SignedOut únicamente si la eliminación aprueba. Un Timeout u otro error recuperable conserva el token.

El éxito de registro/login llega después de que AuthRepository haya guardado el token. La Route informa a SessionViewModel y la navegación usa popUpTo inclusivo y launchSingleTop para retirar destinos anteriores. SignedOut reconoce tanto Login como Registro, de modo que rotar en Registro no fuerza Login. Los efectos de navegación dependen del resultado y de la fase, no de cada recomposición.

home_provisional solo confirma la sesión y permite cerrar sesión; no representa el Home definitivo ni realiza CRUD. Logout primero muestra LogoutConfirmation. Cancelar o descartar no llama al repositorio; confirmar bloquea duplicados y llama cerrarSesion. La navegación se limpia solo después de éxito. Ante fallo de almacenamiento se conserva la sesión, se muestra un error recuperable y el diálogo permite reintentar. Tras logout no debe ser posible volver a Home usando Atrás.

El guard de fase impide dibujar destinos protegidos si el estado es SignedOut, incluso si se restaura una navegación antigua. SessionViewModel cancela o ignora resultados de restauración obsoletos para que no sobrescriban una autenticación reciente.

## Teclado y adaptación

Se conserva enableEdgeToEdge y adjustResize. Login y Registro usan Scaffold con WindowInsets.safeDrawing; padding e insets se consumen una sola vez y el contenido puede desplazarse. Los campos admiten IME Next/Done y los controles de visibilidad tienen áreas táctiles de 48 dp y descripciones accesibles.

La fuente del sistema, sombras de elevación y aumento de campos de Registro de 46 a 48 dp son diferencias conocidas. El diálogo de logout mide aproximadamente 211 dp de alto frente a 208 dp de la referencia, por el ajuste de componentes Material y sus áreas táctiles. Se compararon Login y Registro renderizados en el emulador con los SVG originales rasterizados mediante Chrome. Se ajustó la descripción de Login a dos líneas y la altura de los indicadores de campos obligatorios a partir de esa comparación. Se mantiene la limitación tipográfica: no se afirma igualdad píxel a píxel con textos originalmente trazados.

Capturas locales de evidencia, excluidas de Git: build/mockup-review/stage5-login.png, build/mockup-review/stage5-register.png, build/mockup-review/stage5-small-keyboard.png, build/mockup-review/stage5-logout.png y build/mockup-review/stage5-login-landscape.png. No son imágenes usadas como interfaz. La revisión manual comparó el diálogo con el SVG de alertas, comprobó que Registro conserva el nombre al rotar y que, a 320 × 640 con teclado abierto, se puede desplazar hasta confirmación, botón y enlace de acceso. Login en horizontal a 884 × 390 permite alcanzar campos, botón y enlace mediante desplazamiento; Registro también se comprobó en horizontal. Las pruebas instrumentadas verificaron recreación, navegación y los tres controles de visibilidad. Al finalizar se restauraron resolución 1344 × 2992, densidad 480 y orientación originales del emulador.

## Comandos de verificación

Desde la raíz:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --console=plain
.\gradlew.bat connectedDebugAndroidTest --console=plain
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-android-api.ps1 -UseAdbReverse
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-android-api.ps1 -UseAdbReverse -PauseForVisualReview
```

El script requiere backend/.venv y un emulador/dispositivo autorizado. Usa una clave aleatoria y SQLite temporal, ejecuta FastAPI real y pasa su URL a las pruebas. -UseAdbReverse conecta mediante 127.0.0.1 y evita depender de la ruta 10.0.2.2, que presentó timeout en este entorno. -PauseForVisualReview conserva temporalmente servidor/túnel para revisar la UI antes de continuar y limpiar. No debe apuntarse la suite real a una base de producción.

La prueba con fakes no constituye integración real. Las pruebas reales usan datos aislados y no imprimen tokens. Los originales SVG no se alteran para ajustar el resultado a la implementación.

## Resultados actuales de la etapa 5

| Verificación | Estado documentado |
|---|---|
| Sincronización Gradle | Aprobada, incluida la sincronización final |
| assembleDebug, assembleDebugAndroidTest y lintDebug | Aprobados; la ejecución final de assembleDebug, testDebugUnitTest y lintDebug terminó con BUILD SUCCESSFUL, salida 0 |
| Pruebas JVM | 67 aprobadas: 35 anteriores reejecutadas y 32 nuevas |
| Pruebas instrumentadas | 14 aprobadas, 0 fallidas y 0 omitidas: 5 de etapa 4 reejecutadas, 8 de UI con fakes y 1 de UI con API real |
| FastAPI real desde las pantallas | Registro, login y logout aprobados con SQLite temporal y adb reverse |
| Comparación visual, teclado, rotación y tamaño reducido | Comparación manual y capturas realizadas; recreación y visibilidad aprobadas |
| Reinicio real del proceso de MainActivity | Aprobado manualmente con la Application y AppContainer reales: token restaurado tras force-stop y arranque en frío; después de logout, el nuevo arranque muestra Login |

La primera ejecución instrumentada falló por la incompatibilidad de Espresso descrita arriba; la suite posterior aprobó con 3.7.0. La prueba de recreación también aprobó tras mantener estable el callsite setContent del host de pruebas. Los 14 resultados instrumentados anteriores corresponden a ejecución real de esta etapa, no a la mera reproducción de reportes históricos. Las ocho pruebas con fakes validan comportamiento de UI; la prueba AuthRealUiTest se ejecuta contra FastAPI real y no usa autenticación simulada.

La revisión manual de MainActivity registró un usuario real contra FastAPI con SQLite temporal. Tras force-stop y arranque en frío se restauró Home provisional; Cancelar mantuvo la sesión, confirmar logout mostró Login y el siguiente arranque en frío siguió mostrando Login. Atrás salió al launcher sin volver a Home. Estas comprobaciones de proceso son adicionales a la recreación del host instrumentado. El script finalizó con salida 0 y limpió el backend temporal y adb reverse.

Las 90 pruebas pytest de la etapa 3 son evidencia histórica y no se contabilizan como ejecutadas en esta etapa.

## Rúbrica: evidencia y límites

| Criterio | Archivos y evidencia actuales | Pendiente |
|---|---|---|
| C1: autenticación/sesión/logout | LoginViewModel, RegisterViewModel, SessionViewModel, Routes y LogoutConfirmation; validaciones/transiciones JVM, navegación/Atrás con fakes, registro/login/logout contra FastAPI real y reinicio en frío de MainActivity con y sin sesión verificado manualmente | Evaluación final con los documentos oficiales y verificación del flujo completo al incorporar recetas |
| C2: CRUD | Se reutiliza la infraestructura de datos de la etapa 4; no se implementa CRUD visual | Home, listado, detalle, formularios y eliminación visual en etapas siguientes |
| C3: MVVM | UiStates inmutables, StateFlow, collectAsStateWithLifecycle y Screen/Route separados; pruebas JVM de estado/cancelación e instrumentadas de recreación/navegación; revisión manual de teclado y rotación | Mantener y verificar el patrón en las futuras pantallas de recetas |
| C4: Repository/DTO/Mapper | Se reutilizan los contratos, mappers y repositorios de la etapa 4; la suite JVM anterior se reejecuta | Verificar su integración con los futuros ViewModels de recetas |
| C5: DI manual | Un AppContainer, ViewModels por constructor y AuthViewModelFactories con initializer; pruebas de factories y recorrido instrumentado de autenticación real | Aplicar el mismo mecanismo a los futuros ViewModels de recetas |

Ningún criterio se declara LOGRADO únicamente por existencia del código. Los PDF oficiales no están disponibles; se usan los requisitos transcritos y sigue pendiente confirmar el registro del dominio con el docente.

## Siguiente etapa

La etapa 6 requiere revisión de esta etapa y autorización expresa. Utilizará Ver mis recetas.svg y Ver receta.svg para Home/listado y detalle. No se anticipan pantallas CRUD ni se crea funcionalidad de notas por una referencia textual del mockup.
