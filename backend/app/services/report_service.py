import csv
import io
import os
from datetime import date, datetime, timezone
from typing import Optional

from jinja2 import Environment, BaseLoader
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.platypus import (
    SimpleDocTemplate, Table, TableStyle, Paragraph, Spacer
)
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.schemas.report import ReportSummary, TopMaster

# ── HTML template ──────────────────────────────────────────────────────────────
_HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="UTF-8">
<title>Отчёт тату-студии {{ period_start }} – {{ period_end }}</title>
<style>
  body { font-family: Arial, sans-serif; padding: 2rem; color: #222; }
  h1 { color: #2c2c2c; }
  table { border-collapse: collapse; width: 100%; margin-top: 1rem; }
  th { background: #3a3a3a; color: #fff; padding: 8px 12px; text-align: left; }
  td { padding: 8px 12px; border-bottom: 1px solid #ddd; }
  tr:nth-child(even) { background: #f7f7f7; }
  .stat { display: inline-block; margin: 0.5rem 1rem 0.5rem 0; }
  .stat-value { font-size: 2rem; font-weight: bold; color: #c0392b; }
  .stat-label { font-size: 0.85rem; color: #555; }
</style>
</head>
<body>
<h1>Отчёт тату-студии</h1>
<p>Период: <strong>{{ period_start }}</strong> — <strong>{{ period_end }}</strong></p>

<h2>Общая статистика</h2>
<div>
  <div class="stat"><div class="stat-value">{{ total_works_uploaded }}</div><div class="stat-label">Загружено работ</div></div>
  <div class="stat"><div class="stat-value">{{ total_approved }}</div><div class="stat-label">Одобрено</div></div>
  <div class="stat"><div class="stat-value">{{ total_rejected }}</div><div class="stat-label">Отклонено</div></div>
  <div class="stat"><div class="stat-value">{{ total_pending }}</div><div class="stat-label">На проверке</div></div>
  <div class="stat"><div class="stat-value">{{ total_likes }}</div><div class="stat-label">Лайков</div></div>
  <div class="stat"><div class="stat-value">{{ new_users }}</div><div class="stat-label">Новых пользователей</div></div>
</div>

<h2>Топ мастеров по лайкам</h2>
<table>
  <tr><th>#</th><th>Мастер</th><th>Лайков</th><th>Одобренных работ</th></tr>
  {% for i, m in masters %}
  <tr>
    <td>{{ i }}</td>
    <td>{{ m.full_name }}</td>
    <td>{{ m.total_likes }}</td>
    <td>{{ m.approved_works }}</td>
  </tr>
  {% endfor %}
</table>
</body>
</html>
"""

_jinja_env = Environment(loader=BaseLoader())


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

        # Subscriptions total
        subs_result = await self.db.execute(text("SELECT COUNT(*) FROM subscriptions"))
        total_subscriptions = subs_result.scalar_one() or 0

        # Top 5 masters by likes
        top_result = await self.db.execute(
            text("""
                SELECT
                    u.id AS master_id,
                    u.full_name,
                    COALESCE(SUM(tw.like_count), 0) AS total_likes,
                    COUNT(tw.id) FILTER (WHERE tw.status = 'approved') AS approved_works
                FROM users u
                JOIN roles r ON r.id = u.role_id AND r.level >= 2
                LEFT JOIN tattoo_works tw ON tw.master_id = u.id
                    AND tw.created_at BETWEEN :s AND :e
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

    # ── CSV ───────────────────────────────────────────────────────────────────
    async def export_csv(self, data: ReportSummary) -> bytes:
        buf = io.StringIO()
        writer = csv.writer(buf)

        writer.writerow(["Отчёт тату-студии"])
        writer.writerow(["Период", f"{data.period_start} – {data.period_end}"])
        writer.writerow([])
        writer.writerow(["Метрика", "Значение"])
        writer.writerow(["Загружено работ", data.total_works_uploaded])
        writer.writerow(["Одобрено", data.total_approved])
        writer.writerow(["Отклонено", data.total_rejected])
        writer.writerow(["На проверке", data.total_pending])
        writer.writerow(["Лайков", data.total_likes])
        writer.writerow(["Новых пользователей", data.new_users])
        writer.writerow(["Подписок всего", data.total_subscriptions])
        writer.writerow([])
        writer.writerow(["Топ мастеров"])
        writer.writerow(["#", "Имя", "Лайков", "Одобренных работ"])
        for i, m in enumerate(data.top_masters, 1):
            writer.writerow([i, m.full_name, m.total_likes, m.approved_works])

        return buf.getvalue().encode("utf-8-sig")

    # ── HTML ──────────────────────────────────────────────────────────────────
    async def export_html(self, data: ReportSummary) -> bytes:
        tmpl = _jinja_env.from_string(_HTML_TEMPLATE)
        html = tmpl.render(
            period_start=data.period_start,
            period_end=data.period_end,
            total_works_uploaded=data.total_works_uploaded,
            total_approved=data.total_approved,
            total_rejected=data.total_rejected,
            total_pending=data.total_pending,
            total_likes=data.total_likes,
            new_users=data.new_users,
            masters=list(enumerate(data.top_masters, 1)),
        )
        return html.encode("utf-8")

    # ── PDF ───────────────────────────────────────────────────────────────────
    async def export_pdf(self, data: ReportSummary) -> bytes:
        buf = io.BytesIO()
        doc = SimpleDocTemplate(
            buf,
            pagesize=A4,
            rightMargin=2 * cm,
            leftMargin=2 * cm,
            topMargin=2 * cm,
            bottomMargin=2 * cm,
        )
        styles = getSampleStyleSheet()
        title_style = ParagraphStyle(
            "Title", parent=styles["Title"], fontSize=18, textColor=colors.HexColor("#2c2c2c")
        )
        heading_style = ParagraphStyle(
            "Heading2", parent=styles["Heading2"], fontSize=13, textColor=colors.HexColor("#c0392b")
        )

        story = []
        story.append(Paragraph("Отчёт тату-студии", title_style))
        story.append(Spacer(1, 0.3 * cm))
        story.append(
            Paragraph(f"Период: {data.period_start} — {data.period_end}", styles["Normal"])
        )
        story.append(Spacer(1, 0.5 * cm))

        story.append(Paragraph("Общая статистика", heading_style))
        story.append(Spacer(1, 0.2 * cm))

        stats_data = [
            ["Метрика", "Значение"],
            ["Загружено работ", str(data.total_works_uploaded)],
            ["Одобрено", str(data.total_approved)],
            ["Отклонено", str(data.total_rejected)],
            ["На проверке", str(data.total_pending)],
            ["Лайков", str(data.total_likes)],
            ["Новых пользователей", str(data.new_users)],
            ["Подписок всего", str(data.total_subscriptions)],
        ]
        stats_table = Table(stats_data, colWidths=[10 * cm, 5 * cm])
        stats_table.setStyle(
            TableStyle([
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#3a3a3a")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f7f7f7")]),
                ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#cccccc")),
                ("PADDING", (0, 0), (-1, -1), 6),
            ])
        )
        story.append(stats_table)
        story.append(Spacer(1, 0.6 * cm))

        story.append(Paragraph("Топ мастеров по лайкам", heading_style))
        story.append(Spacer(1, 0.2 * cm))

        top_data = [["#", "Мастер", "Лайков", "Одобренных работ"]]
        for i, m in enumerate(data.top_masters, 1):
            top_data.append([str(i), m.full_name, str(m.total_likes), str(m.approved_works)])

        top_table = Table(top_data, colWidths=[1.5 * cm, 8 * cm, 3 * cm, 4.5 * cm])
        top_table.setStyle(
            TableStyle([
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#3a3a3a")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f7f7f7")]),
                ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#cccccc")),
                ("PADDING", (0, 0), (-1, -1), 6),
            ])
        )
        story.append(top_table)

        doc.build(story)
        return buf.getvalue()
