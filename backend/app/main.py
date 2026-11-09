from contextlib import asynccontextmanager
import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.config import Settings
from app.database import create_database_engine, migrate_recetas
from app.models import Base
from app.routers import auth, recetas

logger = logging.getLogger(__name__)


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or Settings.from_env()
    engine = create_database_engine(settings.database_url)

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        Base.metadata.create_all(engine)
        migrate_recetas(engine)
        try:
            yield
        finally:
            engine.dispose()

    app = FastAPI(title="Ricetta API", version="1.0.0", lifespan=lifespan)
    app.state.engine = engine
    app.state.settings = settings

    @app.exception_handler(RequestValidationError)
    async def validation_error(request: Request, error: RequestValidationError):
        # No incluir valores de entrada: pueden contener contraseñas.
        details = [
            {"loc": item["loc"], "msg": item["msg"], "type": item["type"]}
            for item in error.errors()
        ]
        return JSONResponse(status_code=422, content={"detail": details})

    @app.exception_handler(Exception)
    async def internal_error(request: Request, error: Exception):
        logger.exception("Error interno de la API")
        return JSONResponse(status_code=500, content={"detail": "Error interno del servidor"})

    app.include_router(auth.router)
    app.include_router(recetas.router)
    return app
