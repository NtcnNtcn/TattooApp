from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import Integer, String, Text, DateTime, ForeignKey
from sqlalchemy.orm import mapped_column, relationship, Mapped

from app.db.base import Base


class WorkReview(Base):
    """Audit log: every status change (pending→approved/rejected) is recorded here.
    Also populated by the PostgreSQL trigger trg_work_status_change."""
    __tablename__ = "work_reviews"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    work_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("tattoo_works.id", ondelete="CASCADE"), nullable=False, index=True
    )
    reviewer_id: Mapped[Optional[int]] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="SET NULL"), nullable=True
    )
    old_status: Mapped[str] = mapped_column(String(20), nullable=False)
    new_status: Mapped[str] = mapped_column(String(20), nullable=False)
    comment: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    work: Mapped["TattooWork"] = relationship("TattooWork", back_populates="reviews")  # type: ignore[name-defined]
    reviewer: Mapped[Optional["User"]] = relationship("User", back_populates="work_reviews")  # type: ignore[name-defined]


class Notification(Base):
    __tablename__ = "notifications"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True
    )
    type: Mapped[str] = mapped_column(String(50), nullable=False)
    # e.g. "work_approved", "work_rejected", "new_work_from_subscription"
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    message: Mapped[str] = mapped_column(Text, nullable=False)
    is_read: Mapped[bool] = mapped_column(default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    user: Mapped["User"] = relationship("User", back_populates="notifications")  # type: ignore[name-defined]
