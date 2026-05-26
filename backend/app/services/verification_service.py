import secrets
import string
from typing import Optional
from datetime import datetime, timedelta, timezone
from sqlalchemy import select, delete
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.verification import VerificationCode, VerificationType
from app.models.user import User


async def generate_verification_code(
    db: AsyncSession, 
    user_id: int, 
    email: str, 
    v_type: VerificationType,
    expires_delta: timedelta = timedelta(minutes=10)
) -> str:
    # Delete old codes of same type for this user
    await db.execute(
        delete(VerificationCode)
        .where(VerificationCode.user_id == user_id, VerificationCode.type == v_type)
    )
    
    code = "".join(secrets.choice(string.digits) for _ in range(6))
    expires_at = datetime.now(timezone.utc) + expires_delta
    
    v_code = VerificationCode(
        user_id=user_id,
        email=email,
        code=code,
        type=v_type,
        expires_at=expires_at
    )
    db.add(v_code)
    await db.commit()
    return code


async def verify_code(
    db: AsyncSession,
    email: str,
    code: str,
    v_type: VerificationType
) -> Optional[User]:
    result = await db.execute(
        select(VerificationCode)
        .where(
            VerificationCode.email == email,
            VerificationCode.code == code,
            VerificationCode.type == v_type,
            VerificationCode.expires_at > datetime.now(timezone.utc)
        )
    )
    v_code = result.scalar_one_or_none()
    
    if not v_code:
        return None
    
    # Get user
    result = await db.execute(select(User).where(User.id == v_code.user_id))
    user = result.scalar_one_or_none()
    
    # Delete used code
    await db.delete(v_code)
    await db.commit()
    
    return user


async def send_verification_email(email: str, code: str, v_type: VerificationType):
    from app.core.config import settings
    import asyncio
    
    if settings.use_mock_email:
        print(f"DEBUG: [MOCK] Sending {v_type} code {code} to {email}")
        return

    subject_map = {
        VerificationType.registration: "Подтверждение регистрации",
        VerificationType.email_change: "Подтверждение смены почты",
        VerificationType.password_reset: "Сброс пароля",
    }
    
    subject = subject_map.get(v_type, "Код подтверждения")
    
    # HTML Template
    html_content = f"""
    <html>
        <body style="font-family: sans-serif; padding: 20px; background-color: #f4f4f4;">
            <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 10px; border: 1px solid #ddd;">
                <h2 style="color: #333;">Tattoo Studio</h2>
                <p>Здравствуйте!</p>
                <p>Ваш код для <strong>{subject.lower()}</strong>:</p>
                <div style="background-color: #f0f0f0; padding: 20px; text-align: center; font-size: 32px; letter-spacing: 5px; font-weight: bold; border-radius: 5px; margin: 20px 0;">
                    {code}
                </div>
                <p style="color: #666; font-size: 14px;">Код действителен в течение 10 минут. Если вы не запрашивали этот код, просто проигнорируйте это письмо.</p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                <p style="color: #999; font-size: 12px; text-align: center;">© 2026 Tattoo Studio. Все права защищены.</p>
            </div>
        </body>
    </html>
    """

    def send_smtp():
        import smtplib
        from email.mime.text import MIMEText
        from email.mime.multipart import MIMEMultipart

        msg = MIMEMultipart()
        msg['From'] = settings.smtp_from
        msg['To'] = email
        msg['Subject'] = f"Tattoo Studio: {subject}"
        msg.attach(MIMEText(html_content, 'html'))

        try:
            with smtplib.SMTP(settings.smtp_host, settings.smtp_port) as server:
                server.starttls()
                server.login(settings.smtp_user, settings.smtp_pass)
                server.send_message(msg)
            return True
        except Exception as e:
            import logging
            logging.error(f"Failed to send email to {email}: {e}")
            return False

    # Send in background thread to avoid blocking event loop
    await asyncio.to_thread(send_smtp)


async def cleanup_expired_codes(db: AsyncSession, max_age: timedelta = timedelta(hours=24)) -> int:
    """Delete all verification codes older than max_age.
    
    Returns:
        Number of deleted records.
    """
    import logging
    logger = logging.getLogger(__name__)
    
    cutoff = datetime.now(timezone.utc) - max_age
    
    result = await db.execute(
        delete(VerificationCode)
        .where(VerificationCode.expires_at < cutoff)
    )
    await db.commit()
    
    deleted_count = result.rowcount
    if deleted_count > 0:
        logger.info(f"Cleaned up {deleted_count} expired verification codes")
    
    return deleted_count
