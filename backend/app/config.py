from dataclasses import dataclass
import os
from pathlib import Path

from dotenv import load_dotenv

BACKEND_DIR = Path(__file__).resolve().parent.parent


@dataclass(frozen=True)
class Settings:
    jwt_secret_key: str
    database_url: str
    access_token_expire_minutes: int = 60

    def __post_init__(self):
        if len(self.jwt_secret_key.encode("utf-8")) < 32:
            raise ValueError("JWT_SECRET_KEY debe contener al menos 32 bytes.")
        if self.access_token_expire_minutes < 1:
            raise ValueError("ACCESS_TOKEN_EXPIRE_MINUTES debe ser positivo.")

    @classmethod
    def from_env(cls):
        load_dotenv(BACKEND_DIR / ".env")
        return cls(
            jwt_secret_key=os.environ.get("JWT_SECRET_KEY", ""),
            database_url=os.environ.get(
                "DATABASE_URL", f"sqlite:///{(BACKEND_DIR / 'ricetta.db').as_posix()}"
            ),
            access_token_expire_minutes=int(
                os.environ.get("ACCESS_TOKEN_EXPIRE_MINUTES", "60")
            ),
        )
