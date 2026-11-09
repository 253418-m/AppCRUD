"""Evolución de un archivo SQLite de la etapa anterior, sin borrar datos."""
import sqlite3

from fastapi.testclient import TestClient

from app.main import create_app
from app.security import create_access_token


def test_legacy_database_migrates_without_losing_rows_and_is_idempotent(settings):
    path = settings.database_url.removeprefix("sqlite:///")
    with sqlite3.connect(path) as db:
        db.execute("""CREATE TABLE usuarios (
            id INTEGER PRIMARY KEY, nombre VARCHAR(120) NOT NULL,
            correo VARCHAR(254) NOT NULL UNIQUE, password_hash TEXT NOT NULL
        )""")
        db.execute("""CREATE TABLE recetas (
            id INTEGER PRIMARY KEY, usuario_id INTEGER NOT NULL REFERENCES usuarios(id),
            nombre VARCHAR(150) NOT NULL, porciones INTEGER NOT NULL,
            tiempo_preparacion_min INTEGER NOT NULL, dificultad VARCHAR(7) NOT NULL,
            es_vegetariana BOOLEAN NOT NULL
        )""")
        db.execute("INSERT INTO usuarios VALUES (1, 'Ana', 'ana@example.com', 'hash-historico')")
        db.execute("INSERT INTO recetas VALUES (7, 1, 'Sopa antigua', 2, 30, 'FACIL', 1)")
        db.commit()

    auth = {"Authorization": f"Bearer {create_access_token(1, settings)}"}
    with TestClient(create_app(settings)) as client:
        response = client.get("/recetas/7", headers=auth)
        assert response.status_code == 200
        assert response.json() == {
            "id": 7, "nombre": "Sopa antigua", "porciones": 2,
            "tiempoPreparacionMin": 30, "dificultad": "FACIL",
            "esVegetariana": True, "ingredientes": None, "preparacion": None,
        }
        data = {**response.json(), "ingredientes": "Agua\nSal", "preparacion": "Hervir\nServir"}
        del data["id"]
        assert client.put("/recetas/7", headers=auth, json=data).json()["preparacion"] == "Hervir\nServir"

    with TestClient(create_app(settings)) as restarted:
        assert restarted.get("/recetas/7", headers=auth).json()["ingredientes"] == "Agua\nSal"
        assert restarted.get("/recetas", headers=auth).json()[0]["id"] == 7
    with sqlite3.connect(path) as db:
        assert db.execute("SELECT count(*) FROM usuarios").fetchone()[0] == 1
        assert db.execute("SELECT count(*) FROM recetas").fetchone()[0] == 1
        assert [c[1] for c in db.execute("PRAGMA table_info(recetas)")][-2:] == [
            "ingredientes", "preparacion"
        ]
