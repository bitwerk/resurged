package basic;

import junit.AbstractTestCase;

import org.resurged.QueryObjectFactory;
import org.resurged.impl.Log;
import org.resurged.jdbc.DataSet;

public abstract class AbstractAutoIncrTestCase extends AbstractTestCase{
	private PersonDao dao;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		openConnection();
		
		dao = QueryObjectFactory.createQueryObject(PersonDao.class, con, configuration);
		Log.info(this, "PersonDao loaded");

		int createResult =(useMysql)? dao.createTableAutoIncrMySql() : dao.createTableAutoIncr();
		Log.info(this, "Table create, rows affected: " + createResult);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		int dropResult = dao.dropTable();
		Log.info(this, "Table dropped, rows affected: " + dropResult);

		closeConnection();
	}
	
//	public void testAutoGeneratedKeys() throws Exception {
//		String sql="INSERT INTO Persons (first_name, last_name) VALUES (?, ?)";
//		PreparedStatement stmt = con.prepareStatement(sql, new String[]{"ID"});
//		
//		DatabaseMetaData dbmeta = con.getMetaData();
//		System.out.println("supportsGetGeneratedKeys: " + dbmeta.supportsGetGeneratedKeys());
//		
//		stmt.setString(1, "Arthur");
//		stmt.setString(2, "Dent");
//		int rowsAffected = stmt.executeUpdate();
//		
//		ResultSet keys = stmt.getGeneratedKeys();
//		if (keys.next()) {
//			ResultSetMetaData meta = keys.getMetaData();
//			int colCount = meta.getColumnCount();
//			for (int i = 1; i <= colCount; i++) {
//				String key = meta.getColumnName(i);
//				String type = meta.getColumnClassName(i);
//	            Object value = keys.getObject(i);
//	            System.out.println(type + " " + key + " is " + value);
//			}
//		}
//		
//		Log.info(this, "Row inserted, rows affected: " + rowsAffected);
//	}
	
	public void testSimpleQueries() throws Exception {
		int rowsAffected = dao.insertAutoIncr("Arthur", "Dent");
		Log.info(this, "Row inserted, rows affected: " + rowsAffected);

		rowsAffected += dao.insertAutoIncr("Ford", "Prefect");
		Log.info(this, "Row inserted, rows affected: " + rowsAffected);

		rowsAffected += dao.insertAutoIncr("Ford", "Prefect");
		Log.info(this, "Row inserted, rows affected: " + rowsAffected);

		assertEquals(3, rowsAffected);
		
		int affected = dao.update(2, "Zaphod", "Beeblebrox");
		Log.info(this, "Row updated, rows affected: " + affected);
		assertEquals(1, affected);

		DataSet<Person> all = dao.getAll();
		assertEquals(3, all.size());
		for (Person dto : all) {
			Log.info(this, dto.toString());
		}

		DataSet<Person> some = dao.getSome(1);
		assertEquals(1, some.size());
		for (Person dto : some) {
			Log.info(this, dto.toString());
		}

		rowsAffected -= dao.delete(1);
		Log.info(this, "Row deleted, rows left: " + rowsAffected);

		rowsAffected -= dao.deleteAll();
		Log.info(this, "Row deleted, rows left: " + rowsAffected);

		assertEquals(0, rowsAffected);
	}
}
