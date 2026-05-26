import os
import uuid
from pathlib import Path
from typing import Optional, Dict, Tuple

from fastapi import UploadFile, HTTPException, status

from app.core.config import settings

ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp", "image/gif"}
MAX_BYTES = settings.max_file_size_mb * 1024 * 1024

# Whitelist of allowed extensions
ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "webp", "gif"}

# Magic bytes signatures for validation
MAGIC_BYTES: Dict[str, list] = {
    "image/jpeg": [(b"\xff\xd8\xff\xe0", 0), (b"\xff\xd8\xff\xe1", 0), (b"\xff\xd8\xff\xe8", 0)],
    "image/png": [(b"\x89PNG\r\n\x1a\n", 0)],
    "image/webp": [(b"RIFF", 0), (b"WEBP", 8)],  # RIFF....WEBP
    "image/gif": [(b"GIF87a", 0), (b"GIF89a", 0)],
}


def _validate_magic_bytes(content: bytes, content_type: str) -> bool:
    """Validate file content against known magic bytes signatures."""
    signatures = MAGIC_BYTES.get(content_type, [])
    for signature, offset in signatures:
        if content[offset:offset + len(signature)] == signature:
            return True
    return False


def _get_safe_extension(filename: Optional[str]) -> str:
    """Extract and validate file extension."""
    if not filename:
        return "jpg"
    ext = filename.rsplit(".", 1)[-1].lower() if "." in filename else ""
    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid file extension '.{ext}'. Allowed: {', '.join(ALLOWED_EXTENSIONS)}",
        )
    return ext


async def save_upload(file: UploadFile, subfolder: str = "works") -> str:
    """Validate and save an uploaded image. Returns the relative URL path."""
    # Check content type (can be spoofed, so we also check magic bytes)
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unsupported file type '{file.content_type}'. Use JPEG, PNG, WebP or GIF.",
        )

    content = await file.read()
    
    # Check file size
    if len(content) > MAX_BYTES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"File too large. Maximum allowed size is {settings.max_file_size_mb} MB.",
        )

    # Validate magic bytes (prevents uploading PHP with image content-type)
    if not _validate_magic_bytes(content, file.content_type):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="File content does not match claimed image type.",
        )

    # Validate and get safe extension
    ext = _get_safe_extension(file.filename)
    filename = f"{uuid.uuid4().hex}.{ext}"

    upload_path = Path(settings.upload_dir) / subfolder
    upload_path.mkdir(parents=True, exist_ok=True)

    dest = upload_path / filename
    dest.write_bytes(content)

    return f"/uploads/{subfolder}/{filename}"


def delete_upload(url: str) -> None:
    """Remove a previously saved upload file."""
    import logging
    logger = logging.getLogger(__name__)
    
    # Validate URL format
    if not url.startswith("/uploads/"):
        logger.warning(f"Invalid upload URL format: {url}")
        return
    
    # Extract the relative path and validate it
    rel_path = url[9:]  # Remove "/uploads/" prefix
    
    # Prevent path traversal - only allow basename, no directory traversal
    if ".." in rel_path or rel_path.startswith("/") or "\\" in rel_path:
        logger.warning(f"Path traversal attempt detected: {url}")
        return
    
    # Build full path and verify it's within uploads directory
    upload_root = Path(settings.upload_dir).resolve()
    full_path = (upload_root / rel_path).resolve()
    
    # Security check: ensure the resolved path is within upload directory
    try:
        full_path.relative_to(upload_root)
    except ValueError:
        logger.warning(f"Path escapes upload directory: {url}")
        return
    
    # Only delete if it's a file (not directory)
    if full_path.is_file():
        try:
            full_path.unlink()
            logger.info(f"Deleted upload: {url}")
        except Exception as e:
            logger.error(f"Failed to delete upload {url}: {e}")
    else:
        logger.warning(f"Upload path is not a file: {url}")
