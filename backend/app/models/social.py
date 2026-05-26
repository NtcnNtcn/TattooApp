from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import Integer, DateTime, ForeignKey, UniqueConstraint
from sqlalchemy.orm import mapped_column, relationship, Mapped

from app.db.base import Base


class Favorite(Base):
    __tablename__ = "favorites"
    __table_args__ = (UniqueConstraint("user_id", "work_id", name="uq_favorite"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    work_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("tattoo_works.id", ondelete="CASCADE"), nullable=False, index=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    user: Mapped["User"] = relationship("User", back_populates="favorites")  # type: ignore[name-defined]
    work: Mapped["TattooWork"] = relationship("TattooWork", back_populates="favorites")  # type: ignore[name-defined]


class Subscription(Base):
    __tablename__ = "subscriptions"
    __table_args__ = (UniqueConstraint("subscriber_id", "master_id", name="uq_subscription"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    subscriber_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    master_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    subscriber: Mapped["User"] = relationship(  # type: ignore[name-defined]
        "User", back_populates="subscriptions", foreign_keys=[subscriber_id]
    )
    master: Mapped["User"] = relationship(  # type: ignore[name-defined]
        "User", back_populates="subscribers", foreign_keys=[master_id]
    )


class Like(Base):
    __tablename__ = "likes"
    __table_args__ = (UniqueConstraint("user_id", "work_id", name="uq_like"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    work_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("tattoo_works.id", ondelete="CASCADE"), nullable=False, index=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    user: Mapped["User"] = relationship("User", back_populates="likes")  # type: ignore[name-defined]
    work: Mapped["TattooWork"] = relationship("TattooWork", back_populates="likes")  # type: ignore[name-defined]
