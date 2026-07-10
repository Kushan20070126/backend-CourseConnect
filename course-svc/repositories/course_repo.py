import json
import oracledb

class CourseRepository:
    def __init__(self, db: oracledb.AsyncConnection):
        self.db = db 

    async def save_to_db(self, course_id: int, course_data: dict) -> dict:
        sql = """
            INSERT INTO courses (
                id, title, summary, instructor, category, level_name, language, 
                duration, rating, price, lessons_count, students, bestseller, 
                grades, learn, curriculum, sections, created
            ) VALUES (
                :1, :2, :3, :4, :5, :6, :7, :8, :9, :10, :11, :12, :13, :14, :15, :16, :17, :18
            )
        """
        async with self.db.cursor() as cursor:
            await cursor.execute(sql, [
                course_id,
                course_data["title"],
                course_data["summary"],
                course_data["instructor"],
                course_data["category"],
                course_data["level"], 
                course_data["language"],
                course_data["duration"],
                course_data["rating"],
                course_data["price"],
                course_data["lessons"],
                course_data["students"],
                int(course_data["bestseller"]), 
                json.dumps(course_data["grades"]),
                json.dumps(course_data["learn"]),
                json.dumps(course_data["curriculum"]),
                json.dumps(course_data["sections"]), 
                int(course_data["created"])
            ])
            await self.db.commit() 
        return course_data

    async def find_all(self) -> list[dict]:
        """Fetches all courses and safely handles JSON/Key field conversions."""
        sql = "SELECT * FROM courses"
        courses_list = []
        
        async with self.db.cursor() as cursor:
            await cursor.execute(sql)

            columns = [col[0].lower() for col in cursor.description]
            
            async for row in cursor:
                row_dict = dict(zip(columns, row))
                

                for json_field in ["grades", "learn", "curriculum", "sections"]:
                    if json_field in row_dict and isinstance(row_dict[json_field], str):
                        row_dict[json_field] = json.loads(row_dict[json_field])
                

                if "level_name" in row_dict:
                    row_dict["level"] = row_dict.pop("level_name")
                
                if "lessons_count" in row_dict:
                    row_dict["lessons"] = row_dict.pop("lessons_count")


                if "bestseller" in row_dict:
                    row_dict["bestseller"] = bool(row_dict["bestseller"])
                if "created" in row_dict:
                    row_dict["created"] = bool(row_dict["created"])
                    
                courses_list.append(row_dict)
                
        return courses_list


    async def update_in_db(self, course_id: int, updated_data: dict) -> bool:
        sql = """
            UPDATE courses 
            SET title = :1, summary = :2, price = :3, sections = :4
            WHERE id = :5
        """
        async with self.db.cursor() as cursor:
            await cursor.execute(sql, [
                updated_data["title"],
                updated_data["summary"],
                updated_data["price"],
                json.dumps(updated_data["sections"]),
                course_id
            ])
            await self.db.commit()
            return cursor.rowcount > 0

    async def delete_from_db(self, course_id: int) -> bool:
        sql = "DELETE FROM courses WHERE id = :1"
        async with self.db.cursor() as cursor:
            await cursor.execute(sql, [course_id])
            await self.db.commit()
            return cursor.rowcount > 0
