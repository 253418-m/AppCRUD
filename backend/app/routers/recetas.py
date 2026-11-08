from typing import Annotated

from fastapi import APIRouter, HTTPException, Path, Response
from sqlalchemy import select

from app.dependencies import CurrentUser, DbSession
from app.models import Receta
from app.schemas import RecetaInput, RecetaOutput

router = APIRouter(prefix="/recetas", tags=["Recetas"])
RecipeId = Annotated[int, Path(ge=1, le=2147483647)]


def owned_recipe(recipe_id: int, user_id: int, session: DbSession) -> Receta:
    recipe = session.scalar(
        select(Receta).where(Receta.id == recipe_id, Receta.usuario_id == user_id)
    )
    if recipe is None:
        raise HTTPException(404, "Receta no encontrada")
    return recipe


@router.get("", response_model=list[RecetaOutput])
def list_recipes(user: CurrentUser, session: DbSession):
    return session.scalars(
        select(Receta).where(Receta.usuario_id == user.id).order_by(Receta.id)
    ).all()


@router.get("/{id}", response_model=RecetaOutput)
def get_recipe(id: RecipeId, user: CurrentUser, session: DbSession):
    return owned_recipe(id, user.id, session)


@router.post("", response_model=RecetaOutput, status_code=201)
def create_recipe(body: RecetaInput, user: CurrentUser, session: DbSession):
    recipe = Receta(usuario_id=user.id, **body.model_dump())
    session.add(recipe)
    session.commit()
    session.refresh(recipe)
    return recipe


@router.put("/{id}", response_model=RecetaOutput)
def update_recipe(id: RecipeId, body: RecetaInput, user: CurrentUser, session: DbSession):
    recipe = owned_recipe(id, user.id, session)
    for key, value in body.model_dump().items():
        setattr(recipe, key, value)
    session.commit()
    session.refresh(recipe)
    return recipe


@router.delete("/{id}", status_code=204, response_class=Response)
def delete_recipe(id: RecipeId, user: CurrentUser, session: DbSession):
    recipe = owned_recipe(id, user.id, session)
    session.delete(recipe)
    session.commit()
    return Response(status_code=204)
