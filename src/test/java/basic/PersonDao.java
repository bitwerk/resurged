package basic;

import org.resurged.jdbc.BaseQuery;
import org.resurged.jdbc.DataSet;
import org.resurged.jdbc.Select;
import org.resurged.jdbc.Update;

public interface PersonDao extends BaseQuery{
	
	@Update(sql="CREATE TABLE Persons(ID int, FIRST_NAME varchar(255), LASTNAME varchar(255))")
	public int createTable();
	
	@Update("drop table Persons")
	public int dropTable();
	
	@Update("INSERT INTO Persons (id, first_Name, lastName) VALUES (?1, ?2, ?3)")
	public int insert(int id, String firstName, String lastName);
	
	@Update("UPDATE Persons SET first_Name=?2, lastName=?3 WHERE id=?1")
	public int update(int id, String firstName, String lastName);
	
	@Update("DELETE FROM Persons WHERE id=?1")
	public int delete(int id);
	
	@Update("DELETE FROM Persons")
	public int deleteAll();
	
	@Select("SELECT * FROM Persons")
	public DataSet<Person> getAll();

	@Select("SELECT * FROM Persons where id=?1")
	public DataSet<Person> getSome(int id);
	
}
