from datetime import date
from typing import List, Optional
from pydantic import BaseModel


class TopMaster(BaseModel):
    master_id: int
    full_name: str
    total_likes: int
    approved_works: int


class ReportSummary(BaseModel):
    period_start: date
    period_end: date
    total_works_uploaded: int
    total_approved: int
    total_rejected: int
    total_pending: int
    total_likes: int
    total_subscriptions: int
    new_users: int
    top_masters: List[TopMaster]


class ReportExportRequest(BaseModel):
    format: str = "csv"         # csv | html | pdf
    start_date: Optional[date] = None
    end_date: Optional[date] = None
