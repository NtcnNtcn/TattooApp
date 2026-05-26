import re
from datetime import datetime
from typing import Optional
from pydantic import BaseModel, EmailStr, field_validator


# ── Role ──────────────────────────────────────────────────────────────────────

class RoleOut(BaseModel):
    id: int
    name: str
    level: int

    model_config = {"from_attributes": True}


# ── User ──────────────────────────────────────────────────────────────────────

class UserRegister(BaseModel):
    email: EmailStr
    password: str
    full_name: str
    role: str = "client"  # only "client" or "master" allowed at registration

    @field_validator("email")
    @classmethod
    def email_length(cls, v: str) -> str:
        if len(v) > 255:
            raise ValueError("Email must be at most 255 characters")
        return v

    @field_validator("password")
    @classmethod
    def password_validation(cls, v: str) -> str:
        if len(v) < 8:
            raise ValueError("Password must be at least 8 characters")
        if len(v) > 255:
            raise ValueError("Password must be at most 255 characters")
        
        if not re.search(r"[A-Z]", v):
            raise ValueError("Password must contain at least one uppercase letter")
        if not re.search(r"[a-z]", v):
            raise ValueError("Password must contain at least one lowercase letter")
        if not re.search(r"[0-9]", v): # User didn't ask for digits but it's common practice, wait, user said "1 special symbol"
            pass
        if not re.search(r'[!@#$%^&*(),.?":{}|<>]', v):
            raise ValueError("Password must contain at least one special character")
            
        return v

    @field_validator("full_name")
    @classmethod
    def name_length(cls, v: str) -> str:
        if len(v) < 2:
            raise ValueError("Name must be at least 2 characters")
        if len(v) > 255:
            raise ValueError("Name must be at most 255 characters")
        return v

    @field_validator("role")
    @classmethod
    def role_must_be_public(cls, v: str) -> str:
        if v not in ("client", "master"):
            raise ValueError("Role must be 'client' or 'master' at registration")
        return v


class UserLogin(BaseModel):
    email: EmailStr
    password: str


class UserStatsOut(BaseModel):
    likes_given: int
    likes_received: int
    favorites_count: int
    works_count: int
    following_count: int
    followers_count: int
    applications_count: int
    favorites_received: int


class UserOut(BaseModel):
    id: int
    email: str
    full_name: str
    avatar_url: Optional[str]
    is_verified: bool
    status: str
    description: Optional[str] = None
    created_at: datetime
    role: RoleOut
    stats: Optional[UserStatsOut] = None

    model_config = {"from_attributes": True}


class UsersPage(BaseModel):
    """Paginated list of users."""
    items: list[UserOut]
    total: int
    page: int
    size: int
    pages: int


class UserPublic(BaseModel):
    """Minimal public profile shown on work cards."""
    id: int
    full_name: str
    avatar_url: Optional[str]
    status: Optional[str] = None
    description: Optional[str] = None
    role: RoleOut

    model_config = {"from_attributes": True}


class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    email: Optional[EmailStr] = None
    avatar_url: Optional[str] = None
    description: Optional[str] = None


class FCMTokenUpdate(BaseModel):
    fcm_token: str


class VerifyCodeRequest(BaseModel):
    email: EmailStr
    code: str
    type: str  # "registration", "email_change", "password_reset"


class ResendCodeRequest(BaseModel):
    email: EmailStr
    type: str


class ForgotPasswordRequest(BaseModel):
    email: EmailStr


class ResetPasswordRequest(BaseModel):
    email: EmailStr
    code: str
    new_password: str


# ── Auth tokens ───────────────────────────────────────────────────────────────

class TokenPair(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RefreshRequest(BaseModel):
    refresh_token: str
