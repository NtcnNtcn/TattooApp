from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_db, get_current_user
from app.models.user import User
from app.schemas.user import (
    UserRegister, UserOut, TokenPair, RefreshRequest,
    FCMTokenUpdate, VerifyCodeRequest, ResendCodeRequest,
    ForgotPasswordRequest, ResetPasswordRequest
)
from app.services.auth_service import AuthService

router = APIRouter(prefix="/api/auth", tags=["Auth"])


def get_auth_service(db: AsyncSession = Depends(get_db)) -> AuthService:
    """Dependency to get AuthService instance."""
    return AuthService(db)


@router.post("/register", response_model=UserOut, status_code=status.HTTP_201_CREATED)
async def register(
    payload: UserRegister,
    auth_service: AuthService = Depends(get_auth_service)
):
    """Register new user."""
    user, _ = await auth_service.register(payload)
    return user


@router.post("/verify-code", response_model=Optional[TokenPair])
async def verify_auth_code(
    payload: VerifyCodeRequest,
    auth_service: AuthService = Depends(get_auth_service)
):
    """Verify registration code."""
    return await auth_service.verify_registration(payload.email, payload.code)


@router.post("/resend-code", status_code=status.HTTP_204_NO_CONTENT)
async def resend_auth_code(
    payload: ResendCodeRequest,
    auth_service: AuthService = Depends(get_auth_service)
):
    """Resend verification code."""
    await auth_service.resend_verification(payload.email, payload.type)


@router.post("/login", response_model=TokenPair)
async def login(
    form_data: OAuth2PasswordRequestForm = Depends(),
    auth_service: AuthService = Depends(get_auth_service)
):
    """Login user."""
    return await auth_service.login(form_data.username, form_data.password)


@router.post("/refresh", response_model=TokenPair)
async def refresh_token(
    payload: RefreshRequest,
    auth_service: AuthService = Depends(get_auth_service)
):
    """Refresh access token."""
    return await auth_service.refresh_tokens(payload.refresh_token)


@router.post("/fcm-token", status_code=status.HTTP_204_NO_CONTENT)
async def update_fcm_token(
    payload: FCMTokenUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Update FCM token for push notifications."""
    current_user.fcm_token = payload.fcm_token
    db.add(current_user)
    await db.commit()


@router.post("/forgot-password", status_code=status.HTTP_204_NO_CONTENT)
async def forgot_password(
    payload: ForgotPasswordRequest,
    auth_service: AuthService = Depends(get_auth_service)
):
    """Request password reset."""
    await auth_service.forgot_password(payload.email)


@router.post("/reset-password", status_code=status.HTTP_204_NO_CONTENT)
async def reset_password(
    payload: ResetPasswordRequest,
    auth_service: AuthService = Depends(get_auth_service)
):
    """Reset password with verification code."""
    await auth_service.reset_password(
        payload.email,
        payload.code,
        payload.new_password
    )
