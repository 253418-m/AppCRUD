from datetime import datetime, timedelta, timezone
import secrets

import jwt
from pwdlib import PasswordHash

from app.config import Settings

password_hasher = PasswordHash.recommended()
DUMMY_HASH = password_hasher.hash(secrets.token_urlsafe(32))


def create_access_token(user_id: int, settings: Settings) -> str:
    now = datetime.now(timezone.utc)
    return jwt.encode(
        {
            "sub": str(user_id),
            "iat": now,
            "exp": now + timedelta(minutes=settings.access_token_expire_minutes),
        },
        settings.jwt_secret_key,
        algorithm="HS256",
    )


def decode_user_id(token: str, settings: Settings) -> int:
    claims = jwt.decode(
        token,
        settings.jwt_secret_key,
        algorithms=["HS256"],
        options={"require": ["sub", "exp", "iat"]},
    )
    if (type(claims["exp"]) is not int or type(claims["iat"]) is not int
            or claims["exp"] <= claims["iat"]):
        raise jwt.InvalidTokenError("Invalid timestamps")
    subject = claims["sub"]
    if not isinstance(subject, str) or not subject.isascii() or not subject.isdecimal():
        raise jwt.InvalidTokenError("Invalid subject")
    user_id = int(subject)
    if user_id < 1 or user_id > 9223372036854775807:
        raise jwt.InvalidTokenError("Invalid subject")
    return user_id
