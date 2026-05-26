import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.tests.conftest import _create_user, _get_token
from app.models.tattoo_work import TattooWork, WorkStatus


async def _pending_work(db: AsyncSession, master_id: int) -> TattooWork:
    work = TattooWork(master_id=master_id, image_url="/uploads/works/mod.jpg", status="pending")
    db.add(work)
    await db.commit()
    await db.refresh(work)
    return work


class TestModerationAccess:
    async def test_client_cannot_see_pending(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "cl_mod@test.com", "client")
        token = await _get_token(client, "cl_mod@test.com")
        resp = await client.get("/api/moderation/pending",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 403

    async def test_master_cannot_see_pending(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "ma_mod@test.com", "master")
        token = await _get_token(client, "ma_mod@test.com")
        resp = await client.get("/api/moderation/pending",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 403

    async def test_admin_can_see_pending(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "ad_mod@test.com", "admin")
        token = await _get_token(client, "ad_mod@test.com")
        resp = await client.get("/api/moderation/pending",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200


class TestApproveWorkflow:
    async def test_approve_pending_work(self, client: AsyncClient, db: AsyncSession):
        master = await _create_user(db, "appr_master@test.com", "master")
        admin = await _create_user(db, "appr_admin@test.com", "admin")
        work = await _pending_work(db, master.id)

        token = await _get_token(client, "appr_admin@test.com")
        resp = await client.post(
            f"/api/moderation/{work.id}/approve",
            json={"comment": "Great work!"},
            headers={"Authorization": f"Bearer {token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["new_status"] == "approved"
        assert data["work_id"] == work.id

        # Verify in DB
        await db.refresh(work)
        assert work.status == WorkStatus.approved

    async def test_reject_pending_work(self, client: AsyncClient, db: AsyncSession):
        master = await _create_user(db, "rej_master@test.com", "master")
        await _create_user(db, "rej_admin@test.com", "admin")
        work = await _pending_work(db, master.id)

        token = await _get_token(client, "rej_admin@test.com")
        resp = await client.post(
            f"/api/moderation/{work.id}/reject",
            json={"comment": "Image quality too low"},
            headers={"Authorization": f"Bearer {token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["new_status"] == "rejected"
        assert "Image quality" in data["comment"]

        await db.refresh(work)
        assert work.status == WorkStatus.rejected
        assert work.rejection_reason == "Image quality too low"

    async def test_cannot_approve_already_approved(self, client: AsyncClient, db: AsyncSession):
        master = await _create_user(db, "dbl_master@test.com", "master")
        await _create_user(db, "dbl_admin@test.com", "admin")
        work = await _pending_work(db, master.id)

        token = await _get_token(client, "dbl_admin@test.com")
        await client.post(f"/api/moderation/{work.id}/approve", json={},
                          headers={"Authorization": f"Bearer {token}"})
        resp = await client.post(f"/api/moderation/{work.id}/approve", json={},
                                 headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 400
