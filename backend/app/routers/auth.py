from fastapi import APIRouter, HTTPException, Request
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError

from app.dependencies import DbSession
from app.models import Usuario
from app.schemas import AuthOutput, LoginInput, RegistroInput, UsuarioOutput
from app.security import DUMMY_HASH, create_access_token, password_hasher

router = APIRouter(tags=["Autenticación"])


def auth_response(user: Usuario, request: Request):
    return AuthOutput(
        access_token=create_access_token(user.id, request.app.state.settings),
        usuario=UsuarioOutput.model_validate(user),
    )


@router.post("/register", response_model=AuthOutput, status_code=201)
def register(body: RegistroInput, request: Request, session: DbSession):
    user = Usuario(
        nombre=body.nombre,
        correo=str(body.correo),
        password_hash=password_hasher.hash(body.password.get_secret_value()),
    )
    session.add(user)
    try:
        session.commit()
    except IntegrityError as error:
        session.rollback()
        if getattr(error.orig, "sqlite_errorname", None) == "SQLITE_CONSTRAINT_UNIQUE":
            raise HTTPException(409, "Correo ya registrado") from None
        raise
    session.refresh(user)
    return auth_response(user, request)


@router.post("/login", response_model=AuthOutput)
def login(body: LoginInput, request: Request, session: DbSession):
    user = session.scalar(select(Usuario).where(Usuario.correo == str(body.correo)))
    valid = password_hasher.verify(
        body.password.get_secret_value(), user.password_hash if user else DUMMY_HASH
    )
    if user is None or not valid:
        raise HTTPException(
            401, "Credenciales incorrectas",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return auth_response(user, request)
