import pytest
from fastapi.testclient import TestClient
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.main import create_app
from app.models import Receta


PUBLIC_FIELDS = {
    "id", "nombre", "porciones", "tiempoPreparacionMin", "dificultad", "esVegetariana"
}


def create_recipe(client, auth, recipe):
    response = client.post("/recetas", headers=auth, json=recipe)
    assert response.status_code == 201, response.text
    body = response.json()
    assert set(body) == PUBLIC_FIELDS
    assert isinstance(body["id"], int)
    assert body["id"] > 0
    return body


def test_empty_list_is_json_array(client, auth):
    response = client.get("/recetas", headers=auth)
    assert response.status_code == 200
    assert response.json() == []
    assert response.headers["content-type"].startswith("application/json")


def test_create_detail_update_and_delete_contract(client, auth, recipe):
    created = create_recipe(client, auth, recipe)
    assert created == {"id": created["id"], **recipe}
    recipe_id = created["id"]

    detail = client.get(f"/recetas/{recipe_id}", headers=auth)
    assert detail.status_code == 200
    assert detail.json() == created
    assert client.get("/recetas", headers=auth).json() == [created]

    updated_payload = {
        "nombre": "  Sopa de pollo  ", "porciones": 4, "tiempoPreparacionMin": 45,
        "dificultad": "MEDIA", "esVegetariana": False,
    }
    updated = client.put(f"/recetas/{recipe_id}", headers=auth, json=updated_payload)
    assert updated.status_code == 200
    assert set(updated.json()) == PUBLIC_FIELDS
    expected = {"id": recipe_id, **updated_payload, "nombre": "Sopa de pollo"}
    assert updated.json() == expected
    assert client.get(f"/recetas/{recipe_id}", headers=auth).json() == expected
    assert client.get("/recetas", headers=auth).json() == [expected]

    deleted = client.delete(f"/recetas/{recipe_id}", headers=auth)
    assert deleted.status_code == 204
    assert deleted.content == b""
    assert client.get("/recetas", headers=auth).json() == []
    assert client.get(f"/recetas/{recipe_id}", headers=auth).status_code == 404
    assert client.delete(f"/recetas/{recipe_id}", headers=auth).status_code == 404


@pytest.mark.parametrize(
    "changes",
    [
        {"nombre": ""},
        {"nombre": "   "},
        {"porciones": 0},
        {"porciones": -1},
        {"porciones": True},
        {"porciones": 1.5},
        {"porciones": "2"},
        {"tiempoPreparacionMin": 0},
        {"tiempoPreparacionMin": -3},
        {"tiempoPreparacionMin": False},
        {"tiempoPreparacionMin": 1.5},
        {"tiempoPreparacionMin": "20"},
        {"dificultad": "IMPOSIBLE"},
        {"dificultad": "facil"},
        {"esVegetariana": "true"},
        {"esVegetariana": 1},
        {"esVegetariana": None},
        {"id": 888},
        {"usuario_id": 888},
        {"usuarioId": 888},
    ],
)
def test_create_and_update_validate_without_mutating(client, auth, recipe, changes):
    created = create_recipe(client, auth, recipe)
    invalid = {**recipe, **changes}
    assert client.post("/recetas", headers=auth, json=invalid).status_code == 422
    assert client.put(f"/recetas/{created['id']}", headers=auth, json=invalid).status_code == 422
    assert client.get("/recetas", headers=auth).json() == [created]


@pytest.mark.parametrize(
    "field", ["nombre", "porciones", "tiempoPreparacionMin", "dificultad", "esVegetariana"]
)
def test_create_and_put_require_all_editable_fields(client, auth, recipe, field):
    created = create_recipe(client, auth, recipe)
    incomplete = dict(recipe)
    del incomplete[field]
    assert client.post("/recetas", headers=auth, json=incomplete).status_code == 422
    assert client.put(
        f"/recetas/{created['id']}", headers=auth, json=incomplete
    ).status_code == 422


@pytest.mark.parametrize("difficulty", ["FACIL", "MEDIA", "DIFICIL"])
def test_all_domain_difficulties(client, auth, recipe, difficulty):
    body = create_recipe(client, auth, {**recipe, "dificultad": difficulty})
    assert body["dificultad"] == difficulty


def test_user_cannot_access_or_modify_other_users_recipe(client, register, recipe):
    ana = register()
    ana_headers = {"Authorization": f"Bearer {ana['accessToken']}"}
    created = create_recipe(client, ana_headers, recipe)
    other = register(correo="otro@example.com", nombre="Otro")
    other_headers = {"Authorization": f"Bearer {other['accessToken']}"}
    path = f"/recetas/{created['id']}"
    assert client.get("/recetas", headers=other_headers).json() == []
    for response in (
        client.get(path, headers=other_headers),
        client.put(path, headers=other_headers, json={**recipe, "nombre": "Robada"}),
        client.delete(path, headers=other_headers),
    ):
        assert response.status_code == 404
    assert client.get(path, headers=ana_headers).json() == created


@pytest.mark.parametrize("method,path", [
    ("GET", "/recetas"),
    ("GET", "/recetas/1"),
    ("POST", "/recetas"),
    ("PUT", "/recetas/1"),
    ("DELETE", "/recetas/1"),
])
def test_every_recipe_endpoint_requires_authentication(client, recipe, method, path):
    response = client.request(method, path, json=recipe if method in {"POST", "PUT"} else None)
    assert response.status_code == 401
    assert response.headers["www-authenticate"] == "Bearer"


def test_missing_recipe_returns_404_for_get_put_delete(client, auth, recipe):
    path = "/recetas/999999"
    assert client.get(path, headers=auth).status_code == 404
    assert client.put(path, headers=auth, json=recipe).status_code == 404
    assert client.delete(path, headers=auth).status_code == 404


def test_recipes_persist_after_reopening_application(client, settings, auth, recipe):
    created = create_recipe(client, auth, recipe)
    with TestClient(create_app(settings)) as restarted:
        response = restarted.get(f"/recetas/{created['id']}", headers=auth)
        assert response.status_code == 200
        assert response.json() == created
        assert restarted.get("/recetas", headers=auth).json() == [created]


def test_sqlite_enforces_foreign_key(client):
    with Session(client.app.state.engine) as session:
        orphan = Receta(
            usuario_id=999999,
            nombre="Huérfana",
            porciones=1,
            tiempo_preparacion_min=10,
            dificultad="FACIL",
            es_vegetariana=True,
        )
        session.add(orphan)
        with pytest.raises(IntegrityError):
            session.commit()
        session.rollback()
