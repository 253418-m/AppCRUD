# Recetas — etapa 6

## Alcance y referencias

Se utilizan `Mockups/Ver mis recetas.svg` (406 × 905, contenido 390 × 898), `Ver receta.svg` (406 × 1182), `Crear receta o Editar.svg` (406 × 1146) y la lámina `Diálogos de alerta - no es una pantalla.svg` (390 × 1452). Los SVG originales permanecen intactos y excluidos de Git. No se proporcionaron PNG o archivos de tipografía. Los textos exportados como trazados se aproximan con SansSerif del sistema. Las pantallas se construyen con Compose, no con capturas como fondo.

Home presenta barra verde de 80 dp, tarjeta verde de resumen con cierre de sesión, acción Nueva receta compacta y tarjetas de 350 dp máximo alimentadas por la API. La cantidad de recetas y los atributos de cada tarjeta son reales. La lista utiliza LazyColumn. Detalle conserva tarjetas blancas de radio 24 dp, propiedades sobre fondo cálido y acciones Editar/Eliminar. `No.` deriva del id y el subtítulo de Ingredientes deriva de porciones. Preparación se enumera según líneas no vacías del texto persistido; no se inventan pasos. El formulario de 350 dp máximo y tarjeta blanca contiene campos editables, dificultad segmentada, vegetariana, ingredientes y preparación multilínea, guardado y cancelación. Crear y editar utilizan el mismo Composable.

La referencia de alertas se usa solo como componentes: confirmación de eliminación, estado vacío, error recuperable y mensaje de éxito. Se reutiliza LogoutConfirmation de la etapa 5. No existe una ruta de “Estados y Alertas”. Las áreas táctiles de los campos y acciones alcanzan al menos 48 dp cuando corresponde; por ello y por los insets del sistema, la altura exacta del SVG puede variar.

## Modelo, migración y contrato

El recurso Receta conserva id, nombre, porciones, tiempoPreparacionMin, dificultad y esVegetariana, y añade `ingredientes` y `preparacion` como textos. POST/PUT exigen ambos no vacíos (máximo 20000 caracteres) y conservan saltos de línea. GET puede devolver `null` para los dos campos de filas históricas. Android usa `String?` en Receta/RecetaDto de respuesta y `String` no nulo en GuardarReceta/GuardarRecetaDto. No se crean entidades Ingrediente/Paso.

Al arrancar FastAPI, `create_all` crea tablas nuevas y `migrate_recetas` agrega por `ALTER TABLE` solo las columnas ausentes en bases anteriores. La migración es idempotente y no toca usuarios o recetas existentes. Una receta antigua muestra explícitamente “Sin ingredientes registrados” y “Sin preparación registrada”; al editarla debe completar ambos campos. Antes de actualizar una instalación con datos propios, hacer copia de seguridad de `backend/ricetta.db`. La prueba de migración crea un archivo con el esquema anterior, comprueba la fila heredada, edita los campos y arranca de nuevo sin perder filas.

Ejemplo de solicitud, con valores ilustrativos que la aplicación no inserta automáticamente:

```json
{"nombre":"Sopa","porciones":2,"tiempoPreparacionMin":30,"dificultad":"FACIL","esVegetariana":true,"ingredientes":"Agua\nSal","preparacion":"Hervir\nServir"}
```

El contrato conserva `GET/POST/PUT/DELETE /recetas`, Bearer automático, aislamiento por usuario y DELETE 204 sin cuerpo. Retrofit mantiene funciones suspend con retorno directo; Repository traduce errores en AppResult/DataError y los ViewModels solo conocen modelos de dominio.

## MVVM, navegación y sesión

`RecipeListViewModel`, `RecipeDetailViewModel` y `RecipeFormViewModel` reciben `RecipeRepository` por constructor mediante `RecipeViewModelFactories` con `viewModelFactory/initializer`. MutableStateFlow privado, UiState inmutable y collectAsStateWithLifecycle separan estado de presentación. Las pantallas no crean Retrofit ni repositorios. El formulario valida nombre, enteros positivos sin desbordamiento, ingredientes y preparación antes de enviar; los errores se muestran junto a los campos. Loading bloquea duplicados. Cancelar vuelve sin llamar al backend.

Rutas protegidas: `home`, `recipe_detail/{id}`, `recipe_create` y `recipe_edit/{id}`. Se mantiene un NavHost y el SessionViewModel de la etapa 5. Al volver a Home/Detalle se consulta de nuevo la API, sin recrear MainActivity. Los resultados exitosos muestran un mensaje de operación: creación y eliminación en Home, edición en Detalle. DELETE solo navega después de 204. Un 401 en una operación protegida solicita la limpieza de sesión existente; un timeout no borra el token. Logout conserva su diálogo y limpia el back stack al aprobar.

## Pruebas y ejecución

```powershell
Set-Location backend
.\.venv\Scripts\python.exe -m pytest -q
Set-Location ..
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --console=plain
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-android-api.ps1 -UseAdbReverse
```

El script usa SQLite temporal y FastAPI real, conecta mediante `adb reverse` y limpia servidor, base y túnel al finalizar. No apunta a la base de desarrollo. Para ejecutar manualmente la app con ese túnel, configurar la URL debug al mismo puerto en el build. El cambio local del usuario en `gradle.properties` configura `http://127.0.0.1:8000/` y se conserva fuera del commit de esta etapa.

Las pruebas con fakes validan lógica y UI, pero no cuentan como integración HTTP real. Las pruebas de backend y `FastApiIntegrationTest` comprueban persistencia, autenticación y CRUD con los campos extendidos contra FastAPI. La verificación de esta etapa registró 97 pruebas backend, 78 JVM y 19 instrumentadas aprobadas; entre las últimas hay pruebas de Retrofit contra FastAPI real. Se revisaron Home, Detalle, Formulario y el diálogo en el emulador con una receta temporal persistida, además de renderizar los SVG originales.

## Límites

No se agregan dashboard, imágenes, favoritos, comentarios ni funciones de notas. No se declara automáticamente LOGRADO en la rúbrica: los PDF oficiales no están disponibles en este workspace y el registro del dominio con el docente sigue pendiente. La comparación visual muestra diferencias pendientes: tipografía SansSerif en lugar de la fuente trazada, insets del emulador, ausencia de los iconos culinarios específicos del SVG en cada tarjeta y de algunos iconos pequeños del formulario, propiedades y diálogo. El icono de búsqueda del Home no se incorporó porque no existe una función de búsqueda en el alcance. No se afirma igualdad píxel a píxel.
