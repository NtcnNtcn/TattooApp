from typing import List

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func

from app.core.dependencies import get_db
from app.models.tattoo_work import Tag
from app.schemas.tattoo_work import TagOut

router = APIRouter(prefix="/api/tags", tags=["Tags"])


@router.get("", response_model=List[TagOut])
async def list_tags(db: AsyncSession = Depends(get_db)):
    """Return all tags ordered alphabetically."""
    result = await db.execute(select(Tag).order_by(Tag.name))
    return result.scalars().all()


@router.get("/search", response_model=List[TagOut])
async def search_tags(
    q: str = Query(..., min_length=1),
    limit: int = Query(10, ge=1, le=50),
    db: AsyncSession = Depends(get_db),
):
    """Autocomplete: tags whose name contains the query string (case-insensitive)."""
    result = await db.execute(
        select(Tag)
        .where(func.lower(Tag.name).contains(q.lower()))
        .order_by(Tag.name)
        .limit(limit)
    )
    return result.scalars().all()
