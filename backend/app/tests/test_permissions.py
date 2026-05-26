import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.tests.conftest import _create_user, _get_token
from app.models.tattoo_work import TattooWork


async def _approved_work(db: AsyncSession, master_id: int) -> TattooWork:
    work = TattooWork(master_id=master_id, image_url="/uploads/works/p.jpg", status="approved")
    db.add(work)
    await db.commit()
    await db.refresh(work)
    return work


class TestRolePermissions:
    """Verify strict role hierarchy across all protected endpoints."""

    async def test_unauthenticated_cannot_upload(self, client: AsyncClient):
        import io
        resp = await client.post("/api/works",
                                 files={"image": ("t.jpg", io.BytesIO(b"data"), "image/jpeg")},
                                 data={"tags": "test"})
        assert resp.status_code == 401

    async def test_client_cannot_upload(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "perm_cl@test.com", "client")
        token = await _get_token(client, "perm_cl@test.com")
        import io
        resp = await client.post("/api/works",
                                 headers={"Authorization": f"Bearer {token}"},
                                 files={"image": ("t.jpg", io.BytesIO(b"data"), "image/jpeg")},
                                 data={"tags": "test"})
        assert resp.status_code == 403

    async def test_client_cannot_moderate(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "perm_cl2@test.com", "client")
        token = await _get_token(client, "perm_cl2@test.com")
        resp = await client.get("/api/moderation/pending",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 403

    async def test_master_cannot_moderate(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "perm_ma@test.com", "master")
        token = await _get_token(client, "perm_ma@test.com")
        resp = await client.get("/api/moderation/pending",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 403

    async def test_master_cannot_view_report(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "perm_ma2@test.com", "master")
        token = await _get_token(client, "perm_ma2@test.com")
        resp = await client.get("/api/reports/summary",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 403

    async def test_admin_can_do_everything(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "perm_ad2@test.com", "admin")
        token = await _get_token(client, "perm_ad2@test.com")
        # moderation
        resp = await client.get("/api/moderation/pending",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200
        # reports
        resp = await client.get("/api/reports/summary",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200


class TestSocialActions:
    async def test_like_approved_work(self, client: AsyncClient, db: AsyncSession):
        master = await _create_user(db, "like_ma@test.com", "master")
        user = await _create_user(db, "like_cl@test.com", "client")
        work = await _approved_work(db, master.id)
        token = await _get_token(client, "like_cl@test.com")

        resp = await client.post(f"/api/works/{work.id}/like",
                                 headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200
        assert resp.json()["active"] is True

        # Unlike
        resp = await client.post(f"/api/works/{work.id}/like",
                                 headers={"Authorization": f"Bearer {token}"})
        assert resp.json()["active"] is False

    async def test_favorite_work(self, client: AsyncClient, db: AsyncSession):
        master = await _create_user(db, "fav_ma@test.com", "master")
        await _create_user(db, "fav_cl@test.com", "client")
        work = await _approved_work(db, master.id)
        token = await _get_token(client, "fav_cl@test.com")

        resp = await client.post(f"/api/works/{work.id}/favorite",
                                 headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200
        assert resp.json()["active"] is True

        # List favorites
        resp = await client.get("/api/favorites",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200
        ids = [w["id"] for w in resp.json()]
        assert work.id in ids

    async def test_subscribe_to_master(self, client: AsyncClient, db: AsyncSession):
        master = await _create_user(db, "sub_ma@test.com", "master")
        await _create_user(db, "sub_cl@test.com", "client")
        token = await _get_token(client, "sub_cl@test.com")

        resp = await client.post(f"/api/users/{master.id}/subscribe",
                                 headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200
        assert resp.json()["active"] is True
