from contextlib import asynccontextmanager
from fastapi import FastAPI
from database import init_db_pool, close_db_pool
from routers import courses

@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db_pool()
    yield
    await close_db_pool()

app = FastAPI(title="Course microservice", lifespan=lifespan)

app.include_router(courses.router)
