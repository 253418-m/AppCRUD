from typing import Annotated

from fastapi import Depends, HTTPException, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import InvalidTokenError
from sqlalchemy.orm import Session

from app.models import Usuario
from app.security import decode_user_id

bearer = HTTPBearer(auto_error=False)


def get_session(request: Request):
    with Session(request.app.state.engine) as session:
        yield session


DbSession = Annotated[Session, Depends(get_session)]


def unauthorized():
    return HTTPException(
        status_code=401, detail="No autenticado",
        headers={"WWW-Authenticate": "Bearer"},
    )


def get_current_user(
    request: Request,
    session: DbSession,
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer)],
) -> Usuario:
    if credentials is None:
        raise unauthorized()
    try:
        user_id = decode_user_id(credentials.credentials, request.app.state.settings)
    except (InvalidTokenError, ValueError, TypeError):
        raise unauthorized() from None
    user = session.get(Usuario, user_id)
    if user is None:
        raise unauthorized()
    return user


CurrentUser = Annotated[Usuario, Depends(get_current_user)]
