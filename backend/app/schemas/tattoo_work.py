from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel
from app.schemas.user import UserPublic


# ── Tag ───────────────────────────────────────────────────────────────────────

class TagOut(BaseModel):
    id: int
    name: str

    model_config = {"from_attributes": True}


# ── TattooWork ────────────────────────────────────────────────────────────────

class TattooWorkCreate(BaseModel):
    description: Optional[str] = None
    tags: List[str] = []   # tag names (created if not exist)


class TattooWorkUpdate(BaseModel):
    description: Optional[str] = None
    tags: Optional[List[str]] = None


class TattooWorkOut(BaseModel):
    id: int
    image_url: str
    description: Optional[str]
    status: str
    like_count: int
    rejection_reason: Optional[str]
    created_at: datetime
    updated_at: datetime
    tags: List[TagOut]

    model_config = {"from_attributes": True}


class TattooWorkDetail(TattooWorkOut):
    """Full detail including master info."""
    master: UserPublic
    is_liked: bool = False
    is_favorited: bool = False

    model_config = {"from_attributes": True}


class TattooWorkFeed(BaseModel):
    """Lightweight feed item."""
    id: int
    image_url: str
    status: str
    like_count: int
    created_at: datetime
    tags: List[TagOut]
    is_liked: bool = False
    is_favorited: bool = False
    master_name: str = "МАСТЕР"
    master_avatar: Optional[str] = None
    master_id: int = 0

    model_config = {"from_attributes": True}


class WorksPage(BaseModel):
    items: List[TattooWorkFeed]
    total: int
    page: int
    size: int
    pages: int
