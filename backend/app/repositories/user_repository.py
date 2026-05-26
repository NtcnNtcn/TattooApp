from typing import Optional, List
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from sqlalchemy.orm import selectinload

from app.models.user import User, Role
from app.models.social import Like, Favorite, Subscription
from app.models.tattoo_work import TattooWork, WorkStatus


class UserRepository:
    """Repository for User entity operations."""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def get_by_id(self, user_id: int, load_role: bool = True) -> Optional[User]:
        """Get user by ID."""
        query = select(User).where(User.id == user_id)
        if load_role:
            query = query.options(selectinload(User.role))
        result = await self.db.execute(query)
        return result.scalar_one_or_none()
    
    async def get_by_email(self, email: str, load_role: bool = False) -> Optional[User]:
        """Get user by email."""
        query = select(User).where(User.email == email)
        if load_role:
            query = query.options(selectinload(User.role))
        result = await self.db.execute(query)
        return result.scalar_one_or_none()
    
    async def email_exists(self, email: str) -> bool:
        """Check if email is already registered."""
        result = await self.db.execute(
            select(func.count(User.id)).where(User.email == email)
        )
        return result.scalar() > 0
    
    async def create(self, user: User) -> User:
        """Create new user."""
        self.db.add(user)
        await self.db.flush()
        await self.db.refresh(user, ["role"])
        return user
    
    async def update(self, user: User) -> User:
        """Update user."""
        self.db.add(user)
        await self.db.flush()
        await self.db.refresh(user, ["role"])
        return user
    
    async def delete(self, user: User) -> None:
        """Delete user."""
        await self.db.delete(user)
        await self.db.flush()
    
    async def get_role_by_name(self, role_name: str) -> Optional[Role]:
        """Get role by name."""
        result = await self.db.execute(
            select(Role).where(Role.name == role_name)
        )
        return result.scalar_one_or_none()
    
    async def list_users(
        self, 
        role_name: Optional[str] = None,
        offset: int = 0,
        limit: int = 20
    ) -> tuple[List[User], int]:
        """List users with optional role filter and pagination."""
        # Build base query
        query = select(User).options(selectinload(User.role))
        
        if role_name:
            query = query.join(User.role).where(Role.name == role_name)
        
        # Count total
        count_query = select(func.count()).select_from(query.subquery())
        total_result = await self.db.execute(count_query)
        total = total_result.scalar_one()
        
        # Get paginated results
        query = query.order_by(User.created_at.desc()).offset(offset).limit(limit)
        result = await self.db.execute(query)
        users = result.scalars().all()
        
        return list(users), total
    
    async def get_user_stats(self, user_id: int) -> dict:
        """Get user statistics in a single query."""
        from app.models.application import ConsultationApplication
        
        result = await self.db.execute(
            select(
                func.coalesce(
                    select(func.count(Like.id))
                    .where(Like.user_id == user_id)
                    .scalar_subquery(),
                    0
                ).label("likes_given"),
                func.coalesce(
                    select(func.count(Favorite.id))
                    .where(Favorite.user_id == user_id)
                    .scalar_subquery(),
                    0
                ).label("favorites_count"),
                func.coalesce(
                    select(func.count(TattooWork.id))
                    .where(
                        TattooWork.master_id == user_id,
                        TattooWork.status == WorkStatus.approved
                    )
                    .scalar_subquery(),
                    0
                ).label("works_count"),
                func.coalesce(
                    select(func.sum(TattooWork.like_count))
                    .where(TattooWork.master_id == user_id)
                    .scalar_subquery(),
                    0
                ).label("likes_received"),
                func.coalesce(
                    select(func.count(Subscription.id))
                    .where(Subscription.subscriber_id == user_id)
                    .scalar_subquery(),
                    0
                ).label("following_count"),
                func.coalesce(
                    select(func.count(Subscription.id))
                    .where(Subscription.master_id == user_id)
                    .scalar_subquery(),
                    0
                ).label("followers_count"),
                func.coalesce(
                    select(func.count(ConsultationApplication.id))
                    .where(
                        (ConsultationApplication.client_id == user_id) |
                        (ConsultationApplication.master_id == user_id)
                    )
                    .scalar_subquery(),
                    0
                ).label("applications_count"),
                func.coalesce(
                    select(func.count(Favorite.id))
                    .select_from(Favorite)
                    .join(TattooWork, Favorite.work_id == TattooWork.id)
                    .where(TattooWork.master_id == user_id)
                    .scalar_subquery(),
                    0
                ).label("favorites_received"),
            )
        )
        
        row = result.one()
        return {
            "likes_given": row.likes_given,
            "likes_received": row.likes_received or 0,
            "favorites_count": row.favorites_count,
            "works_count": row.works_count,
            "following_count": row.following_count,
            "followers_count": row.followers_count,
            "applications_count": row.applications_count,
            "favorites_received": row.favorites_received,
        }
