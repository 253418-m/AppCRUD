from dataclasses import replace
from datetime import datetime, timezone

import jwt
import pytest
from fastapi.testclient import TestClient
from pwdlib import PasswordHash
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.config import Settings
from app.main import create_app
from app.models import Usuario
from app.routers import recetas
from app.security import create_access_token


def assert_bearer_error(response):
    assert response.status_code == 401
    assert response.headers["www-authenticate"] == "Bearer"


def test_register_returns_token_user_and_safe_fields(client, settings):
    response = client.post(
        "/register",
        json={"nombre": "  Ana  ", "correo": " Ana@EXAMPLE.com ", "password": "ClaveSegura123"},
    )
    assert response.status_code == 201
    body = response.json()
    assert set(body) == {"accessToken", "tokenType", "usuario"}
    assert body["tokenType"] == "bearer"
    assert body["usuario"] == {"id": 1, "nombre": "Ana", "correo": "ana@example.com"}
    claims = jwt.decode(body["accessToken"], settings.jwt_secret_key, algorithms=["HS256"])
    assert claims["sub"] == str(body["usuario"]["id"])
    assert claims["exp"] > datetime.now(timezone.utc).timestamp()
    assert "password" not in response.text
    assert "hash" not in response.text


def test_password_is_argon2_hash_in_database(client, register):
    register()
    with Session(client.app.state.engine) as session:
        user = session.scalar(select(Usuario).where(Usuario.correo == "ana@example.com"))
        assert user is not None
        assert user.password_hash.startswith("$argon2")
        assert user.password_hash != "ClaveSegura123"
        assert PasswordHash.recommended().verify("ClaveSegura123", user.password_hash)


def test_duplicate_email_is_normalized_and_returns_conflict(client, register):
    register()
    response = client.post(
        "/register",
        json={"nombre": "Otra", "correo": " ANA@EXAMPLE.COM ", "password": "OtraClave123"},
    )
    assert response.status_code == 409
    assert client.post(
        "/login", json={"correo": "ana@example.com", "password": "ClaveSegura123"}
    ).status_code == 200


@pytest.mark.parametrize(
    "changes",
    [
        {"nombre": ""},
        {"nombre": "   "},
        {"correo": ""},
        {"correo": "sin-arroba"},
        {"password": ""},
        {"password": "1234567"},
        {"usuario_id": 100},
        {"id": 100},
    ],
)
def test_register_rejects_invalid_input(client, changes):
    payload = {"nombre": "Ana", "correo": "ana@example.com", "password": "ClaveSegura123"}
    payload.update(changes)
    response = client.post("/register", json=payload)
    assert response.status_code == 422
    for error in response.json()["detail"]:
        assert "input" not in error
        assert "ctx" not in error


@pytest.mark.parametrize("field", ["nombre", "correo", "password"])
def test_register_requires_all_fields(client, field):
    payload = {"nombre": "Ana", "correo": "ana@example.com", "password": "ClaveSegura123"}
    del payload[field]
    assert client.post("/register", json=payload).status_code == 422


def test_login_normalizes_email_and_preserves_password(client, register):
    register(password=" ClaveSegura123 ")
    response = client.post(
        "/login", json={"correo": " ANA@EXAMPLE.COM ", "password": " ClaveSegura123 "}
    )
    assert response.status_code == 200
    assert response.json()["usuario"]["correo"] == "ana@example.com"
    assert set(response.json()) == {"accessToken", "tokenType", "usuario"}
    assert "password" not in response.text
    assert "hash" not in response.text


def test_incorrect_password_and_unknown_email_have_same_error(client, register):
    register()
    wrong_password = client.post(
        "/login", json={"correo": "ana@example.com", "password": "Incorrecta123"}
    )
    unknown_user = client.post(
        "/login", json={"correo": "nadie@example.com", "password": "Incorrecta123"}
    )
    assert_bearer_error(wrong_password)
    assert_bearer_error(unknown_user)
    assert wrong_password.json() == unknown_user.json() == {"detail": "Credenciales incorrectas"}


@pytest.mark.parametrize(
    "payload",
    [
        {},
        {"correo": "ana@example.com"},
        {"password": "ClaveSegura123"},
        {"correo": "", "password": "ClaveSegura123"},
        {"correo": "ana@example.com", "password": ""},
        {"correo": "invalido", "password": "ClaveSegura123"},
    ],
)
def test_login_validates_input(client, payload):
    assert client.post("/login", json=payload).status_code == 422


@pytest.mark.parametrize("authorization", [None, "Bearer invalido", "Basic abc", "Bearer"])
def test_missing_or_malformed_token(client, authorization):
    headers = {} if authorization is None else {"Authorization": authorization}
    response = client.get("/recetas", headers=headers)
    assert_bearer_error(response)
    assert response.json() == {"detail": "No autenticado"}


def valid_claims():
    now = int(datetime.now(timezone.utc).timestamp())
    return {"sub": "1", "iat": now, "exp": now + 300}


@pytest.mark.parametrize(
    "changes",
    [
        {"exp": 1},
        {"sub": "no-es-id"},
        {"sub": "999999"},
        {"sub": "-1"},
        {"sub": "0"},
        {"sub": 1},
    ],
)
def test_invalid_claims_are_unauthorized(client, settings, register, changes):
    register()
    token = jwt.encode(
        {**valid_claims(), **changes}, settings.jwt_secret_key, algorithm="HS256"
    )
    response = client.get("/recetas", headers={"Authorization": f"Bearer {token}"})
    assert_bearer_error(response)


@pytest.mark.parametrize("claim", ["sub", "exp", "iat"])
def test_required_claims_cannot_be_omitted(client, settings, register, claim):
    register()
    claims = valid_claims()
    del claims[claim]
    token = jwt.encode(claims, settings.jwt_secret_key, algorithm="HS256")
    assert_bearer_error(client.get("/recetas", headers={"Authorization": f"Bearer {token}"}))


@pytest.mark.parametrize(
    "claim,value",
    [
        ("exp", None),
        ("exp", True),
        ("exp", 4102444800.5),
        ("exp", []),
        ("iat", None),
        ("iat", True),
        ("iat", 1.5),
        ("iat", []),
    ],
)
def test_date_claims_require_json_integers(client, settings, register, claim, value):
    register()
    claims = {**valid_claims(), claim: value}
    token = jwt.encode(claims, settings.jwt_secret_key, algorithm="HS256")
    response = client.get("/recetas", headers={"Authorization": f"Bearer {token}"})
    assert_bearer_error(response)


@pytest.mark.parametrize("algorithm", ["HS256", "HS384"])
def test_wrong_key_or_algorithm_is_unauthorized(client, settings, register, algorithm):
    register()
    token = jwt.encode(
        valid_claims(),
        "otra-clave-distinta-para-prueba-" * 3 if algorithm == "HS256" else settings.jwt_secret_key,
        algorithm=algorithm,
    )
    assert_bearer_error(client.get("/recetas", headers={"Authorization": f"Bearer {token}"}))


def test_configuration_rejects_missing_secret(settings):
    with pytest.raises(ValueError, match="JWT_SECRET_KEY"):
        Settings(jwt_secret_key="", database_url=settings.database_url)


def test_access_token_expiration_is_configurable(settings):
    configured = replace(settings, access_token_expire_minutes=7)
    token = create_access_token(42, configured)
    claims = jwt.decode(token, configured.jwt_secret_key, algorithms=["HS256"])
    assert jwt.get_unverified_header(token)["alg"] == "HS256"
    assert claims["sub"] == "42"
    assert type(claims["iat"]) is int
    assert type(claims["exp"]) is int
    assert claims["exp"] - claims["iat"] == 7 * 60


def test_internal_error_response_is_sanitized(settings, auth, monkeypatch):
    def fail(*args, **kwargs):
        raise RuntimeError("Detalle interno que no debe aparecer en HTTP")

    monkeypatch.setattr(recetas, "owned_recipe", fail)
    with TestClient(create_app(settings), raise_server_exceptions=False) as failing_client:
        response = failing_client.get("/recetas/1", headers=auth)
    assert response.status_code == 500
    assert response.json() == {"detail": "Error interno del servidor"}
    assert "RuntimeError" not in response.text
    assert "Detalle interno" not in response.text


def test_users_survive_new_application_and_engine(client, settings, register):
    account = register()
    with TestClient(create_app(settings)) as restarted:
        response = restarted.post(
            "/login", json={"correo": "ana@example.com", "password": "ClaveSegura123"}
        )
        assert response.status_code == 200
        assert response.json()["usuario"] == account["usuario"]


def test_openapi_documents_http_bearer_and_endpoints(client):
    assert client.get("/docs").status_code == 200
    response = client.get("/openapi.json")
    assert response.status_code == 200
    schema = response.json()
    assert set(schema["paths"]) >= {"/register", "/login", "/recetas", "/recetas/{id}"}
    assert any(
        security.get("type") == "http" and security.get("scheme") == "bearer"
        for security in schema["components"]["securitySchemes"].values()
    )
