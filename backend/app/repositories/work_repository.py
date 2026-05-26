from typing import Optional, List
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, update
from sqlalchemy.orm import selectinload

from app.models.tattoo_work import TattooWork, WorkStatus, Tag
from app.models.social import Like, Favorite


class WorkRepository:
    """Repository for TattooWork entity operations."""
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def get_by_id(
        self, 
        work_id: int, 
        load_relations: bool = True
    ) -> Optional[TattooWork]:
        """Get work by ID with optional relation loading."""
        query = select(TattooWork).where(TattooWork.id == work_id)
        
        if load_relations:
            query = query.options(
                selectinload(TattooWork.tags),
                selectinload(TattooWork.master)
            )
        
        result = await self.db.execute(query)
        return result.scalar_one_or_none()
    
    async def create(self, work: TattooWork) -> TattooWork:
        """Create new work."""
        self.db.add(work)
        await self.db.flush()
        await self.db.refresh(work, ["tags", "master"])
        return work
    
    async def update(self, work: TattooWork) -> TattooWork:
        """Update work."""
        self.db.add(work)
        await self.db.flush()
        await self.db.refresh(work, ["tags", "master"])
        return work
    
    async def delete(self, work: TattooWork) -> None:
        """Delete work."""
        await self.db.delete(work)
        await self.db.flush()
    
    async def list_works(
        self,
        status: Optional[WorkStatus] = None,
        master_id: Optional[int] = None,
        tags: Optional[List[str]] = None,
        offset: int = 0,
        limit: int = 20,
        sort_by: str = "created_at",
        descending: bool = True
    ) -> tuple[List[TattooWork], int]:
        """List works with filters and pagination."""
        # Build base query
        query = select(TattooWork).options(
            selectinload(TattooWork.tags),
            selectinload(TattooWork.master)
        )
        
        # Apply filters
        if status:
            query = query.where(TattooWork.status == status)
        if master_id:
            query = query.where(TattooWork.master_id == master_id)
        if tags:
            query = query.join(TattooWork.tags).where(Tag.name.in_(tags))
        
        # Count total
        count_query = select(func.count()).select_from(query.subquery())
        total_result = await self.db.execute(count_query)
        total = total_result.scalar_one()
        
        # Apply sorting
        sort_column = getattr(TattooWork, sort_by, TattooWork.created_at)
        if descending:
            query = query.order_by(sort_column.desc())
        else:
            query = query.order_by(sort_column.asc())
        
        # Apply pagination
        query = query.offset(offset).limit(limit)
        result = await self.db.execute(query)
        works = result.scalars().all()
        
        return list(works), total
    
    async def list_pending_works(
        self,
        offset: int = 0,
        limit: int = 20
    ) -> tuple[List[TattooWork], int]:
        """List pending works for moderation."""
        return await self.list_works(
            status=WorkStatus.pending,
            offset=offset,
            limit=limit,
            sort_by="created_at",
            descending=False  # Oldest first for moderation
        )
    
    async def update_status(
        self,
        work_id: int,
        status: WorkStatus,
        rejection_reason: Optional[str] = None
    ) -> Optional[TattooWork]:
        """Update work status."""
        work = await self.get_by_id(work_id, load_relations=False)
        if not work:
            return None
        
        work.status = status
        if rejection_reason is not None:
            work.rejection_reason = rejection_reason
        
        await self.db.flush()
        return work
    
    async def increment_like_count(self, work_id: int) -> None:
        """Increment work like count."""
        await self.db.execute(
            update(TattooWork)
            .where(TattooWork.id == work_id)
            .values(like_count=TattooWork.like_count + 1)
        )
        await self.db.flush()
    
    async def decrement_like_count(self, work_id: int) -> None:
        """Decrement work like count (won't go below 0)."""
        await self.db.execute(
            update(TattooWork)
            .where(TattooWork.id == work_id, TattooWork.like_count > 0)
            .values(like_count=TattooWork.like_count - 1)
        )
        await self.db.flush()
    
    async def user_has_liked(self, user_id: int, work_id: int) -> bool:
        """Check if user has liked a work."""
        result = await self.db.execute(
            select(func.count(Like.id))
            .where(Like.user_id == user_id, Like.work_id == work_id)
        )
        return result.scalar() > 0
    
    async def user_has_favorited(self, user_id: int, work_id: int) -> bool:
        """Check if user has favorited a work."""
        result = await self.db.execute(
            select(func.count(Favorite.id))
            .where(Favorite.user_id == user_id, Favorite.work_id == work_id)
        )
        return result.scalar() > 0
    
    async def get_or_create_tag(self, tag_name: str) -> Tag:
        """Get existing tag or create new one."""
        result = await self.db.execute(
            select(Tag).where(Tag.name == tag_name)
        )
        tag = result.scalar_one_or_none()
        
        if not tag:
            tag = Tag(name=tag_name)
            self.db.add(tag)
            await self.db.flush()
        
        return tag
