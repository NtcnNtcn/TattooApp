import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.tests.conftest import _create_user, _get_token


@pytest.mark.asyncio
async def test_create_application_to_master(
    client: AsyncClient, db: AsyncSession, client_user, master_user
):
    token = await _get_token(client, client_user.email)
    resp = await client.post(
        "/api/applications",
        json={
            "master_id": master_user.id,
            "contact_info": "+7 999 123-45-67",
            "message": "Хочу татуировку",
        },
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert data["master_id"] == master_user.id
    assert data["client_id"] == client_user.id
    assert data["status"] == "pending"


@pytest.mark.asyncio
async def test_create_application_to_client_fails(
    client: AsyncClient, db: AsyncSession, client_user
):
    other_client = await _create_user(db, "other_client@test.com", "client")
    token = await _get_token(client, client_user.email)
    resp = await client.post(
        "/api/applications",
        json={
            "master_id": other_client.id,
            "contact_info": "+7 999 123-45-67",
            "message": "Хочу татуировку",
        },
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 400
    assert "not a master" in resp.json()["detail"].lower()


@pytest.mark.asyncio
async def test_create_application_master_not_found(
    client: AsyncClient, client_user
):
    token = await _get_token(client, client_user.email)
    resp = await client.post(
        "/api/applications",
        json={
            "master_id": 99999,
            "contact_info": "+7 999 123-45-67",
            "message": "Хочу татуировку",
        },
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 404


@pytest.mark.asyncio
async def test_get_my_applications(
    client: AsyncClient, db: AsyncSession, client_user, master_user
):
    token = await _get_token(client, client_user.email)
    # Create an application first
    await client.post(
        "/api/applications",
        json={
            "master_id": master_user.id,
            "contact_info": "+7 999 123-45-67",
            "message": "Хочу татуировку",
        },
        headers={"Authorization": f"Bearer {token}"},
    )

    # Client should see it
    resp = await client.get(
        "/api/applications/my",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) == 1
    assert data[0]["master_id"] == master_user.id

    # Master should also see it
    master_token = await _get_token(client, master_user.email)
    resp = await client.get(
        "/api/applications/my",
        headers={"Authorization": f"Bearer {master_token}"},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) == 1
    assert data[0]["client_id"] == client_user.id
