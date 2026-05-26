import io
import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.tests.conftest import _create_user, _get_token
from app.models.tattoo_work import TattooWork, Tag, WorkStatus


async def _create_work(db: AsyncSession, master_id: int, status: str = "pending") -> TattooWork:
    work = TattooWork(master_id=master_id, image_url="/uploads/works/test.jpg", status=status)
    db.add(work)
    await db.commit()
    await db.refresh(work)
    return work


class TestWorkUpload:
    async def test_upload_requires_master_role(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "client_upload@test.com", "client")
        token = await _get_token(client, "client_upload@test.com")
        img = io.BytesIO(b"fake image data")
        resp = await client.post(
            "/api/works",
            headers={"Authorization": f"Bearer {token}"},
            files={"image": ("test.jpg", img, "image/jpeg")},
            data={"tags": "realism", "description": "My tattoo"},
        )
        assert resp.status_code == 403

    async def test_master_can_upload(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "uploader@test.com", "master")
        token = await _get_token(client, "uploader@test.com")
        # 1×1 JPEG bytes (valid minimal JPEG)
        jpeg_bytes = bytes([
            0xFF,0xD8,0xFF,0xE0,0x00,0x10,0x4A,0x46,0x49,0x46,0x00,0x01,
            0x01,0x00,0x00,0x01,0x00,0x01,0x00,0x00,0xFF,0xDB,0x00,0x43,
            0x00,0x08,0x06,0x06,0x07,0x06,0x05,0x08,0x07,0x07,0x07,0x09,
            0x09,0x08,0x0A,0x0C,0x14,0x0D,0x0C,0x0B,0x0B,0x0C,0x19,0x12,
            0x13,0x0F,0x14,0x1D,0x1A,0x1F,0x1E,0x1D,0x1A,0x1C,0x1C,0x20,
            0x24,0x2E,0x27,0x20,0x22,0x2C,0x23,0x1C,0x1C,0x28,0x37,0x29,
            0x2C,0x30,0x31,0x34,0x34,0x34,0x1F,0x27,0x39,0x3D,0x38,0x32,
            0x3C,0x2E,0x33,0x34,0x32,0xFF,0xC0,0x00,0x0B,0x08,0x00,0x01,
            0x00,0x01,0x01,0x01,0x11,0x00,0xFF,0xC4,0x00,0x1F,0x00,0x00,
            0x01,0x05,0x01,0x01,0x01,0x01,0x01,0x01,0x00,0x00,0x00,0x00,
            0x00,0x00,0x00,0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,
            0x09,0x0A,0x0B,0xFF,0xC4,0x00,0xB5,0x10,0x00,0x02,0x01,0x03,
            0x03,0x02,0x04,0x03,0x05,0x05,0x04,0x04,0x00,0x00,0x01,0x7D,
            0xFF,0xDA,0x00,0x08,0x01,0x01,0x00,0x00,0x3F,0x00,0xFB,0xFF,
            0xD9,
        ])
        resp = await client.post(
            "/api/works",
            headers={"Authorization": f"Bearer {token}"},
            files={"image": ("test.jpg", io.BytesIO(jpeg_bytes), "image/jpeg")},
            data={"tags": "realism,blackwork", "description": "Test work"},
        )
        assert resp.status_code == 201
        data = resp.json()
        assert data["status"] == "pending"
        assert len(data["tags"]) == 2


class TestWorkFeed:
    async def test_approved_works_visible_without_auth(self, client: AsyncClient, db: AsyncSession):
        user = await _create_user(db, "feedmaster@test.com", "master")
        work = await _create_work(db, user.id, status="approved")
        resp = await client.get("/api/works")
        assert resp.status_code == 200
        ids = [w["id"] for w in resp.json()["items"]]
        assert work.id in ids

    async def test_pending_works_not_in_feed(self, client: AsyncClient, db: AsyncSession):
        user = await _create_user(db, "pendmaster@test.com", "master")
        work = await _create_work(db, user.id, status="pending")
        resp = await client.get("/api/works")
        assert resp.status_code == 200
        ids = [w["id"] for w in resp.json()["items"]]
        assert work.id not in ids

    async def test_pagination(self, client: AsyncClient):
        resp = await client.get("/api/works?page=1&size=5")
        assert resp.status_code == 200
        data = resp.json()
        assert "total" in data
        assert "pages" in data
        assert len(data["items"]) <= 5

    async def test_filter_by_tags(self, client: AsyncClient, db: AsyncSession):
        user = await _create_user(db, "tagmaster@test.com", "master")
        tag = Tag(name="watercolor_unique")
        db.add(tag)
        await db.flush()
        work = TattooWork(
            master_id=user.id,
            image_url="/uploads/works/t.jpg",
            status="approved",
            tags=[tag],
        )
        db.add(work)
        await db.commit()

        resp = await client.get("/api/works?tags=watercolor_unique")
        assert resp.status_code == 200
        ids = [w["id"] for w in resp.json()["items"]]
        assert work.id in ids
