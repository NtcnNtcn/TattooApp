from sqlalchemy.orm import DeclarativeBase, MappedColumn
from sqlalchemy import Integer
from sqlalchemy.orm import mapped_column


class Base(DeclarativeBase):
    """Base class for all SQLAlchemy models."""
    pass
