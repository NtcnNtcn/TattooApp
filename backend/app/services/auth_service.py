from typing import Optional, Tuple
from sqlalchemy.ext.asyncio import AsyncSession
from fastapi import HTTPException, status

from app.repositories.user_repository import UserRepository
from app.models.user import User, Role, UserStatus
from app.models.application import MasterApplication, ApplicationStatus
from app.models.verification import VerificationType
from app.core.security import hash_password, verify_password, create_access_token, create_refresh_token, decode_token
from app.services.verification_service import generate_verification_code, verify_code, send_verification_email
from app.schemas.user import UserRegister, TokenPair


class AuthService:
    """Service for authentication operations."""
    
    def __init__(self, db: AsyncSession):
        self.db = db
        self.user_repo = UserRepository(db)
    
    async def register(self, payload: UserRegister) -> Tuple[User, str]:
        """
        Register a new user.
        
        Returns:
            Tuple of (created_user, verification_code)
        """
        # Check duplicate email
        if await self.user_repo.email_exists(payload.email):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Email already registered"
            )
        
        # Resolve role
        role = await self.user_repo.get_role_by_name(payload.role)
        if not role:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Role '{payload.role}' not found"
            )
        
        # Create user
        user = User(
            email=payload.email,
            hashed_password=hash_password(payload.password),
            full_name=payload.full_name,
            role_id=role.id,
            is_verified=False,
        )
        
        if payload.role == "master":
            user.status = UserStatus.frozen.value
        
        user = await self.user_repo.create(user)
        
        # Create master application if needed
        if payload.role == "master":
            app = MasterApplication(master_id=user.id, status=ApplicationStatus.PENDING)
            self.db.add(app)
            await self.db.flush()
        
        # Generate verification code
        code = await generate_verification_code(
            self.db, user.id, user.email, VerificationType.registration
        )
        
        # Send email (fire and forget - don't let email failure break registration)
        try:
            await send_verification_email(user.email, code, VerificationType.registration)
        except Exception:
            # Log the error but don't fail registration
            pass
        
        await self.db.commit()
        return user, code
    
    async def verify_registration(self, email: str, code: str) -> Optional[TokenPair]:
        """
        Verify registration code and issue tokens if successful.
        
        Returns:
            TokenPair if verification successful and user is active, None otherwise
        """
        user = await verify_code(self.db, email, code, VerificationType.registration)
        
        if not user:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or expired code"
            )
        
        # Mark user as verified
        user.is_verified = True
        await self.user_repo.update(user)
        await self.db.commit()
        
        # Only return tokens if user is active
        if user.status != UserStatus.active.value:
            return None
        
        return self._create_token_pair(user)
    
    async def login(self, email: str, password: str) -> TokenPair:
        """
        Authenticate user and return tokens.
        
        Raises:
            HTTPException: If credentials invalid, user inactive, or email not verified
        """
        user = await self.user_repo.get_by_email(email)
        
        if not user or not verify_password(password, user.hashed_password):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Incorrect email or password",
                headers={"WWW-Authenticate": "Bearer"},
            )
        
        if user.status != UserStatus.active.value:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Inactive user account"
            )
        
        if not user.is_verified:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Email not verified"
            )
        
        return self._create_token_pair(user)
    
    async def refresh_tokens(self, refresh_token: str) -> TokenPair:
        """
        Refresh access token using refresh token.
        
        Raises:
            HTTPException: If refresh token invalid or user not found/inactive
        """
        token_data = decode_token(refresh_token)
        
        if not token_data or token_data.get("type") != "refresh":
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid refresh token"
            )
        
        user_id = token_data.get("sub")
        user = await self.user_repo.get_by_id(int(user_id))
        
        if not user or user.status != UserStatus.active.value:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User not found or inactive"
            )
        
        return self._create_token_pair(user)
    
    async def forgot_password(self, email: str) -> None:
        """
        Initiate password reset flow.
        Does not reveal if email exists for security.
        """
        user = await self.user_repo.get_by_email(email)
        
        if user:
            code = await generate_verification_code(
                self.db, user.id, user.email, VerificationType.password_reset
            )
            try:
                await send_verification_email(user.email, code, VerificationType.password_reset)
            except Exception:
                pass
            await self.db.commit()
    
    async def reset_password(self, email: str, code: str, new_password: str) -> None:
        """
        Reset password using verification code.
        
        Raises:
            HTTPException: If code invalid or expired
        """
        user = await verify_code(self.db, email, code, VerificationType.password_reset)
        
        if not user:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or expired code"
            )
        
        user.hashed_password = hash_password(new_password)
        user.is_verified = True  # If they can reset password via email, they are verified
        
        await self.user_repo.update(user)
        await self.db.commit()
    
    async def resend_verification(self, email: str, v_type: str) -> None:
        """Resend verification code."""
        user = await self.user_repo.get_by_email(email)
        
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found"
            )
        
        code = await generate_verification_code(
            self.db, user.id, user.email, VerificationType(v_type)
        )
        
        try:
            await send_verification_email(user.email, code, VerificationType(v_type))
        except Exception:
            pass
        
        await self.db.commit()
    
    def _create_token_pair(self, user: User) -> TokenPair:
        """Create access and refresh tokens for user."""
        token_data = {"sub": str(user.id)}
        return TokenPair(
            access_token=create_access_token(token_data),
            refresh_token=create_refresh_token(token_data),
        )
