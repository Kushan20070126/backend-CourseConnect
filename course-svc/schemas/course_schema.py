from typing import Literal
from pydantic import BaseModel

class LessonSchema(BaseModel):
    title: str
    mins: int
    video: str = ""
    vname: str = ""
    desc: str = ""

class SectionSchema(BaseModel):
    title: str
    lessons: list[LessonSchema]


class CourseCreate(BaseModel):
    id: int  
    title: str
    summary: str
    instructor: str
    category: str
    level: Literal["Beginner", "Intermediate", "Advanced"] 
    language: str
    duration: str
    rating: float
    price: float
    lessons: int
    students: int
    bestseller: bool
    grades: list[str]
    learn: list[str]
    curriculum: list[str]
    sections: list[SectionSchema]
    created: bool
