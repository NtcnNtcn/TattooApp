import math
from typing import List

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from sqlalchemy.orm import selectinload

from app.core.dependencies import get_db, require_role
from app.models.user import User
from app.models.tattoo_work import TattooWork, WorkStatus
from app.models.review import WorkReview
from app.schemas.tattoo_work import TattooWorkOut, WorksPage
from app.schemas.review import ReviewAction, WorkReviewOut
from app.services.notification_service import NotificationService

router = APIRouter(prefix="/api/moderation", tags=["Moderation"])


@router.get("/pending", response_model=WorksPage)
async def list_pending(
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db),
):
    """List all works awaiting moderation (admin+ only) with pagination."""
    query = (
        select(TattooWork)
        .options(selectinload(TattooWork.tags))
        .where(TattooWork.status == WorkStatus.pending)
        .order_by(TattooWork.created_at.asc())
    )
    
    # Count total
    count_query = select(func.count()).select_from(query.subquery())
    total_result = await db.execute(count_query)
    total = total_result.scalar_one()
    
    # Paginate
    offset = (page - 1) * size
    result = await db.execute(query.offset(offset).limit(size))
    items = result.scalars().all()
    
    return WorksPage(
        items=items,
        total=total,
        page=page,
        size=size,
        pages=math.ceil(total / size) if total else 0,
    )


@router.post("/{work_id}/approve", response_model=WorkReviewOut)
async def approve_work(
    work_id: int,
    body: ReviewAction,
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db),
):
    """Approve a pending work (admin+ only). Triggers FCM to master."""
    work = await db.get(TattooWork, work_id)
    if not work:
        raise HTTPException(status_code=404, detail="Work not found")
    if work.status != WorkStatus.pending:
        raise HTTPException(status_code=400, detail=f"Work is already '{work.status}'")

    old_status = work.status
    work.status = WorkStatus.approved
    db.add(work)

    review = WorkReview(
        work_id=work_id,
        reviewer_id=current_user.id,
        old_status=old_status,
        new_status=WorkStatus.approved,
        comment=body.comment,
    )
    db.add(review)
    await db.commit()
    await db.refresh(review)

    # Push notifications
    master = await db.get(User, work.master_id)
    notif = NotificationService(db)
    await notif.notify_work_reviewed(work_id, "approved", master.fcm_token if master else None)
    await notif.notify_subscribers_new_work(work_id, work.master_id)
    await db.commit()

    return review


@router.post("/{work_id}/reject", response_model=WorkReviewOut)
async def reject_work(
    work_id: int,
    body: ReviewAction,
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db),
):
    """Reject a pending work with optional reason (admin+ only)."""
    work = await db.get(TattooWork, work_id)
    if not work:
        raise HTTPException(status_code=404, detail="Work not found")
    if work.status != WorkStatus.pending:
        raise HTTPException(status_code=400, detail=f"Work is already '{work.status}'")

    old_status = work.status
    work.status = WorkStatus.rejected
    work.rejection_reason = body.comment
    db.add(work)

    review = WorkReview(
        work_id=work_id,
        reviewer_id=current_user.id,
        old_status=old_status,
        new_status=WorkStatus.rejected,
        comment=body.comment,
    )
    db.add(review)
    await db.commit()
    await db.refresh(review)

    master = await db.get(User, work.master_id)
    notif = NotificationService(db)
    await notif.notify_work_reviewed(work_id, "rejected", master.fcm_token if master else None)
    await db.commit()

    return review
