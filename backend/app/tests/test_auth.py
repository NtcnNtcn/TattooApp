import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.tests.conftest import _create_user, _get_token


class TestRegistration:
    async def test_register_client_success(self, client: AsyncClient, db: AsyncSession):
        resp = await client.post("/api/auth/register", json={
            "email": "newclient@test.com",
            "password": "securepass",
            "full_name": "New Client",
            "role": "client",
        })
        assert resp.status_code == 201
        data = resp.json()
        assert data["email"] == "newclient@test.com"
        assert data["role"]["name"] == "client"

    async def test_register_master_success(self, client: AsyncClient, db: AsyncSession):
        resp = await client.post("/api/auth/register", json={
            "email": "newmaster@test.com",
            "password": "securepass",
            "full_name": "New Master",
            "role": "master",
        })
        assert resp.status_code == 201

    async def test_register_duplicate_email(self, client: AsyncClient, db: AsyncSession):
        payload = {"email": "dup@test.com", "password": "pass123", "full_name": "Dup", "role": "client"}
        await client.post("/api/auth/register", json=payload)
        resp = await client.post("/api/auth/register", json=payload)
        assert resp.status_code == 400
        assert "already registered" in resp.json()["detail"]

    async def test_register_invalid_role(self, client: AsyncClient):
        resp = await client.post("/api/auth/register", json={
            "email": "hacker@test.com",
            "password": "pass123",
            "full_name": "Hacker",
            "role": "owner",      # not allowed at registration
        })
        assert resp.status_code == 422

    async def test_register_short_password(self, client: AsyncClient):
        resp = await client.post("/api/auth/register", json={
            "email": "short@test.com",
            "password": "abc",
            "full_name": "Short",
            "role": "client",
        })
        assert resp.status_code == 422


class TestLogin:
    async def test_login_success(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "logintest@test.com", "client", "mypassword")
        resp = await client.post("/api/auth/login", data={
            "username": "logintest@test.com",
            "password": "mypassword",
        })
        assert resp.status_code == 200
        data = resp.json()
        assert "access_token" in data
        assert "refresh_token" in data
        assert data["token_type"] == "bearer"

    async def test_login_wrong_password(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "wrongpass@test.com", "client")
        resp = await client.post("/api/auth/login", data={
            "username": "wrongpass@test.com",
            "password": "badpassword",
        })
        assert resp.status_code == 401

    async def test_login_nonexistent_user(self, client: AsyncClient):
        resp = await client.post("/api/auth/login", data={
            "username": "ghost@test.com",
            "password": "any",
        })
        assert resp.status_code == 401


class TestTokenRefresh:
    async def test_refresh_success(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "refresh@test.com", "client")
        login_resp = await client.post("/api/auth/login", data={
            "username": "refresh@test.com",
            "password": "pass123",
        })
        refresh_token = login_resp.json()["refresh_token"]

        resp = await client.post("/api/auth/refresh", json={"refresh_token": refresh_token})
        assert resp.status_code == 200
        assert "access_token" in resp.json()

    async def test_refresh_invalid_token(self, client: AsyncClient):
        resp = await client.post("/api/auth/refresh", json={"refresh_token": "not.a.token"})
        assert resp.status_code == 401


class TestProtectedEndpoints:
    async def test_get_me_requires_auth(self, client: AsyncClient):
        resp = await client.get("/api/users/me")
        assert resp.status_code == 401

    async def test_get_me_success(self, client: AsyncClient, db: AsyncSession):
        await _create_user(db, "me@test.com", "master")
        token = await _get_token(client, "me@test.com")
        resp = await client.get("/api/users/me", headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200
        assert resp.json()["email"] == "me@test.com"
