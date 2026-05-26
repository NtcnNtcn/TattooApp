from typing import Optional, List
from sqlalchemy.ext.asyncio import AsyncSession
from fastapi import UploadFile, HTTPException, status

from app.repositories.user_repository import UserRepository
from app.models.user import User, UserStatus
from app.models.verification import VerificationType
from app.services.verification_service import generate_verification_code, verify_code, send_verification_email
from app.services.file_service import save_upload, delete_upload
from app.schemas.user import UserUpdate, UserStatsOut


class UserService:
    """Service for user operations."""
    
    def __init__(self, db: AsyncSession):
        self.db = db
        self.user_repo = UserRepository(db)
    
    async def get_current_user_with_stats(self, user_id: int) -> User:
        """Get current user with statistics."""
        user = await self.user_repo.get_by_id(user_id, load_role=True)
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        return user
    
    async def get_user_with_stats(self, user_id: int) -> User:
        """Get user by ID with statistics."""
        user = await self.user_repo.get_by_id(user_id, load_role=True)
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        return user
    
    async def get_user_stats(self, user_id: int) -> UserStatsOut:
        """Get user statistics."""
        stats_dict = await self.user_repo.get_user_stats(user_id)
        return UserStatsOut(**stats_dict)
    
    async def update_profile(self, user: User, payload: UserUpdate) -> User:
        """Update user profile."""
        if payload.full_name is not None:
            user.full_name = payload.full_name
        
        if payload.email is not None and payload.email != user.email:
            # Check if email is already taken
            if await self.user_repo.email_exists(payload.email):
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Email already registered"
                )
            
            # Send verification code to new email instead of immediate update
            code = await generate_verification_code(
                self.db, user.id, payload.email, VerificationType.email_change
            )
            try:
                await send_verification_email(payload.email, code, VerificationType.email_change)
            except Exception:
                pass
            await self.db.commit()
        
        if payload.avatar_url is not None:
            user.avatar_url = payload.avatar_url
        
        if payload.description is not None:
            user.description = payload.description
        
        user = await self.user_repo.update(user)
        await self.db.commit()
        return user
    
    async def verify_email_change(self, user: User, email: str, code: str) -> None:
        """Verify and apply email change."""
        verified_user = await verify_code(
            self.db, email, code, VerificationType.email_change
        )
        
        if not verified_user or verified_user.id != user.id:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or expired code"
            )
        
        user.email = email
        await self.user_repo.update(user)
        await self.db.commit()
    
    async def upload_avatar(self, user: User, file: UploadFile) -> User:
        """Upload and set user avatar."""
        # Delete old avatar if exists
        if user.avatar_url:
            delete_upload(user.avatar_url)
        
        # Save new avatar
        file_path = await save_upload(file, subfolder="avatars")
        user.avatar_url = file_path
        
        user = await self.user_repo.update(user)
        await self.db.commit()
        return user
    
    async def update_user_status(
        self, 
        user_id: int, 
        status_val: str,
        admin_user: User
    ) -> User:
        """Update user status (admin only)."""
        if status_val not in [s.value for s in UserStatus]:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid status"
            )
        
        user = await self.user_repo.get_by_id(user_id, load_role=True)
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        
        user.status = UserStatus(status_val)
        user = await self.user_repo.update(user)
        await self.db.commit()
        return user
    
    async def delete_account(self, user: User) -> None:
        """Delete user account and cleanup."""
        # Delete avatar if exists
        if user.avatar_url:
            delete_upload(user.avatar_url)
        
        await self.user_repo.delete(user)
        await self.db.commit()
    
    async def list_users(
        self,
        role: Optional[str] = None,
        page: int = 1,
        size: int = 20
    ) -> tuple[List[User], int]:
        """List users with pagination."""
        offset = (page - 1) * size
        return await self.user_repo.list_users(
            role_name=role,
            offset=offset,
            limit=size
        )
