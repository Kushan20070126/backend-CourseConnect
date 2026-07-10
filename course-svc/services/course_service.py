import json
from fastapi import HTTPException, status
import oracledb
from repositories.course_repo import CourseRepository
from schemas.course_schema import CourseCreate

class CourseService:
    def __init__(self, db: oracledb.AsyncConnection):

        self.repo = CourseRepository(db)

    async def get_all_courses(self) -> list[dict]:
        """Business logic for retrieving and checking all courses."""
        courses = await self.repo.find_all()
        return courses

    async def create_new_course(self, course: CourseCreate) -> dict:
        """Business logic for adding a course with business rules validation."""
        if course.price < 0:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST, 
                detail="Course price cannot be negative."
            )
            

        if not course.learn:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="A course must have at least one intended outcome in 'learn'."
            )


        course_data = course.model_dump()
        return await self.repo.save_to_db(course.id, course_data)

    async def update_course_details(self, course_id: int, course: CourseCreate) -> dict:
        """Business logic to check execution updates."""
        course_data = course.model_dump()
        success = await self.repo.update_in_db(course_id, course_data)
        
        if not success:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, 
                detail=f"Course with ID {course_id} does not exist in Oracle DB."
            )
            
        return {"message": f"Course {course_id} updated successfully", "status": "success"}

    async def remove_course(self, course_id: int) -> dict:
        """Business logic validation for deletion protection loops."""
        success = await self.repo.delete_from_db(course_id)
        
        if not success:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, 
                detail=f"Course with ID {course_id} cannot be deleted because it does not exist."
            )
            
        return {"message": f"Course {course_id} deleted successfully", "status": "success"}
