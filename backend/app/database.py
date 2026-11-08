from sqlalchemy import create_engine, event
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
