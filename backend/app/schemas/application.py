from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional
from enum import Enum

class ApplicationStatus(str, Enum):
    PENDING = "pending"
    ACCEPTED = "accepted"
    REJECTED = "rejected"
    COMPLETED = "completed"

class ApplicationCreate(BaseModel):
    master_id: int
    contact_info: str = Field(..., max_length=255)
    message: Optional[str] = Field(None, max_length=127)

class ApplicationOut(BaseModel):
    id: int
    client_id: int
    master_id: int
    contact_info: str
    message: Optional[str]
    status: ApplicationStatus
    created_at: datetime

    class Config:
        from_attributes = True
