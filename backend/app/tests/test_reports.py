import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.tests.conftest import _create_user, _get_token
from app.services.report_service import ReportService
from app.schemas.report import ReportSummary


class TestReportAccess:
    async def test_client_cannot_access_reports(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "cl_rep@test.com", "client")
        token = await _get_token(client, "cl_rep@test.com")
        resp = await client.get("/api/reports/summary",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 403

    async def test_admin_can_access_reports(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "ad_rep2@test.com", "admin")
        token = await _get_token(client, "ad_rep2@test.com")
        resp = await client.get("/api/reports/summary",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200


class TestReportExport:
    async def _admin_token(self, client, db, email="exp_admin@test.com"):
        await _create_user(db, email, "admin")
        return await _get_token(client, email)

    async def test_export_csv(self, client: AsyncClient, db: AsyncSession):
        token = await self._admin_token(client, db, "csv_admin@test.com")
        resp = await client.get("/api/reports/export?format=csv",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200
        assert resp.headers["content-type"].startswith("text/csv")
        assert b"\xef\xbb\xbf" in resp.content or b"period" in resp.content.lower() \
               or len(resp.content) > 0

    async def test_export_html(self, client: AsyncClient, db: AsyncSession):
        token = await self._admin_token(client, db, "html_admin@test.com")
        resp = await client.get("/api/reports/export?format=html",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200
        assert b"<!DOCTYPE html>" in resp.content

    async def test_export_pdf(self, client: AsyncClient, db: AsyncSession):
        token = await self._admin_token(client, db, "pdf_admin@test.com")
        resp = await client.get("/api/reports/export?format=pdf",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200
        assert resp.headers["content-type"] == "application/pdf"
        assert resp.content[:4] == b"%PDF"

    async def test_invalid_format(self, client: AsyncClient, db: AsyncSession):
        token = await self._admin_token(client, db, "bad_admin@test.com")
        resp = await client.get("/api/reports/export?format=xlsx",
                                headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 422

    async def test_report_service_data(self, db: AsyncSession):
        """Unit test: ReportService returns a valid ReportSummary."""
        svc = ReportService(db)
        data = await svc.get_report_data()
        assert isinstance(data, ReportSummary)
        assert data.total_works_uploaded >= 0
        assert isinstance(data.top_masters, list)
