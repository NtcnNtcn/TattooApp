from datetime import date, datetime, timezone
from typing import Optional

from jinja2 import Environment, BaseLoader

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.schemas.report import ReportSummary, TopMaster

class ReportService:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_report_data(
        self,
        start_date: Optional[date] = None,
        end_date: Optional[date] = None,
    ) -> ReportSummary:
        today = date.today()
        start = start_date or date(today.year, today.month, 1)
        end = end_date or today

        start_dt = datetime(start.year, start.month, start.day, tzinfo=timezone.utc)
        end_dt = datetime(end.year, end.month, end.day, 23, 59, 59, tzinfo=timezone.utc)

        # Works stats
        works_result = await self.db.execute(
            text("""
                SELECT
                    COUNT(*) FILTER (WHERE created_at BETWEEN :s AND :e) AS total_uploaded,
                    COUNT(*) FILTER (WHERE status = 'approved' AND created_at BETWEEN :s AND :e) AS total_approved,
                    COUNT(*) FILTER (WHERE status = 'rejected' AND created_at BETWEEN :s AND :e) AS total_rejected,
                    COUNT(*) FILTER (WHERE status = 'pending' AND created_at BETWEEN :s AND :e) AS total_pending
                FROM tattoo_works
            """),
            {"s": start_dt, "e": end_dt},
        )
        w = works_result.mappings().one()

        # Likes stats
        likes_result = await self.db.execute(
            text("SELECT COUNT(*) FROM likes WHERE created_at BETWEEN :s AND :e"),
            {"s": start_dt, "e": end_dt},
        )
        total_likes = likes_result.scalar_one() or 0

        # New users
        users_result = await self.db.execute(
            text("SELECT COUNT(*) FROM users WHERE created_at BETWEEN :s AND :e"),
            {"s": start_dt, "e": end_dt},
        )
        new_users = users_result.scalar_one() or 0

        # Subscriptions in period
        subs_result = await self.db.execute(
            text("SELECT COUNT(*) FROM subscriptions WHERE created_at BETWEEN :s AND :e"),
            {"s": start_dt, "e": end_dt},
        )
        total_subscriptions = subs_result.scalar_one() or 0

        # Top 5 masters by likes
        top_result = await self.db.execute(
            text("""
                SELECT
                    u.id AS master_id,
                    u.full_name,
                    COALESCE(COUNT(l.id), 0) AS total_likes,
                    COUNT(tw.id) FILTER (WHERE tw.status = 'approved' AND tw.created_at BETWEEN :s AND :e) AS approved_works
                FROM users u
                JOIN roles r ON r.id = u.role_id AND r.level >= 2
                LEFT JOIN tattoo_works tw ON tw.master_id = u.id
                    AND tw.created_at BETWEEN :s AND :e
                LEFT JOIN likes l ON l.work_id = tw.id
                    AND l.created_at BETWEEN :s AND :e
                GROUP BY u.id, u.full_name
                ORDER BY total_likes DESC
                LIMIT 5
            """),
            {"s": start_dt, "e": end_dt},
        )
        top_masters = [
            TopMaster(
                master_id=row.master_id,
                full_name=row.full_name,
                total_likes=int(row.total_likes),
                approved_works=int(row.approved_works),
            )
            for row in top_result.mappings().all()
        ]

        return ReportSummary(
            period_start=start,
            period_end=end,
            total_works_uploaded=int(w["total_uploaded"] or 0),
            total_approved=int(w["total_approved"] or 0),
            total_rejected=int(w["total_rejected"] or 0),
            total_pending=int(w["total_pending"] or 0),
            total_likes=total_likes,
            total_subscriptions=total_subscriptions,
            new_users=new_users,
            top_masters=top_masters,
        )

   