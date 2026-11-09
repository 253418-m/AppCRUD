"""Fixtures aisladas: cada prueba usa un archivo SQLite temporal."""
import secrets

import pytest
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app


@pytest.fixture
def settings(tmp_path):
    return Settings(
        jwt_secret_key=secrets.token_urlsafe(48),
        database_url=f"sqlite:///{(tmp_path / 'test.sqlite3').as_posix()}",
        access_token_expire_minutes=60,
    )


@pytest.fixture
def client(settings):
    with TestClient(create_app(settings)) as test_client:
        yield test_client


@pytest.fixture
def register(client):
    def register_user(correo="ana@example.com", password="ClaveSegura123", nombre="Ana"):
        response = client.post(
            "/register",
            json={"nombre": nombre, "correo": correo, "password": password},
        )
        assert response.status_code == 201, response.text
        return response.json()

    return register_user


@pytest.fixture
def auth(register):
    account = register()
    return {"Authorization": f"Bearer {account['accessToken']}"}


@pytest.fixture
def recipe():
    return {
        "nombre": "Pasta con verduras",
        "porciones": 2,
        "tiempoPreparacionMin": 25,
        "dificultad": "FACIL",
        "esVegetariana": True,
        "ingredientes": "Tomate\nCebolla",
        "preparacion": "Lavar verduras\nCocinar",
    }
