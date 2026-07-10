from fastapi import APIRouter, Depends, status
import oracledb
from database import get_db_connection
from schemas.course_schema import CourseCreate
from services.course_service import CourseService

router = APIRouter(prefix="/courses", tags=["Courses"])

@router.post("/", status_code=status.HTTP_201_CREATED)
async def create_course(payload: CourseCreate, db: oracledb.AsyncConnection = Depends(get_db_connection)):
    service = CourseService(db)
    return await service.create_new_course(payload)

@router.get("/", response_model=list[CourseCreate])
async def get_all_courses(db: oracledb.AsyncConnection = Depends(get_db_connection)):
    service = CourseService(db)
    return await service.get_all_courses()

@router.put("/{course_id}")
async def update_course(course_id: int, payload: CourseCreate, db: oracledb.AsyncConnection = Depends(get_db_connection)):
    service = CourseService(db)
    return await service.update_course_details(course_id, payload)

@router.delete("/{course_id}")
async def delete_course(course_id: int, db: oracledb.AsyncConnection = Depends(get_db_connection)):
    service = CourseService(db)
    return await service.remove_course(course_id)
