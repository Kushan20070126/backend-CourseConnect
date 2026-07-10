import os
import oracledb
from typing import AsyncGenerator

DB_USER = os.getenv("DB_USER", "system")
DB_PASSWORD = os.getenv("DB_PASSWORD", "root")
DB_DSN = os.getenv("DB_DSN", "localhost:1521/FREEPDB1") 

pool = None

async def init_db_pool():

    global pool
    pool = oracledb.create_pool_async(
        user=DB_USER,
        password=DB_PASSWORD,
        dsn=DB_DSN,
        min=2,
        max=10
    )


async def close_db_pool():
    if pool:
        await pool.close()

async def get_db_connection() -> AsyncGenerator[oracledb.AsyncConnection, None]:
    async with pool.acquire() as connection:
        yield connection
