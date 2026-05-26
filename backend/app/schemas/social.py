from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel


class FavoriteOut(BaseModel):
    id: int
    work_id: int
    created_at: datetime

    model_config = {"from_attributes": True}


class SubscriptionOut(BaseModel):
    id: int
    master_id: int
    created_at: datetime

    model_config = {"from_attributes": True}


class LikeOut(BaseModel):
    id: int
    work_id: int
    created_at: datetime

    model_config = {"from_attributes": True}


class ToggleResponse(BaseModel):
    """Generic response for like/favorite/subscribe toggles."""
    active: bool
    message: str
