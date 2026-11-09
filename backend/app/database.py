from sqlalchemy import create_engine, event, text
from sqlalchemy.engine import make_url


def create_database_engine(database_url: str):
    if make_url(database_url).get_backend_name() != "sqlite":
        raise ValueError("Esta API utiliza una base de datos SQLite.")
    engine = create_engine(
        database_url,
        connect_args={"check_same_thread": False},
        hide_parameters=True,
    )

    @event.listens_for(engine, "connect")
    def enable_foreign_keys(connection, _):
        cursor = connection.cursor()
        cursor.execute("PRAGMA foreign_keys=ON")
        cursor.close()

    return engine


def migrate_recetas(engine):
    """Añade textos opcionales a recetas existentes sin alterar filas históricas."""
    with engine.begin() as connection:
        columns = {row[1] for row in connection.exec_driver_sql("PRAGMA table_info(recetas)")}
        for name in ("ingredientes", "preparacion"):
            if name not in columns:
                connection.execute(text(f"ALTER TABLE recetas ADD COLUMN {name} TEXT"))
