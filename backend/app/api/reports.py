from datetime import date
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.responses import Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_db, require_role
from app.models.user import User
from app.schemas.report import ReportSummary
from app.services.report_service import ReportService

router = APIRouter(prefix="/api/reports", tags=["Reports"])


@router.get("/summary", response_model=ReportSummary)
async def get_report_summary(
    start_date: Optional[date] = Query(None),
    end_date: Optional[date] = Query(None),
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db),
):
    """Dashboard statistics for the admin."""
    svc = ReportService(db)
    return await svc.get_report_data(start_date, end_date)


@router.get("/export")
async def export_report(
    format: str = Query("csv", enum=["csv", "html", "pdf"]),
    start_date: Optional[date] = Query(None),
    end_date: Optional[date] = Query(None),
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db),
):
    """Download a report file in the requested format (admin only)."""
    svc = ReportService(db)
    data = await svc.get_report_data(start_date, end_date)

    filename = f"report_{data.period_start}_{data.period_end}"

    if format == "csv":
        content = await svc.export_csv(data)
        return Response(
            content=content,
            media_type="text/csv; charset=utf-8-sig",
            headers={"Content-Disposition": f'attachment; filename="{filename}.csv"'},
        )
    elif format == "html":
        content = await svc.export_html(data)
        return Response(
            content=content,
            media_type="text/html; charset=utf-8",
            headers={"Content-Disposition": f'attachment; filename="{filename}.html"'},
        )
    elif format == "pdf":
        content = await svc.export_pdf(data)
        return Response(
            content=content,
            media_type="application/pdf",
            headers={"Content-Disposition": f'attachment; filename="{filename}.pdf"'},
        )
    else:
        raise HTTPException(status_code=400, detail="Invalid format")
