from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query, File, UploadFile, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update

from app.core.dependencies import get_db, get_current_user, require_role
from app.models.user import User
from app.models.review import Notification
from app.schemas.user import UserOut, UserUpdate, VerifyCodeRequest, UserStatsOut, UsersPage
from app.schemas.review import NotificationOut
from app.services.user_service import UserService

router = APIRouter(prefix="/api/users", tags=["Users"])


def get_user_service(db: AsyncSession = Depends(get_db)) -> UserService:
    """Dependency to get UserService instance."""
    return UserService(db)


@router.get("/me", response_model=UserOut)
async def get_me(
    current_user: User = Depends(get_current_user),
    user_service: UserService = Depends(get_user_service)
):
    """Get current user profile with statistics."""
    user = await user_service.get_current_user_with_stats(current_user.id)
    stats = await user_service.get_user_stats(current_user.id)
    
    user_out = UserOut.model_validate(user)
    user_out.stats = stats
    return user_out


@router.patch("/me", response_model=UserOut)
async def update_me(
    payload: UserUpdate,
    current_user: User = Depends(get_current_user),
    user_service: UserService = Depends(get_user_service)
):
    """Update current user profile."""
    user = await user_service.update_profile(current_user, payload)
    return user


@router.post("/me/avatar", response_model=UserOut)
async def upload_avatar(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    user_service: UserService = Depends(get_user_service)
):
    """Upload and set avatar for current user."""
    user = await user_service.upload_avatar(current_user, file)
    return user


@router.post("/me/verify-email-change", status_code=status.HTTP_204_NO_CONTENT)
async def verify_email_change(
    payload: VerifyCodeRequest,
    current_user: User = Depends(get_current_user),
    user_service: UserService = Depends(get_user_service)
):
    """Verify and update user email."""
    await user_service.verify_email_change(current_user, payload.email, payload.code)


@router.get("/me/notifications", response_model=List[NotificationOut])
async def get_notifications(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Get current user notifications."""
    result = await db.execute(
        select(Notification)
        .where(Notification.user_id == current_user.id)
        .order_by(Notification.created_at.desc())
        .limit(50)
    )
    return result.scalars().all()


@router.post("/me/notifications/read-all", status_code=204)
async def mark_all_read(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Mark all notifications as read."""
    await db.execute(
        update(Notification)
        .where(Notification.user_id == current_user.id, Notification.is_read == False)
        .values(is_read=True)
    )
    await db.commit()


@router.get("/{user_id}", response_model=UserOut)
async def get_user(
    user_id: int,
    user_service: UserService = Depends(get_user_service)
):
    """Get user by ID with statistics."""
    user = await user_service.get_user_with_stats(user_id)
    stats = await user_service.get_user_stats(user_id)
    
    user_out = UserOut.model_validate(user)
    user_out.stats = stats
    return user_out


@router.get("", response_model=UsersPage)
async def list_users(
    role: Optional[str] = Query(None),
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    current_user: User = Depends(require_role("admin")),
    user_service: UserService = Depends(get_user_service)
):
    """List all users (admin+ only) with pagination."""
    users, total = await user_service.list_users(role=role, page=page, size=size)
    
    return UsersPage(
        items=users,
        total=total,
        page=page,
        size=size,
        pages=(total + size - 1) // size if total else 0,
    )


@router.patch("/{user_id}/status", response_model=UserOut)
async def update_user_status(
    user_id: int,
    status_val: str,
    current_user: User = Depends(require_role("admin")),
    user_service: UserService = Depends(get_user_service)
):
    """Update a user's status (admin+ only)."""
    user = await user_service.update_user_status(user_id, status_val, current_user)
    return user


@router.delete("/me", status_code=status.HTTP_204_NO_CONTENT)
async def delete_me(
    current_user: User = Depends(get_current_user),
    user_service: UserService = Depends(get_user_service)
):
    """Delete current user account."""
    await user_service.delete_account(current_user)
