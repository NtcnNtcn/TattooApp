import logging
from typing import Optional

import httpx

from app.core.config import settings

logger = logging.getLogger(__name__)

FCM_URL = "https://fcm.googleapis.com/fcm/send"


async def send_push_notification(
    fcm_token: str,
    title: str,
    body: str,
    data: Optional[dict] = None,
) -> bool:
    """Send a push notification via Firebase Cloud Messaging Legacy HTTP API."""
    if not settings.fcm_api_key:
        logger.warning("FCM_API_KEY not configured – skipping push notification.")
        return False

    payload = {
        "to": fcm_token,
        "notification": {"title": title, "body": body},
        "data": data or {},
    }
    headers = {
        "Authorization": f"key={settings.fcm_api_key}",
        "Content-Type": "application/json",
    }

    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.post(FCM_URL, json=payload, headers=headers)
            if resp.status_code == 200:
                logger.info("Push sent to token %s…", fcm_token[:20])
                return True
            else:
                logger.error("FCM error %s: %s", resp.status_code, resp.text)
                return False
    except Exception as exc:
        logger.error("FCM request failed: %s", exc)
        return False


class NotificationService:
    def __init__(self, db):
        self.db = db

    async def notify_work_reviewed(
        self, work_id: int, new_status: str, master_fcm_token: Optional[str]
    ) -> None:
        """Push to master when their work is approved or rejected."""
        status_text = "одобрена ✅" if new_status == "approved" else "отклонена ❌"
        title = "Результат проверки работы"
        body = f"Ваша работа #{work_id} была {status_text}"

        if master_fcm_token:
            await send_push_notification(
                master_fcm_token,
                title,
                body,
                data={"work_id": str(work_id), "status": new_status},
            )

        # Persist in-app notification
        from sqlalchemy import text
        from app.models.tattoo_work import TattooWork
        from sqlalchemy import select

        result = await self.db.execute(
            select(TattooWork).where(TattooWork.id == work_id)
        )
        work = result.scalar_one_or_none()
        if work:
            await self.db.execute(
                text(
                    "INSERT INTO notifications (user_id, type, title, message, is_read)"
                    " VALUES (:uid, :type, :title, :msg, false)"
                ),
                {
                    "uid": work.master_id,
                    "type": f"work_{new_status}",
                    "title": title,
                    "msg": body,
                },
            )

    async def notify_admin_new_work(self, work_id: int) -> None:
        """Push to all admin users when a new work arrives."""
        from sqlalchemy import text

        admins = await self.db.execute(
            text("""
                SELECT u.id, u.fcm_token FROM users u
                JOIN roles r ON r.id = u.role_id
                WHERE r.level >= 3 AND u.status = 'active'
            """)
        )
        for row in admins.mappings().all():
            title = "Новая работа на проверке"
            body = f"Работа #{work_id} ожидает модерации"
            if row["fcm_token"]:
                await send_push_notification(row["fcm_token"], title, body)
            await self.db.execute(
                text(
                    "INSERT INTO notifications (user_id, type, title, message, is_read)"
                    " VALUES (:uid, 'new_pending_work', :title, :msg, false)"
                ),
                {"uid": row["id"], "title": title, "msg": body},
            )

    async def notify_subscribers_new_work(self, work_id: int, master_id: int) -> None:
        """Notify subscribers when a master's work gets approved."""
        from sqlalchemy import text

        subs = await self.db.execute(
            text("""
                SELECT u.id, u.full_name, u.fcm_token
                FROM subscriptions s
                JOIN users u ON u.id = s.subscriber_id
                WHERE s.master_id = :master_id AND u.status = 'active'
            """),
            {"master_id": master_id},
        )
        master_row = await self.db.execute(
            text("SELECT full_name FROM users WHERE id = :id"), {"id": master_id}
        )
        master_name = (master_row.scalar_one_or_none() or "Мастер")

        for row in subs.mappings().all():
            title = f"Новая работа от {master_name}"
            body = f"Мастер {master_name} опубликовал новую работу!"
            if row["fcm_token"]:
                await send_push_notification(
                    row["fcm_token"], title, body, data={"work_id": str(work_id)}
                )
            await self.db.execute(
                text(
                    "INSERT INTO notifications (user_id, type, title, message, is_read)"
                    " VALUES (:uid, 'new_work_subscription', :title, :msg, false)"
                ),
                {"uid": row["id"], "title": title, "msg": body},
            )
