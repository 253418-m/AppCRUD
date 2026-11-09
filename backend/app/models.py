from sqlalchemy import Boolean, CheckConstraint, ForeignKey, String, Text
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


class Base(DeclarativeBase):
    pass


class Usuario(Base):
    __tablename__ = "usuarios"

    id: Mapped[int] = mapped_column(primary_key=True)
    nombre: Mapped[str] = mapped_column(String(120))
    correo: Mapped[str] = mapped_column(String(254), unique=True)
    password_hash: Mapped[str] = mapped_column(Text)
    recetas: Mapped[list["Receta"]] = relationship(back_populates="usuario")


class Receta(Base):
    __tablename__ = "recetas"
    __table_args__ = (
        CheckConstraint("length(trim(nombre)) > 0", name="nombre_no_vacio"),
        CheckConstraint("porciones >= 1", name="porciones_positivas"),
        CheckConstraint("tiempo_preparacion_min > 0", name="tiempo_positivo"),
        CheckConstraint(
            "dificultad IN ('FACIL', 'MEDIA', 'DIFICIL')",
            name="dificultad_valida",
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    usuario_id: Mapped[int] = mapped_column(ForeignKey("usuarios.id"), index=True)
    nombre: Mapped[str] = mapped_column(String(150))
    porciones: Mapped[int]
    tiempo_preparacion_min: Mapped[int]
    dificultad: Mapped[str] = mapped_column(String(7))
    es_vegetariana: Mapped[bool] = mapped_column(Boolean(create_constraint=True))
    ingredientes: Mapped[str | None] = mapped_column(Text, nullable=True)
    preparacion: Mapped[str | None] = mapped_column(Text, nullable=True)
    usuario: Mapped[Usuario] = relationship(back_populates="recetas")
