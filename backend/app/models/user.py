import enum
from datetime import datetime, timezone
from typing import Optional, List

from sqlalchemy import Integer, String, DateTime, ForeignKey, Text, Enum as SAEnum
from sqlalchemy.orm import mapped_column, MappedColumn, relationship, Mapped

from app.db.base import Base


class RoleName(str, enum.Enum):
    client = "client"
    master = "master"
    admin = "admin"


class UserStatus(str, enum.Enum):
    active = "active"
    frozen = "frozen"
    pending_deletion = "pending_deletion"


class Role(Base):
    __tablename__ = "roles"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    name: Mapped[str] = mapped_column(String(20), unique=True, nullable=False)
    level: Mapped[int] = mapped_column(Integer, nullable=False)  # 1=client,2=master,3=admin

    users: Mapped[List["User"]] = relationship("User", back_populates="role")

    def __repr__(self) -> str:
        return f"<Role {self.name}>"


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True, nullable=False)
    hashed_password: Mapped[str] = mapped_column(String(255), nullable=False)
    full_name: Mapped[str] = mapped_column(String(255), nullable=False)
    avatar_url: Mapped[Optional[str]] = mapped_column(String(500), nullable=True)
    fcm_token: Mapped[Optional[str]] = mapped_column(String(500), nullable=True)
    is_verified: Mapped[bool] = mapped_column(default=False, nullable=False)
    status: Mapped[str] = mapped_column(String(50), default=UserStatus.active.value, nullable=False)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    role_id: Mapped[int] = mapped_column(Integer, ForeignKey("roles.id"), nullable=False)
    role: Mapped["Role"] = relationship("Role", back_populates="users")

    # Relationships
    tattoo_works: Mapped[List["TattooWork"]] = relationship(  # type: ignore[name-defined]
        "TattooWork", back_populates="master", foreign_keys="TattooWork.master_id"
    )
    favorites: Mapped[List["Favorite"]] = relationship("Favorite", back_populates="user")  # type: ignore[name-defined]
    likes: Mapped[List["Like"]] = relationship("Like", back_populates="user")  # type: ignore[name-defined]
    subscriptions: Mapped[List["Subscription"]] = relationship(  # type: ignore[name-defined]
        "Subscription", back_populates="subscriber", foreign_keys="Subscription.subscriber_id"
    )
    subscribers: Mapped[List["Subscription"]] = relationship(  # type: ignore[name-defined]
        "Subscription", back_populates="master", foreign_keys="Subscription.master_id"
    )
    notifications: Mapped[List["Notification"]] = relationship("Notification", back_populates="user")  # type: ignore[name-defined]
    work_reviews: Mapped[List["WorkReview"]] = relationship("WorkReview", back_populates="reviewer")  # type: ignore[name-defined]
    
    consultation_requests: Mapped[List["ConsultationApplication"]] = relationship(  # type: ignore[name-defined]
        "ConsultationApplication", back_populates="client", foreign_keys="ConsultationApplication.client_id"
    )
    received_consultation_requests: Mapped[List["ConsultationApplication"]] = relationship(  # type: ignore[name-defined]
        "ConsultationApplication", back_populates="master", foreign_keys="ConsultationApplication.master_id"
    )
    
    verification_codes: Mapped[List["VerificationCode"]] = relationship("VerificationCode", back_populates="user", cascade="all, delete-orphan")  # type: ignore[name-defined]
    master_application: Mapped[Optional["MasterApplication"]] = relationship("MasterApplication", back_populates="master", uselist=False, cascade="all, delete-orphan")  # type: ignore[name-defined]

    def __repr__(self) -> str:
        return f"<User {self.email}>"
