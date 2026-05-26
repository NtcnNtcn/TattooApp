import math
from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query, UploadFile, File, Form, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, and_, desc, asc
from sqlalchemy.orm import selectinload

from app.core.dependencies import get_db, get_current_user, require_role, get_current_user_optional
from app.models.user import User
from app.models.tattoo_work import TattooWork, Tag, work_tags, WorkStatus
from app.models.social import Like, Favorite
from app.schemas.tattoo_work import TattooWorkOut, TattooWorkDetail, TattooWorkFeed, WorksPage
from app.services.file_service import save_upload
from app.services.notification_service import NotificationService

router = APIRouter(prefix="/api/works", tags=["Works"])


@router.get("", response_model=WorksPage)
async def list_works(
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    tags: Optional[str] = Query(None, description="Comma-separated tag names"),
    sort: str = Query("date", enum=["date", "popularity"]),
    master_id: Optional[int] = Query(None),
    current_user: Optional[User] = Depends(get_current_user_optional),
    db: AsyncSession = Depends(get_db),
):
    """Public feed of approved works with pagination, tag filter and sort."""
    query = (
        select(TattooWork)
        .join(TattooWork.master)
        .options(selectinload(TattooWork.tags), selectinload(TattooWork.master))
        .where(
            TattooWork.status == WorkStatus.approved,
            User.status == "active"
        )
    )

    if master_id:
        query = query.where(TattooWork.master_id == master_id)

    if tags:
        tag_list = [t.strip().lower() for t in tags.split(",") if t.strip()]
        
        # Subquery to find works with at least one matching tag and count them
        matches = (
            select(
                work_tags.c.work_id,
                func.count(work_tags.c.tag_id).label("match_count")
            )
            .join(Tag, Tag.id == work_tags.c.tag_id)
            .where(func.lower(Tag.name).in_(tag_list))
            .group_by(work_tags.c.work_id)
            .subquery()
        )
        
        # Join with matches and prioritize by match count
        query = query.join(matches, TattooWork.id == matches.c.work_id)
        query = query.order_by(desc(matches.c.match_count))

    order_col = desc(TattooWork.like_count) if sort == "popularity" else desc(TattooWork.created_at)
    query = query.order_by(order_col)

    # Count total (separate base query to avoid order_by/join side-effects)
    count_query = (
        select(func.count(TattooWork.id))
        .join(TattooWork.master)
        .where(TattooWork.status == WorkStatus.approved, User.status == "active")
    )
    if master_id:
        count_query = count_query.where(TattooWork.master_id == master_id)
    if tags:
        count_query = count_query.join(matches, TattooWork.id == matches.c.work_id)
    total_result = await db.execute(count_query)
    total = total_result.scalar_one()

    # Paginate
    offset = (page - 1) * size
    result = await db.execute(query.offset(offset).limit(size))
    works = result.scalars().all()

    # Determine likes/favorites if user is logged in
    items = []
    liked_ids = set()
    favorited_ids = set()
    
    if current_user:
        work_ids = [w.id for w in works]
        if work_ids:
            likes_res = await db.execute(
                select(Like.work_id).where(Like.work_id.in_(work_ids), Like.user_id == current_user.id)
            )
            liked_ids = set(likes_res.scalars().all())
            
            favs_res = await db.execute(
                select(Favorite.work_id).where(Favorite.work_id.in_(work_ids), Favorite.user_id == current_user.id)
            )
            favorited_ids = set(favs_res.scalars().all())

    for w in works:
        feed_item = TattooWorkFeed.model_validate(w)
        feed_item.is_liked = w.id in liked_ids
        feed_item.is_favorited = w.id in favorited_ids
        feed_item.master_name = w.master.full_name
        feed_item.master_avatar = w.master.avatar_url
        feed_item.master_id = w.master_id
        items.append(feed_item)

    return WorksPage(
        items=items,
        total=total,
        page=page,
        size=size,
        pages=math.ceil(total / size) if total else 0,
    )


@router.get("/{work_id}", response_model=TattooWorkDetail)
async def get_work(
    work_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: Optional[User] = Depends(get_current_user_optional)
):
    result = await db.execute(
        select(TattooWork)
        .options(selectinload(TattooWork.tags), selectinload(TattooWork.master).selectinload(User.role))
        .where(TattooWork.id == work_id, TattooWork.status == WorkStatus.approved)
    )
    work = result.scalar_one_or_none()
    if not work:
        raise HTTPException(status_code=404, detail="Work not found")
    
    # Check if liked/favorited
    is_liked = False
    is_favorited = False
    if current_user:
        like_res = await db.execute(
            select(Like).where(Like.work_id == work_id, Like.user_id == current_user.id)
        )
        is_liked = like_res.scalar_one_or_none() is not None
        
        fav_res = await db.execute(
            select(Favorite).where(Favorite.work_id == work_id, Favorite.user_id == current_user.id)
        )
        is_favorited = fav_res.scalar_one_or_none() is not None

    detail = TattooWorkDetail.model_validate(work)
    detail.is_liked = is_liked
    detail.is_favorited = is_favorited
    return detail


@router.post("", response_model=TattooWorkOut, status_code=status.HTTP_201_CREATED)
async def upload_work(
    description: Optional[str] = Form(None),
    tags: str = Form(""),           # comma-separated tag names
    image: UploadFile = File(...),
    current_user: User = Depends(require_role("master")),
    db: AsyncSession = Depends(get_db),
):
    """Upload a new tattoo work (master+ only). Image is saved locally."""
    image_url = await save_upload(image, subfolder="works")

    # Resolve / create tags
    tag_names = [t.strip().lower() for t in tags.split(",") if t.strip()]
    tag_objects: List[Tag] = []
    for name in tag_names:
        res = await db.execute(select(Tag).where(func.lower(Tag.name) == name))
        tag = res.scalar_one_or_none()
        if not tag:
            tag = Tag(name=name)
            db.add(tag)
            await db.flush()
        tag_objects.append(tag)

    work = TattooWork(
        master_id=current_user.id,
        image_url=image_url,
        description=description,
        status=WorkStatus.pending,
        tags=tag_objects,
    )
    db.add(work)
    await db.commit()
    await db.refresh(work, ["tags"])

    # Notify admins about new pending work
    notif = NotificationService(db)
    await notif.notify_admin_new_work(work.id)
    await db.commit()

    return work


@router.delete("/{work_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_work(
    work_id: int,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(TattooWork).options(selectinload(TattooWork.master).selectinload(User.role))
        .where(TattooWork.id == work_id)
    )
    work = result.scalar_one_or_none()
    if not work:
        raise HTTPException(status_code=404, detail="Work not found")

    role_level = current_user.role.level
    if role_level < 3 and work.master_id != current_user.id:
        raise HTTPException(status_code=403, detail="Cannot delete another master's work")

    from app.services.file_service import delete_upload
    delete_upload(work.image_url)

    await db.delete(work)
    await db.commit()
