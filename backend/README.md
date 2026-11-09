# API REST de Ricetta

Backend iniciado en la etapa 3: FastAPI, SQLite y SQLAlchemy 2, Pydantic, JWT HS256 y Argon2 mediante pwdlib. Dominio académico: Recetario; recurso: Receta. La etapa 6 amplía únicamente el contrato de Receta con ingredientes y preparación.

## Instalación y ejecución en Windows

Desde la raíz del repositorio, con Python 3.13:

```powershell
python -m venv backend/.venv
Set-Location backend
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
$env:JWT_SECRET_KEY = & .\.venv\Scripts\python.exe -c "import secrets; print(secrets.token_urlsafe(48))"
$env:ACCESS_TOKEN_EXPIRE_MINUTES = "60"
.\.venv\Scripts\python.exe -m uvicorn app.main:create_app --factory --host 127.0.0.1 --port 8000
```

La asignación de la clave no la imprime en la terminal. Guardar una clave propia en `backend/.env` permite reutilizarla entre sesiones; usar `.env.example` como referencia, nunca versionar el secreto. Cambiar la clave invalida tokens anteriores. El servidor se detiene con Ctrl+C.

Documentación: http://127.0.0.1:8000/docs y http://127.0.0.1:8000/openapi.json. Las rutas de autenticación reciben JSON, no formularios OAuth2. Swagger permite pegar un token con el botón Authorize (esquema HTTP Bearer).

Para pruebas desde el emulador, iniciar con `--host 0.0.0.0` y usar `http://10.0.2.2:8000/` como base URL. Esto expone el puerto a la red local; habilitarlo solo durante el desarrollo. La configuración Android permite HTTP local solamente en debug; release requiere HTTPS.

## Configuración

| Variable | Valor |
|---|---|
| JWT_SECRET_KEY | Obligatoria, aleatoria, al menos 32 bytes; sin valor predeterminado |
| ACCESS_TOKEN_EXPIRE_MINUTES | Entero positivo; predeterminado 60 |
| DATABASE_URL | Opcional; predeterminado SQLite en `backend/ricetta.db` |
| Ejemplo DATABASE_URL | `sqlite:///C:/ruta/absoluta/ricetta.db` |

El archivo `.env` se busca en `backend/`; las variables del proceso tienen prioridad. Una URL SQLite relativa explícita se resuelve respecto al directorio de ejecución. Se recomienda una ruta absoluta. La configuración se valida al iniciar, sin imprimir secretos.

`requirements.txt` fija las versiones directas y transitivas probadas. Principales: FastAPI 0.162.2, SQLAlchemy 2.1.8, Pydantic 2.14.3, pwdlib 0.3.1, PyJWT 2.15.1, Uvicorn 0.54.0, pytest 9.2.0 y httpx 0.28.1. No se instalan paquetes globales.

## Organización y persistencia

- `app/config.py`: configuración del entorno.
- `app/database.py`: motor SQLite y claves foráneas activas en cada conexión.
- `app/models.py`: tablas y relación Usuario–Receta.
- `app/schemas.py`: validación HTTP y respuestas públicas.
- `app/security.py`: Argon2 y generación/validación JWT.
- `app/dependencies.py`: sesión por petición y usuario autenticado.
- `app/routers/`: autenticación y CRUD.
- `app/main.py`: factoría, ciclo de vida y errores.
- `tests/`: contratos, seguridad, persistencia y HTTP real.

`usuarios`: id, nombre, correo único normalizado y password_hash.
`recetas`: id, usuario_id (FK), nombre, porciones, tiempo_preparacion_min, dificultad, es_vegetariana, ingredientes y preparacion. Las dos últimas columnas son opcionales solo para filas históricas; crear y editar exige textos no vacíos.

SQLAlchemy crea el esquema inicial al iniciar. A continuación, `migrate_recetas` consulta `PRAGMA table_info(recetas)` y añade solo las columnas que falten con `ALTER TABLE`; volver a iniciar es idempotente. La migración no elimina ni recrea tablas o filas. Los valores de recetas antiguas permanecen `NULL`, sin ingredientes o instrucciones inventadas. Al editar una fila histórica se requieren ambos textos. Antes de actualizar una instalación existente, conservar una copia de seguridad de `backend/ricetta.db`; al iniciar esta versión, la migración se aplica automáticamente. Los tests usan archivos temporales y prueban dos arranques sobre un esquema anterior.

## Contrato HTTP

Base local: `http://127.0.0.1:8000/`. Los campos públicos compuestos usan camelCase. Todos los errores conservan sus códigos HTTP.

| Método y ruta | Cuerpo | Éxito |
|---|---|---|
| POST /register | nombre, correo, password | 201, Auth |
| POST /login | correo, password | 200, Auth |
| GET /recetas | Sin cuerpo | 200, arreglo Receta (vacío: []) |
| GET /recetas/{id} | Sin cuerpo | 200, Receta |
| POST /recetas | Siete atributos editables, incluidos ingredientes y preparacion | 201, Receta completa |
| PUT /recetas/{id} | Los siete atributos obligatorios | 200, Receta completa |
| DELETE /recetas/{id} | Sin cuerpo | 204, sin contenido |

Registro de ejemplo (credenciales ficticias):

```json
{"nombre":"Ana","correo":"ana@example.com","password":"ClaveSegura123"}
```

Auth:

```json
{"accessToken":"JWT_GENERADO","tokenType":"bearer","usuario":{"id":1,"nombre":"Ana","correo":"ana@example.com"}}
```

Crear/editar:

```json
{"nombre":"Pasta con verduras","porciones":2,"tiempoPreparacionMin":25,"dificultad":"FACIL","esVegetariana":true,"ingredientes":"Tomate\nCebolla","preparacion":"Lavar\nCocinar"}
```

Receta devuelta por POST, GET detalle y PUT:

```json
{"id":1,"nombre":"Pasta con verduras","porciones":2,"tiempoPreparacionMin":25,"dificultad":"FACIL","esVegetariana":true,"ingredientes":"Tomate\nCebolla","preparacion":"Lavar\nCocinar"}
```

Todas las rutas de recetas requieren `Authorization: Bearer TOKEN`. La API deriva usuario_id del token; no acepta id, usuario_id, usuarioId ni campos desconocidos en el cuerpo. Las recetas de otros usuarios son indistinguibles de un recurso inexistente (404); tampoco aparecen en el listado.

Validación: nombres recortados y no vacíos (usuario hasta 120 caracteres, receta hasta 150); correo válido, recortado y convertido a minúsculas; contraseña de registro de 8 a 128 caracteres, sin recortar espacios. Login requiere contraseña no vacía. Porciones y tiempo son enteros JSON de 1 a 2147483647, sin aceptar strings, booleanos o decimales. Dificultad: FACIL, MEDIA o DIFICIL. esVegetariana es booleano JSON obligatorio. PUT reemplaza los siete atributos editables. Ingredientes y preparación son strings obligatorios de 1 a 20000 caracteres y no pueden contener solo espacios; se conservan sus saltos de línea. En GET de filas históricas esos campos se devuelven como `null`.

| Código | Significado |
|---|---|
| 401 | Credenciales incorrectas o token ausente, inválido o expirado; WWW-Authenticate: Bearer |
| 404 | Receta inexistente o ajena |
| 409 | Correo ya registrado |
| 422 | Datos o parámetros inválidos |
| 500 | Error interno con respuesta genérica |

Error normal: `{"detail":"Receta no encontrada"}`. Validación: `{"detail":[{"loc":["body","porciones"],"msg":"...","type":"..."}]}`. No se incluyen valores de entrada ni contextos que puedan reproducir contraseñas. Los errores internos se registran en el servidor, sin exponer trazas al cliente; el motor oculta parámetros SQL.

## Seguridad

JWT usa exclusivamente HS256 y exige sub (id de usuario), iat y exp. Las fechas emitidas son enteros; se verifican firma, expiración y usuario existente. Las claves no están en el código. Las contraseñas se guardan con Argon2; no se devuelven hashes ni contraseñas. El login responde el mismo 401 para correo inexistente o contraseña incorrecta y realiza verificación de hash en ambos casos. No hay logs explícitos de tokens o contraseñas.

No se incorporan refresh tokens, revocación ni endpoints adicionales de sesión; el cierre de sesión local está implementado en Android. Esta es una base académica local, sin despliegue público ni configuración de producción.

## Pruebas

Desde `backend/`:

```powershell
.\.venv\Scripts\python.exe -m pytest -q -s
.\.venv\Scripts\python.exe -m pip check
.\.venv\Scripts\python.exe -m compileall -q app tests
```

Resultado verificado el 8 de noviembre de 2026 con Python 3.13.14 en Windows: **97 pruebas aprobadas, 0 fallidas**, una advertencia de deprecación. También aprobaron instalación desde requirements.txt, pip check y compileall.

Las pruebas verifican registro/login, validaciones, Argon2, claims JWT, autorización por propietario, CRUD, JSON camelCase, 204 vacío, FK, errores sanitizados y persistencia en archivo. `test_live_api.py` inicia realmente Uvicorn en localhost con puerto disponible y SQLite temporal: comprueba /docs, OpenAPI, registro y creación; termina el proceso, lo reinicia sobre la misma base, inicia sesión y recupera la receta antes de editar/eliminar. No imprime tokens y finaliza los procesos que crea.

Advertencia conocida: Starlette 1.10.0 marca como obsoleto el uso de httpx en TestClient. Las versiones fijadas siguen siendo compatibles; no se agrega un segundo cliente solo por esa advertencia.

## Compatibilidad con Retrofit Android

Las interfaces Retrofit de Android ya están implementadas con `suspend fun x(): T`: AuthResponseDto, List<RecetaDto> o RecetaDto según el endpoint. El repositorio traduce HttpException, errores de red, timeout y serialización a AppResult/DataError. El DTO y el mapper incluyen ingredientes y preparación; sus campos de respuesta son nullable solo para registros históricos.

No se utilizan `Response<T>`, `Call<T>`, enqueue, execute ni callbacks REST. DELETE conserva 204 sin cuerpo y retorno `Unit`, probado con Retrofit. Las llamadas suspend son main-safe; los ViewModels las ejecutan en viewModelScope y respetan CancellationException.

Referencias consultadas: [seguridad JWT y pwdlib en FastAPI](https://fastapi.tiangolo.com/tutorial/security/oauth2-jwt/) y [SQLite y claves foráneas en SQLAlchemy](https://docs.sqlalchemy.org/en/20/dialects/sqlite.html#foreign-key-support).

## Pendientes académicos

Confirmar registro del dominio con el docente. PDF oficiales ausentes; se usan los requisitos transcritos. La integración Android se documenta en el README principal. La evaluación académica completa sigue pendiente. Completar esta API no demuestra todavía LOGRADO en los criterios Android. No se inician funciones ajenas a la etapa 6 sin autorización.
