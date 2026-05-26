import logging
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text

from app.core.security import hash_password
from app.db.session import AsyncSessionLocal

logger = logging.getLogger(__name__)

DEFAULT_ROLES = [
    {"name": "client", "level": 1},
    {"name": "master", "level": 2},
    {"name": "admin",  "level": 3},
]


async def init_db() -> None:
    """Seed roles and default admin accounts if they don't exist."""
    async with AsyncSessionLocal() as db:
        try:
            await _seed_roles(db)
            await _seed_default_users(db)
            await db.commit()
            logger.info("Database initialised successfully.")
        except Exception as exc:
            await db.rollback()
            logger.error("DB init failed: %s", exc)
            raise


async def _seed_roles(db: AsyncSession) -> None:
    for role_data in DEFAULT_ROLES:
        result = await db.execute(
            text("SELECT id FROM roles WHERE name = :name"),
            {"name": role_data["name"]},
        )
        if result.scalar_one_or_none() is None:
            await db.execute(
                text("INSERT INTO roles (name, level) VALUES (:name, :level)"),
                role_data,
            )
            logger.info("Created role: %s", role_data["name"])


async def _seed_default_users(db: AsyncSession) -> None:
    from app.core.config import settings

    defaults = []

    # Only create admin if credentials are set
    if settings.default_admin_email and settings.default_admin_password:
        defaults.append({
            "email": settings.default_admin_email,
            "password": settings.default_admin_password,
            "full_name": "Studio Admin",
            "role_name": "admin",
        })
    else:
        logger.warning("DEFAULT_ADMIN_EMAIL/PASSWORD not set - skipping admin creation")
    
    if not defaults:
        logger.info("No default users configured - skipping user creation")
        return

    for user_data in defaults:
        existing = await db.execute(
            text("SELECT id FROM users WHERE email = :email"),
            {"email": user_data["email"]},
        )
        if existing.scalar_one_or_none() is not None:
            continue

        role_row = await db.execute(
            text("SELECT id FROM roles WHERE name = :name"),
            {"name": user_data["role_name"]},
        )
        role_id = role_row.scalar_one()

        await db.execute(
            text(
                "INSERT INTO users (email, hashed_password, full_name, role_id, status, is_verified, is_active, created_at)"
                " VALUES (:email, :hashed_password, :full_name, :role_id, 'active', true, true, NOW())"
            ),
            {
                "email": user_data["email"],
                "hashed_password": hash_password(user_data["password"]),
                "full_name": user_data["full_name"],
                "role_id": role_id,
            },
        )
        logger.info("Created default user: %s (%s)", user_data["email"], user_data["role_name"])
