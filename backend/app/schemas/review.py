from datetime import datetime
from typing import Optional
from pydantic import BaseModel


class ReviewAction(BaseModel):
    comment: Optional[str] = None


class WorkReviewOut(BaseModel):
    id: int
    work_id: int
    reviewer_id: Optional[int]
    old_status: str
    new_status: str
    comment: Optional[str]
    created_at: datetime

    model_config = {"from_attributes": True}


class NotificationOut(BaseModel):
    id: int
    type: str
    title: str
    message: str
    is_read: bool
    created_at: datetime

    model_config = {"from_attributes": True}
