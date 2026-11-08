from typing import Annotated, Literal

from pydantic import (
    BaseModel, ConfigDict, EmailStr, Field, SecretStr, StrictBool,
    StrictInt, StringConstraints, field_validator,
)

NombreUsuario = Annotated[
    str, StringConstraints(strip_whitespace=True, min_length=1, max_length=120)
]
NombreReceta = Annotated[
    str, StringConstraints(strip_whitespace=True, min_length=1, max_length=150)
]
EnteroPositivo = Annotated[StrictInt, Field(ge=1, le=2147483647)]


class CorreoInput(BaseModel):
    model_config = ConfigDict(extra="forbid")
    correo: EmailStr

    @field_validator("correo", mode="before")
    @classmethod
    def normalizar_correo(cls, value):
        return value.strip().lower() if isinstance(value, str) else value


class RegistroInput(CorreoInput):
    nombre: NombreUsuario
    password: SecretStr = Field(min_length=8, max_length=128)


class LoginInput(CorreoInput):
    password: SecretStr = Field(min_length=1, max_length=128)


class UsuarioOutput(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    nombre: str
    correo: EmailStr


class AuthOutput(BaseModel):
    access_token: str = Field(serialization_alias="accessToken")
    token_type: Literal["bearer"] = Field(default="bearer", serialization_alias="tokenType")
    usuario: UsuarioOutput


class RecetaInput(BaseModel):
    model_config = ConfigDict(extra="forbid")
    nombre: NombreReceta
    porciones: EnteroPositivo
    tiempo_preparacion_min: EnteroPositivo = Field(alias="tiempoPreparacionMin")
    dificultad: Literal["FACIL", "MEDIA", "DIFICIL"]
    es_vegetariana: StrictBool = Field(alias="esVegetariana")


class RecetaOutput(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    nombre: str
    porciones: int
    tiempo_preparacion_min: int = Field(serialization_alias="tiempoPreparacionMin")
    dificultad: Literal["FACIL", "MEDIA", "DIFICIL"]
    es_vegetariana: bool = Field(serialization_alias="esVegetariana")
