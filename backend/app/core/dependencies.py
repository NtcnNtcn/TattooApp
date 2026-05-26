from typing import AsyncGenerator

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.core.security import decode_token
from app.db.session import AsyncSessionLocal

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/auth/login")
oauth2_scheme_optional = OAuth2PasswordBearer(tokenUrl="/api/auth/login", auto_error=False)

# Role hierarchy: level 1=client, 2=master, 3=admin, 4=owner
ROLE_LEVELS = {
    "client": 1,
    "master": 2,
    "admin": 3,
    "owner": 4,
}


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    async with AsyncSessionLocal() as session:
        try:
            yield session
        finally:
            await session.close()


async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: AsyncSession = Depends(get_db),
):
    from app.models.user import User
    from sqlalchemy.orm import selectinload

    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )

    payload = decode_token(token)
    if payload is None or payload.get("type") != "access":
        raise credentials_exception

    user_id: int = payload.get("sub")
    if user_id is None:
        raise credentials_exception

    result = await db.execute(
        select(User).where(User.id == int(user_id)).options(selectinload(User.role))
    )
    user = result.scalar_one_or_none()

    if user is None or not user.is_active:
        raise credentials_exception

    return user


def require_role(min_role: str):
    """Dependency factory that enforces a minimum role level."""
    min_level = ROLE_LEVELS[min_role]

    async def role_checker(current_user=Depends(get_current_user)):
        if ROLE_LEVELS.get(current_user.role.name, 0) < min_level:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Requires at least '{min_role}' role",
            )
        return current_user

    return role_checker


async def get_current_user_optional(
    token: str = Depends(oauth2_scheme_optional),
    db: AsyncSession = Depends(get_db),
):
    if not token:
        return None
    try:
        return await get_current_user(token, db)
    except HTTPException:
        return None
