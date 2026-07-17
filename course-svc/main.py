import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
import py_eureka_client.eureka_client as eureka_client

from database import init_db_pool, close_db_pool
from routers import courses

EUREKA_SERVER = os.getenv("EUREKA_SERVER", "http://localhost:8761/eureka")
APP_NAME = "course-svc"
INSTANCE_PORT = int(os.getenv("PORT", 8000))
INSTANCE_HOST = os.getenv("INSTANCE_HOST", "localhost")

@asynccontextmanager
async def lifespan(app: FastAPI):

    await init_db_pool()
    
    print(f"Connecting to Eureka Server at {EUREKA_SERVER}...")
    await eureka_client.init_async(
        eureka_server=EUREKA_SERVER,
        app_name=APP_NAME,
        instance_port=INSTANCE_PORT,
        instance_host=INSTANCE_HOST
    )
    print(f"Successfully registered '{APP_NAME}' instance to Eureka!")
    
    yield  

    print("Deregistering from Eureka Server...")
    await eureka_client.stop_async()
    
    await close_db_pool()

app = FastAPI(title="Course Microservice", lifespan=lifespan)

app.include_router(courses.router)
