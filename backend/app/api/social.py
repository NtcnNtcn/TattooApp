from typing import List

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update
from sqlalchemy.orm import selectinload

from app.core.dependencies import get_db, get_current_user
from app.models.user import User
from app.models.tattoo_work import TattooWork, WorkStatus
from app.models.social import Favorite, Like, Subscription
from app.schemas.social import ToggleResponse, FavoriteOut, LikeOut, SubscriptionOut
from app.schemas.tattoo_work import TattooWorkFeed

router = APIRouter(tags=["Social"])


# ── Likes ─────────────────────────────────────────────────────────────────────

@router.post("/api/works/{work_id}/like", response_model=ToggleResponse)
async def toggle_like(
    work_id: int,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    work = await db.get(TattooWork, work_id)
    if not work or work.status != WorkStatus.approved:
        raise HTTPException(status_code=404, detail="Work not found")

    existing = await db.execute(
        select(Like).where(Like.user_id == current_user.id, Like.work_id == work_id)
    )
    like = existing.scalar_one_or_none()

    if like:
        # Remove like — trigger will decrement counter
        await db.delete(like)
        await db.commit()
        return ToggleResponse(active=False, message="Like removed")
    else:
        # Add like — trigger will increment counter
        db.add(Like(user_id=current_user.id, work_id=work_id))
        await db.commit()
        return ToggleResponse(active=True, message="Work liked")


# ── Favorites ─────────────────────────────────────────────────────────────────

@router.post("/api/works/{work_id}/favorite", response_model=ToggleResponse)
async def toggle_favorite(
    work_id: int,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    work = await db.get(TattooWork, work_id)
    if not work:
        raise HTTPException(status_code=404, detail="Work not found")

    existing = await db.execute(
        select(Favorite).where(Favorite.user_id == current_user.id, Favorite.work_id == work_id)
    )
    fav = existing.scalar_one_or_none()

    if fav:
        await db.delete(fav)
        await db.commit()
        return ToggleResponse(active=False, message="Removed from favorites")
    else:
        db.add(Favorite(user_id=current_user.id, work_id=work_id))
        await db.commit()
        return ToggleResponse(active=True, message="Added to favorites")


@router.get("/api/likes", response_model=List[TattooWorkFeed])
async def list_likes(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(TattooWork)
        .join(Like, Like.work_id == TattooWork.id)
        .options(selectinload(TattooWork.tags), selectinload(TattooWork.master))
        .where(Like.user_id == current_user.id)
        .order_by(Like.created_at.desc())
    )
    works = result.scalars().all()
    
    # Populate flags
    items = []
    work_ids = [w.id for w in works]
    if not work_ids:
        return []
        
    liked_ids = set(work_ids) # They are all liked by definition here
    
    favs_res = await db.execute(
        select(Favorite.work_id).where(Favorite.work_id.in_(work_ids), Favorite.user_id == current_user.id)
    )
    favorited_ids = set(favs_res.scalars().all())

    for w in works:
        feed_item = TattooWorkFeed.model_validate(w)
        feed_item.is_liked = True
        feed_item.is_favorited = w.id in favorited_ids
        feed_item.master_name = w.master.full_name
        feed_item.master_avatar = w.master.avatar_url
        items.append(feed_item)
    return items


@router.get("/api/favorites", response_model=List[TattooWorkFeed])
async def list_favorites(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(TattooWork)
        .join(Favorite, Favorite.work_id == TattooWork.id)
        .options(selectinload(TattooWork.tags), selectinload(TattooWork.master))
        .where(Favorite.user_id == current_user.id)
        .order_by(Favorite.created_at.desc())
    )
    works = result.scalars().all()
    
    # Populate flags
    items = []
    work_ids = [w.id for w in works]
    if not work_ids:
        return []
        
    favorited_ids = set(work_ids) # They are all favorited by definition here
    
    likes_res = await db.execute(
        select(Like.work_id).where(Like.work_id.in_(work_ids), Like.user_id == current_user.id)
    )
    liked_ids = set(likes_res.scalars().all())

    for w in works:
        feed_item = TattooWorkFeed.model_validate(w)
        feed_item.is_liked = w.id in liked_ids
        feed_item.is_favorited = True
        feed_item.master_name = w.master.full_name
        feed_item.master_avatar = w.master.avatar_url
        items.append(feed_item)
    return items


# ── Subscriptions ─────────────────────────────────────────────────────────────

@router.post("/api/users/{master_id}/subscribe", response_model=ToggleResponse)
async def toggle_subscription(
    master_id: int,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if master_id == current_user.id:
        raise HTTPException(status_code=400, detail="Cannot subscribe to yourself")

    master = await db.get(User, master_id)
    if not master:
        raise HTTPException(status_code=404, detail="User not found")

    existing = await db.execute(
        select(Subscription).where(
            Subscription.subscriber_id == current_user.id,
            Subscription.master_id == master_id,
        )
    )
    sub = existing.scalar_one_or_none()

    if sub:
        await db.delete(sub)
        await db.commit()
        return ToggleResponse(active=False, message="Unsubscribed")
    else:
        db.add(Subscription(subscriber_id=current_user.id, master_id=master_id))
        await db.commit()
        return ToggleResponse(active=True, message="Subscribed")


@router.get("/api/subscriptions", response_model=List[SubscriptionOut])
async def list_subscriptions(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Subscription)
        .where(Subscription.subscriber_id == current_user.id)
        .order_by(Subscription.created_at.desc())
    )
    return result.scalars().all()
