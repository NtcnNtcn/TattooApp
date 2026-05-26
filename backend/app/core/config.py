from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # App
    app_name: str = "TattooStudio API"
    app_version: str = "1.0.0"
    debug: bool = False

    # Database
    database_url: str = "postgresql+asyncpg://tattoo_user:tattoo_pass@localhost:5432/tattoo_db"

    # Security - NO DEFAULTS for secrets
    secret_key: str  # Must be set in .env
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 30
    refresh_token_expire_days: int = 7

    # File Storage
    upload_dir: str = "./uploads"
    max_file_size_mb: int = 10

    # Firebase Cloud Messaging
    fcm_api_key: str = ""
    fcm_project_id: str = ""

    # CORS - comma-separated list of allowed origins
    cors_origins: str = "http://localhost:3000,http://localhost:8000,http://127.0.0.1:3000"

    # SMTP Settings
    smtp_host: str = "smtp.gmail.com"
    smtp_port: int = 587
    smtp_user: str = ""
    smtp_pass: str = ""
    smtp_from: str = "Tattoo Studio <noreply@example.com>"
    use_mock_email: bool = True

    # Default admin credentials - MUST be set in .env for first run
    default_owner_email: str = ""
    default_owner_password: str = ""  # Must be set in .env
    default_admin_email: str = ""
    default_admin_password: str = ""  # Must be set in .env

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
