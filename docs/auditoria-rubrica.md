# Auditoría técnica de la rúbrica — etapa 8

**Alcance:** revisión estática y pruebas automatizadas repetidas. Los PDF oficiales no están disponibles en los archivos inspeccionados; esta matriz usa los requisitos explícitos del proyecto. La calificación final corresponde al docente.

## Matriz C1–C5

| Criterio | Estado técnico | Evidencia |
|---|---|---|
| C1 — Autenticación | Verificado | Backend 97/97; JVM 79/79; instrumentación 22/22; integración real Android/FastAPI aprobada con base SQLite temporal. |
| C2 — CRUD | Verificado | Pruebas de backend, JVM e instrumentación completas; integración Retrofit real aprobada. |
| C3 — MVVM | Verificado técnicamente | UiState inmutable, StateFlow privado, collectAsStateWithLifecycle, inyección por interfaces y pruebas JVM/UI aprobadas. |
| C4 — Repository/DTO/Mapper | Verificado técnicamente | Contratos dominio/data separados, DTO serializables, mappers usados por repositorios, suites JVM/backend aprobadas. |
| C5 — Inyección manual | Verificado técnicamente | Pruebas instrumentadas de identidad de dependencias, navegación con el mismo grafo, test JVM de factories, búsqueda estructural y suites aprobadas. |

La etiqueta “verificado” describe evidencia técnica de este workspace; no asigna la nota del docente. La inscripción formal del dominio recetario requiere evidencia externa y queda fuera de la verificación del código.

## Auditoría C5

- RicettaApplication extiende Application, se registra en AndroidManifest.xml y expone un AppContainer lazy único.
- AppContainer centraliza lazy SessionStorage, OkHttpClient, Retrofit, AuthRepository y RecipeRepository.
- OkHttp configura un AuthInterceptor sobre el mismo SessionStorage; Retrofit utiliza ese mismo cliente y crea servicios para auth y recetas.
- Los repositorios se exponen como interfaces; sus implementaciones reciben servicios/storage por constructor.
- MainActivity obtiene el contenedor de Application y pasa contratos a RicettaApp. La navegación crea los ViewModels mediante factories del mismo conjunto de dependencias.
- Los seis ViewModels reciben AuthRepository, SessionStorage y/o RecipeRepository por constructor. No construyen infraestructura.
- AuthViewModelFactories y RecipeViewModelFactories usan ViewModelProvider.Factory, viewModelFactory e initializer.
- La búsqueda en app/src/main encontró construcción de builders y RepositoryImpl solo en AppContainer; RicettaApplication solo crea la raíz AppContainer.

### DI01–DI14

| ID | Evidencia | Resultado |
|---|---|---|
| DI01 | La instrumentación verifica dos lecturas de appContainer desde Application con assertSame | APROBADO |
| DI02 | Identidad de sessionStorage | APROBADO |
| DI03 | Identidad de authRepository | APROBADO |
| DI04 | Identidad de recipeRepository | APROBADO |
| DI05 | Identidad del OkHttp lazy, un AuthInterceptor, y búsqueda estática de builders fuera del contenedor | APROBADO |
| DI06 | Identidad del Retrofit lazy y callFactory idéntico al OkHttp compartido | APROBADO |
| DI07 | El interceptor conserva referencia al SessionStorage compartido | APROBADO |
| DI08 | Factories auth existentes ejercitadas con repositorios falsos en tests JVM | APROBADO |
| DI09 | Nuevo test JVM crea list/detail/form por factories con fake y verifica llamadas/estado | APROBADO |
| DI10 | Pruebas inyectan repositorios falsos usando interfaces | APROBADO |
| DI11 | Búsqueda de Retrofit.Builder/create en app/src/main: construcción centralizada en AppContainer | APROBADO |
| DI12 | Búsqueda de RepositoryImpl en producción: solo AppContainer | APROBADO |
| DI13 | Instrumentación navega Login → Registro → Login con dependencias de Application | APROBADO |
| DI14 | La misma prueba compara identidades del grafo antes y después de navegación | APROBADO |

DI05, DI06, DI11 y DI12 combinan revisión estructural con comprobación de comportamiento; no se cambió la visibilidad pública de producción para testear miembros internos.

## Dominio y límites académicos

Receta mantiene id y atributos de texto, enteros y booleano; ingredientes/preparación persisten como textos. ViewModels validan los formularios y pruebas cubren entradas inválidas. El backend mantiene asociación/autorización por usuario. El dominio y el código no prueban que el tema haya sido registrado formalmente con el docente.

## Evidencias ejecutadas

- Backend: 97 aprobadas, una advertencia de deprecación de Starlette/httpx.
- Android JVM: 79 aprobadas, 0 fallidas.
- Android instrumentadas conectadas en Pixel_10_Pro_XL API 35: 22 aprobadas, 0 fallidas. El modo de integración habilitó FastApiIntegrationTest y AuthRealUiTest.
- Integración Android–FastAPI real: script verify-android-api.ps1 -UseAdbReverse terminó con “Integración Android/FastAPI aprobada”; utilizó SQLite temporal aislada y el túnel reverse.
- assembleDebug, testDebugUnitTest, lintDebug y assembleDebugAndroidTest: BUILD SUCCESSFUL.
- Estos resultados corresponden a ejecuciones de esta etapa.
