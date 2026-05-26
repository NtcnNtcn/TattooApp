import os
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.core.config import settings
from app.db.session import engine
from app.db.base import Base


@asynccontextmanager
async def lifespan(app: FastAPI):
    # ── Startup ──────────────────────────────────────────────────────────────
    # Create all tables (Alembic handles migrations in prod; this is dev convenience)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    # Seed roles and default users
    from app.db.init_db import init_db
    await init_db()

    # Ensure upload directory exists
    Path(settings.upload_dir).mkdir(parents=True, exist_ok=True)

    yield  # app is running

    # ── Shutdown ─────────────────────────────────────────────────────────────
    await engine.dispose()


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description=(
        "REST API for TattooStudio mobile application. "
        "Supports portfolio management, role-based access, moderation workflow and reporting."
    ),
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# ── CORS ──────────────────────────────────────────────────────────────────────
# Parse comma-separated origins from settings
origins = [origin.strip() for origin in settings.cors_origins.split(",") if origin.strip()]
if not origins:
    origins = ["http://localhost:3000"]  # fallback default

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Static file serving for uploaded images ───────────────────────────────────
uploads_dir = Path(settings.upload_dir)
app.mount("/uploads", StaticFiles(directory=str(uploads_dir)), name="uploads")

# ── Routers ───────────────────────────────────────────────────────────────────
from app.api import auth, works, tags, social, moderation, reports, users, applications, admin  # noqa: E402

app.include_router(auth.router)
app.include_router(works.router)
app.include_router(tags.router)
app.include_router(social.router)
app.include_router(moderation.router)
app.include_router(reports.router)
app.include_router(users.router)
app.include_router(applications.router)
app.include_router(admin.router)


@app.get("/health", tags=["Health"])
async def health_check():
    return {"status": "ok", "version": settings.app_version}
