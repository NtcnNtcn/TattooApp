from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from sqlalchemy.orm import selectinload

from app.core.dependencies import get_db, require_role
from app.models.user import User
from app.models.application import MasterApplication, ConsultationApplication, ApplicationStatus
from app.models.tattoo_work import TattooWork, WorkStatus
from pydantic import BaseModel, ConfigDict
from datetime import datetime

router = APIRouter(prefix="/api/admin/applications", tags=["Admin Applications"])

class UserSimpleOut(BaseModel):
    id: int
    email: str
    full_name: str
    avatar_url: str | None = None
    
    model_config = ConfigDict(from_attributes=True)

class MasterApplicationOut(BaseModel):
    id: int
    master_id: int
    status: str
    created_at: datetime
    master: UserSimpleOut
    
    model_config = ConfigDict(from_attributes=True)


class ConsultationApplicationOut(BaseModel):
    id: int
    client_id: int
    master_id: int
    contact_info: str
    message: str | None = None
    status: str
    created_at: datetime
    client: UserSimpleOut
    master: UserSimpleOut

    model_config = ConfigDict(from_attributes=True)

class AdminStatsOut(BaseModel):
    works_approved: int
    works_rejected: int
    consultations_processed: int


@router.get("/stats", response_model=AdminStatsOut)
async def get_admin_stats(
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db)
):
    approved = await db.scalar(
        select(func.count()).where(TattooWork.status == WorkStatus.approved)
    )
    rejected = await db.scalar(
        select(func.count()).where(TattooWork.status == WorkStatus.rejected)
    )
    processed = await db.scalar(
        select(func.count()).where(ConsultationApplication.status == ApplicationStatus.COMPLETED)
    )
    return AdminStatsOut(
        works_approved=approved or 0,
        works_rejected=rejected or 0,
        consultations_processed=processed or 0,
    )


@router.get("/masters", response_model=List[MasterApplicationOut])
async def list_master_applications(
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db)
):
    query = select(MasterApplication).options(selectinload(MasterApplication.master)).where(
        MasterApplication.status == ApplicationStatus.PENDING
    ).order_by(MasterApplication.created_at.desc())
    result = await db.execute(query)
    return result.scalars().all()

@router.get("/consultations", response_model=List[ConsultationApplicationOut])
async def list_consultation_applications(
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db)
):
    query = (
        select(ConsultationApplication)
        .options(
            selectinload(ConsultationApplication.client),
            selectinload(ConsultationApplication.master),
        )
        .order_by(ConsultationApplication.created_at.desc())
    )
    result = await db.execute(query)
    return result.scalars().all()


@router.patch("/consultations/{app_id}/close", status_code=status.HTTP_204_NO_CONTENT)
async def close_consultation_application(
    app_id: int,
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db)
):
    app = await db.get(ConsultationApplication, app_id)
    if not app:
        raise HTTPException(status_code=404, detail="Application not found")
    app.status = ApplicationStatus.COMPLETED
    db.add(app)
    await db.commit()


@router.post("/masters/{app_id}/approve", status_code=status.HTTP_204_NO_CONTENT)
async def approve_master_application(
    app_id: int,
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db)
):
    app = await db.get(MasterApplication, app_id)
    if not app:
        raise HTTPException(status_code=404, detail="Application not found")
    if app.status != ApplicationStatus.PENDING:
        raise HTTPException(status_code=400, detail="Application is not pending")
        
    app.status = ApplicationStatus.ACCEPTED
    
    master = await db.get(User, app.master_id)
    if master:
        master.is_active = True
        db.add(master)
        
    db.add(app)
    await db.commit()

@router.post("/masters/{app_id}/reject", status_code=status.HTTP_204_NO_CONTENT)
async def reject_master_application(
    app_id: int,
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db)
):
    app = await db.get(MasterApplication, app_id)
    if not app:
        raise HTTPException(status_code=404, detail="Application not found")
    if app.status != ApplicationStatus.PENDING:
        raise HTTPException(status_code=400, detail="Application is not pending")
        
    app.status = ApplicationStatus.REJECTED
    db.add(app)
    await db.commit()
