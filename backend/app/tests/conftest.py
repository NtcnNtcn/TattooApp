import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession

from app.main import app
from app.db.base import Base
from app.core.dependencies import get_db
from app.core.security import hash_password

# ── In-memory SQLite for tests ────────────────────────────────────────────────
TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"

test_engine = create_async_engine(TEST_DATABASE_URL, echo=False)
TestSessionLocal = async_sessionmaker(
    bind=test_engine, class_=AsyncSession, expire_on_commit=False
)


async def override_get_db():
    async with TestSessionLocal() as session:
        yield session


app.dependency_overrides[get_db] = override_get_db


@pytest_asyncio.fixture(scope="session", autouse=True)
async def setup_database():
    """Create all tables once per test session."""
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest_asyncio.fixture
async def db():
    async with TestSessionLocal() as session:
        yield session


@pytest_asyncio.fixture
async def client() -> AsyncClient:
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        yield ac


# ── Seed helpers ──────────────────────────────────────────────────────────────

async def _seed_roles(db: AsyncSession):
    from sqlalchemy import text
    roles = [("client", 1), ("master", 2), ("admin", 3), ("owner", 4)]
    for name, level in roles:
        existing = await db.execute(text("SELECT id FROM roles WHERE name=:n"), {"n": name})
        if existing.scalar_one_or_none() is None:
            await db.execute(
                text("INSERT INTO roles (name, level) VALUES (:n, :l)"),
                {"n": name, "l": level},
            )
    await db.commit()


async def _create_user(db: AsyncSession, email: str, role_name: str, password: str = "pass123"):
    from sqlalchemy import text
    from app.models.user import User

    await _seed_roles(db)
    role_row = await db.execute(
        text("SELECT id FROM roles WHERE name=:n"), {"n": role_name}
    )
    role_id = role_row.scalar_one()
    user = User(email=email, hashed_password=hash_password(password), full_name="Test User", role_id=role_id, is_verified=True, status="active")
    db.add(user)
    await db.commit()
    await db.refresh(user, ["role"])
    return user


async def _get_token(client: AsyncClient, email: str, password: str = "pass123") -> str:
    resp = await client.post(
        "/api/auth/login",
        data={"username": email, "password": password},
    )
    assert resp.status_code == 200, resp.text
    return resp.json()["access_token"]


@pytest_asyncio.fixture
async def client_user(db):
    return await _create_user(db, "client@test.com", "client")


@pytest_asyncio.fixture
async def master_user(db):
    return await _create_user(db, "master@test.com", "master")


@pytest_asyncio.fixture
async def admin_user(db):
    return await _create_user(db, "admin@test.com", "admin")


@pytest_asyncio.fixture
async def owner_user(db):
    return await _create_user(db, "owner@test.com", "owner")
