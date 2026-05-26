import enum
from datetime import datetime, timezone
from typing import List, Optional

from sqlalchemy import Integer, String, DateTime, ForeignKey, Enum as SAEnum
from sqlalchemy.orm import relationship, Mapped, mapped_column

from app.db.base import Base

class ApplicationStatus(str, enum.Enum):
    PENDING = "pending"
    ACCEPTED = "accepted"
    REJECTED = "rejected"
    COMPLETED = "completed"

class ConsultationApplication(Base):
    __tablename__ = "consultation_applications"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    client_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    master_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    contact_info: Mapped[str] = mapped_column(String(255), nullable=False)
    message: Mapped[Optional[str]] = mapped_column(String(127), nullable=True)
    status: Mapped[ApplicationStatus] = mapped_column(
        SAEnum(ApplicationStatus, name="applicationstatus"),
        default=ApplicationStatus.PENDING,
        nullable=False
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    client: Mapped["User"] = relationship(
        "User", foreign_keys=[client_id], back_populates="consultation_requests"
    )
    master: Mapped["User"] = relationship(
        "User", foreign_keys=[master_id], back_populates="received_consultation_requests"
    )

class MasterApplication(Base):
    __tablename__ = "master_applications"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    master_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, unique=True)
    status: Mapped[ApplicationStatus] = mapped_column(
        SAEnum(ApplicationStatus, name="applicationstatus"),
        default=ApplicationStatus.PENDING,
        nullable=False
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    master: Mapped["User"] = relationship(
        "User", back_populates="master_application"
    )
