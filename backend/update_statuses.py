import asyncio
import os
import random
from sqlalchemy.ext.asyncio import create_async_engine
from sqlalchemy import text
from dotenv import load_dotenv

load_dotenv()

STATUSES = ["active", "frozen", "pending_deletion"]

async def update_master_statuses():
    database_url = os.getenv("DATABASE_URL")
    print(f"Connecting to {database_url}...")
    engine = create_async_engine(database_url)
    try:
        async with engine.begin() as conn:
            # Get master role id
            result = await conn.execute(text("SELECT id FROM roles WHERE name = 'master'"))
            master_role_id = result.scalar_one()
            
            # Get all masters
            result = await conn.execute(text("SELECT id FROM users WHERE role_id = :role_id"), {"role_id": master_role_id})
            masters = result.fetchall()
            
            print(f"Found {len(masters)} masters. Updating statuses...")
            for master in masters:
                new_status = random.choice(STATUSES)
                await conn.execute(
                    text("UPDATE users SET status = :status WHERE id = :id"),
                    {"status": new_status, "id": master.id}
                )
            print("Successfully updated master statuses.")
    except Exception as e:
        print(f"Error: {e}")
    finally:
        await engine.dispose()

if __name__ == "__main__":
    asyncio.run(update_master_statuses())
