"""HTTP real: arranca Uvicorn dos veces usando un archivo SQLite temporal."""
from contextlib import contextmanager
import os
from pathlib import Path
import secrets
import socket
import subprocess
import sys
import time

import httpx


@contextmanager
def running_server(env, port, log_path):
    with log_path.open("wb") as log:
        process = subprocess.Popen(
            [sys.executable, "-m", "uvicorn", "app.main:create_app", "--factory",
             "--host", "127.0.0.1", "--port", str(port), "--no-access-log"],
            cwd=Path(__file__).resolve().parents[1],
            env=env, stdout=log, stderr=subprocess.STDOUT,
        )
        try:
            deadline = time.monotonic() + 30
            with httpx.Client(base_url=f"http://127.0.0.1:{port}", timeout=5,
                              trust_env=False) as client:
                while True:
                    assert process.poll() is None, "Uvicorn no inició; revisar log temporal"
                    try:
                        if client.get("/openapi.json").status_code == 200:
                            break
                    except httpx.TransportError:
                        pass
                    assert time.monotonic() < deadline, "Uvicorn no respondió"
                    time.sleep(0.1)
                yield client
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)


def test_real_http_and_persistence_after_process_restart(tmp_path):
    database_file = tmp_path / "live.sqlite3"
    env = {
        **os.environ,
        "JWT_SECRET_KEY": secrets.token_urlsafe(48),
        "DATABASE_URL": f"sqlite:///{database_file.as_posix()}",
        "ACCESS_TOKEN_EXPIRE_MINUTES": "60",
    }
    with socket.socket() as reservation:
        reservation.bind(("127.0.0.1", 0))
        port = reservation.getsockname()[1]
    credentials = {"correo": "http@example.com", "password": "PruebaHttp123"}
    recipe = {
        "nombre": "Sopa HTTP", "porciones": 2, "tiempoPreparacionMin": 20,
        "dificultad": "FACIL", "esVegetariana": True,
        "ingredientes": "Agua\nSal", "preparacion": "Hervir\nServir",
    }
    with running_server(env, port, tmp_path / "first.log") as client:
        assert client.get("/docs").status_code == 200
        assert "/recetas" in client.get("/openapi.json").json()["paths"]
        assert client.get("/recetas").status_code == 401
        register = client.post("/register", json={"nombre": "HTTP", **credentials})
        assert register.status_code == 201
        headers = {"Authorization": f"Bearer {register.json()['accessToken']}"}
        created = client.post("/recetas", json=recipe, headers=headers)
        assert created.status_code == 201
        stored = created.json()
        assert stored == {"id": stored["id"], **recipe}
    assert database_file.stat().st_size > 0
    with running_server(env, port, tmp_path / "second.log") as client:
        login = client.post("/login", json=credentials)
        assert login.status_code == 200
        headers = {"Authorization": f"Bearer {login.json()['accessToken']}"}
        assert client.get("/recetas", headers=headers).json() == [stored]
        path = f"/recetas/{stored['id']}"
        assert client.get(path, headers=headers).json() == stored
        updated = client.put(path, json={**recipe, "porciones": 5}, headers=headers)
        assert updated.status_code == 200
        assert updated.json() == {**stored, "porciones": 5}
        deleted = client.delete(path, headers=headers)
        assert deleted.status_code == 204 and deleted.content == b""
        assert client.get("/recetas", headers=headers).json() == []
        assert client.get(path, headers=headers).status_code == 404
    print("HTTP real aprobado: /docs, OpenAPI, auth, CRUD y persistencia tras reiniciar Uvicorn.")
