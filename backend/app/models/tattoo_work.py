import enum
from datetime import datetime, timezone
from typing import Optional, List

from sqlalchemy import Integer, String, DateTime, ForeignKey, Text, Enum as SAEnum, Table, Column
from sqlalchemy.orm import mapped_column, relationship, Mapped

from app.db.base import Base


class WorkStatus(str, enum.Enum):
    pending = "pending"
    approved = "approved"
    rejected = "rejected"


# Many-to-many junction table
work_tags = Table(
    "work_tags",
    Base.metadata,
    Column("work_id", Integer, ForeignKey("tattoo_works.id", ondelete="CASCADE"), primary_key=True),
    Column("tag_id", Integer, ForeignKey("tags.id", ondelete="CASCADE"), primary_key=True),
)


class Tag(Base):
    __tablename__ = "tags"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    name: Mapped[str] = mapped_column(String(100), unique=True, nullable=False, index=True)

    works: Mapped[List["TattooWork"]] = relationship(
        "TattooWork", secondary=work_tags, back_populates="tags"
    )

    def __repr__(self) -> str:
        return f"<Tag {self.name}>"


class TattooWork(Base):
    __tablename__ = "tattoo_works"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    master_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    image_url: Mapped[str] = mapped_column(String(500), nullable=False)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    status: Mapped[str] = mapped_column(
        SAEnum(WorkStatus, name="work_status_enum"),
        default=WorkStatus.pending,
        nullable=False,
        index=True,
    )
    like_count: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    rejection_reason: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
        index=True,
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    # Relationships
    master: Mapped["User"] = relationship(  # type: ignore[name-defined]
        "User", back_populates="tattoo_works", foreign_keys=[master_id]
    )
    tags: Mapped[List["Tag"]] = relationship(
        "Tag", secondary=work_tags, back_populates="works"
    )
    favorites: Mapped[List["Favorite"]] = relationship(  # type: ignore[name-defined]
        "Favorite", back_populates="work", cascade="all, delete-orphan"
    )
    likes: Mapped[List["Like"]] = relationship(  # type: ignore[name-defined]
        "Like", back_populates="work", cascade="all, delete-orphan"
    )
    reviews: Mapped[List["WorkReview"]] = relationship(  # type: ignore[name-defined]
        "WorkReview", back_populates="work", cascade="all, delete-orphan"
    )

    def __repr__(self) -> str:
        return f"<TattooWork id={self.id} status={self.status}>"
