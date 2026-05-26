from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.core.dependencies import get_db, get_current_user
from app.models.user import User
from app.models.application import ConsultationApplication
from app.schemas.application import ApplicationCreate, ApplicationOut

router = APIRouter(tags=["Applications"])

@router.post("/api/applications", response_model=ApplicationOut)
async def create_application(
    application_in: ApplicationCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    # Check if master exists
    master = await db.get(User, application_in.master_id)
    if not master:
        raise HTTPException(status_code=404, detail="Master not found")
    
    new_app = ConsultationApplication(
        client_id=current_user.id,
        master_id=application_in.master_id,
        contact_info=application_in.contact_info,
        message=application_in.message
    )
    
    db.add(new_app)
    await db.commit()
    await db.refresh(new_app)
    return new_app

@router.get("/api/applications/my", response_model=List[ApplicationOut])
async def get_my_applications(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    # Returns applications sent by client or received by master
    query = select(ConsultationApplication).where(
        (ConsultationApplication.client_id == current_user.id) | 
        (ConsultationApplication.master_id == current_user.id)
    )
    result = await db.execute(query)
    return result.scalars().all()
