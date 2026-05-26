import enum
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import Integer, String, ForeignKey, DateTime, Enum as SAEnum
from sqlalchemy.orm import mapped_column, Mapped, relationship

from app.db.base import Base


class VerificationType(str, enum.Enum):
    registration = "registration"
    email_change = "email_change"
    password_reset = "password_reset"


class VerificationCode(Base):
    __tablename__ = "verification_codes"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    email: Mapped[str] = mapped_column(String(255), nullable=False)
    code: Mapped[str] = mapped_column(String(6), nullable=False)
    type: Mapped[VerificationType] = mapped_column(SAEnum(VerificationType), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)

    user: Mapped["User"] = relationship("User", back_populates="verification_codes")
