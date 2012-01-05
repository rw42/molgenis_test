package test.web;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DataTestMatrix {

	String dbDriver;
	String dbUrl;
	String dbUsername;
	String dbPassword;

	String file;

	String testTablePrefix;
	String testOwner;
	String sourceTablePrefix;
	String sourceOwner;
	String matrixSeperator;
	String matrixColumnSeperator;
	String matrixStringDateFormat;

	String filter1Table = "";
	String filter1Column = "";
	String filter1Operator = "";
	String filter1Value = "";

	String filter2Table = "";
	String filter2Column = "";
	String filter2Operator = "";
	String filter2Value = "";

	Statement stmt;
	ResultSet rset;
	String[] paidMatrixColumnNameParts;
	Map<String, Integer> matrixColumnsIndex = new HashMap<String, Integer>();
	Map<String, ArrayList<String>> publishTablesColumns = new HashMap<String, ArrayList<String>>();

	public void init() {
		Locale.setDefault(Locale.US);
		try {
			Class.forName(dbDriver);
			Connection conn = DriverManager.getConnection(dbUrl, dbUsername,
					dbPassword);
			stmt = conn.createStatement();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void getMatrixColumnsIndex() throws Exception {
		System.out.println("Getting Matrix column index...");
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new DataInputStream(new FileInputStream(file))));
		String strLine = br.readLine();
		String[] matrixColumns = strLine.split(matrixSeperator);
		for (int i = 0; i < matrixColumns.length; i++)
			if (matrixColumns[i].length() != 0)
				matrixColumnsIndex.put(matrixColumns[i], i);
		br.close();
	}

	public Boolean getPA_ID() {
		for (String column : matrixColumnsIndex.keySet()) {
			if (column.contains("PA_ID")) {
				paidMatrixColumnNameParts = column.split(matrixColumnSeperator);
				if (paidMatrixColumnNameParts.length == 2) {
					return false;
				}
			}
		}
		System.out.println("FAIL: No PA_ID found");
		return true;
	}

	public boolean getPublishTablesColumns() throws Exception {
		System.out.println("Generating Publish table column structure...");
		boolean fail = false;
		for (Map.Entry<String, Integer> matrixColumn : matrixColumnsIndex
				.entrySet()) {
			String[] matrixColumnParts = matrixColumn.getKey().split(
					matrixColumnSeperator);
			if (matrixColumnParts.length == 2) {
				if (tableColumnExists(sourceOwner, sourceTablePrefix
						+ matrixColumnParts[0], matrixColumnParts[1])) {
					if (publishTablesColumns.get(matrixColumnParts[0]) == null) {
						ArrayList<String> dbColumns = new ArrayList<String>();
						dbColumns.add(matrixColumnParts[1]);
						publishTablesColumns.put(matrixColumnParts[0],
								dbColumns);
					} else {
						ArrayList<String> dbColumns = new ArrayList<String>();
						dbColumns = publishTablesColumns
								.get(matrixColumnParts[0]);
						if (dbColumns.contains(matrixColumnParts[1]) == false)
							dbColumns.add(matrixColumnParts[1]);
						publishTablesColumns.put(matrixColumnParts[0],
								dbColumns);
					}
				} else {
					System.out.println("FAIL: '" + sourceTablePrefix
							+ matrixColumnParts[0] + "." + matrixColumnParts[1]
							+ "' (based on matrix column) not in source");
					fail = true;
				}
			} else {
				System.out
						.println("FAIL: '"
								+ matrixColumn.getValue()
								+ "' not conform format. (can not extract the source table and column name)");
				fail = true;
			}
		}
		return fail;
	}

	public void makeGlobalTable() throws Exception {
		System.out.println("Making global table...");
		if (tableExists(testOwner, testTablePrefix + "GLOBAL"))
			stmt.execute("drop table " + testTablePrefix + "GLOBAL");
		String sql = "create table " + testTablePrefix + "GLOBAL (";
		for (Integer key : matrixColumnsIndex.values())
			sql += "c" + key + " varchar(255), ";
		sql = sql.substring(0, sql.length() - 2) + ")";
		stmt.execute(sql);
	}

	public boolean fillGlobalTable() throws Exception {
		boolean fail = false;
		System.out.println("Filling global table...");
		String[] lineParts;
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new DataInputStream(new FileInputStream(file))));
		// skip first line column names
		br.readLine();
		String insertIntoSql = "insert into " + testTablePrefix + "GLOBAL (";
		for (Integer key : matrixColumnsIndex.values())
			insertIntoSql += "c" + key + ", ";
		insertIntoSql = insertIntoSql.substring(0, insertIntoSql.length() - 2)
				+ ") ";

		String line;
		Integer totalCounter = 1;
		Integer counter = 1;
		while ((line = br.readLine()) != null) {
			totalCounter++;
			counter++;
			if (counter >= 500) {
				counter = 0;
				System.out.println("Rowcount: " + totalCounter);
			}
			Boolean exception = false;
			lineParts = line.split(matrixSeperator);
			String selectSql = "select ";
			for (Integer key : matrixColumnsIndex.values()) {
				try {
					selectSql += "'" + lineParts[key].replace("'", "''")
							+ "', ";
				} catch (Exception e) {
					exception = true;
					fail = true;
				}
			}
			if (exception) {
				System.out.println("FAIL: Wrong line length. Line: " + line);
			} else {
				selectSql = selectSql.substring(0, selectSql.length() - 2)
						+ " from dual";
				// System.out.println(insertIntoSql + selectSql);
				stmt.executeQuery(insertIntoSql + selectSql);
			}
		}
		br.close();
		return fail;
	}

	public void makeTables() throws Exception {
		System.out.println("Make tables...");
		for (Map.Entry<String, ArrayList<String>> entry : publishTablesColumns
				.entrySet()) {
			if (tableExists(testOwner, testTablePrefix + entry.getKey()))
				stmt.executeQuery("drop table " + testTablePrefix
						+ entry.getKey());
			String sql = "create table " + testTablePrefix + entry.getKey()
					+ " (";
			for (String value : entry.getValue())
				sql += value + " varchar(255), ";
			sql = sql.substring(0, sql.length() - 2) + ")";
			stmt.executeQuery(sql);
		}
	}

	public void fillTables() throws Exception {
		System.out.println("Filling tables...");
		for (Map.Entry<String, ArrayList<String>> entry : publishTablesColumns
				.entrySet()) {
			String table = entry.getKey();
			String sql = "insert into " + testTablePrefix + entry.getKey()
					+ " (";
			for (String column : entry.getValue())
				sql += column + ", ";
			sql = sql.substring(0, sql.length() - 2) + ") ";
			sql += "select ";
			for (String column : entry.getValue())
				sql += "c"
						+ matrixColumnsIndex.get(table + matrixColumnSeperator
								+ column) + ", ";
			sql = sql.substring(0, sql.length() - 2) + " from "
					+ testTablePrefix + "GLOBAL ";
			stmt.executeQuery(sql);
		}
	}

	public boolean compareTables() throws Exception {
		boolean fail = false;
		for (Map.Entry<String, ArrayList<String>> entry : publishTablesColumns
				.entrySet()) {
			String table = entry.getKey();
			System.out.println("Comparing " + table + "...");

			String testTableSql = "select ";
			for (String column : entry.getValue())
				testTableSql += "case when upper(" + column
						+ ") ='NULL' then null else " + column + " end as "
						+ column + ", ";
			testTableSql = testTableSql.substring(0, testTableSql.length() - 2)
					+ " from " + testTablePrefix + table + " ";
			// Filter rows with only NULL values because this can be a permitted
			// result from joining multiple tables.
			testTableSql += "where ";
			for (String column : entry.getValue())
				testTableSql += column + " is not null and ";
			testTableSql = testTableSql.substring(0, testTableSql.length() - 4);

			String baseTableSql = "select ";
			for (String column : entry.getValue()) {
				rset = stmt
						.executeQuery("select data_type from ALL_TAB_COLUMNS where owner = '"
								+ testOwner
								+ "' and table_name='"
								+ table
								+ "' and column_name = '" + column + "'");
				rset.next();
				if (rset.getString(1).equals("DATE"))
					baseTableSql += "to_char(" + column + ", '"
							+ matrixStringDateFormat + "'), ";
				else
					baseTableSql += "to_char(" + column + "), ";
			}
			baseTableSql = baseTableSql.substring(0, baseTableSql.length() - 2)
					+ " from " + table;

			String sql = "select count(*) from (" + testTableSql + " minus "
					+ baseTableSql + ") ";
			rset = stmt.executeQuery(sql);
			rset.next();
			if (Integer.parseInt(rset.getString(1)) == 0)
				System.out.println("SUCCES " + table + " data in source.");
			else {
				fail = true;
				System.out.println("FAILED datacompare for: " + table);
				System.out.println(sql);
			}

			/*
			 * String sql = "";
			 * 
			 * String sqlTestTable = "select " + column + " from " +
			 * testTablePrefix + entry.getKey() + " ";
			 * 
			 * // select case when DEMO12A2 is null then 'null' else //
			 * to_char(DEMO12A2) end as tada from MOLGENIS1.UVDEMOG
			 * 
			 * String sqlSourceTable = "select case when " + column +
			 * " is null then '' else to_char(" + column + ") end as from " +
			 * sourceOwner + "." + sourceTablePrefix + entry.getKey() + " ";
			 * 
			 * if (filter1Table.length() != 0 &&
			 * filter1Table.equals(entry.getKey())) sqlSourceTable += "where " +
			 * filter1Column + " " + filter1Operator + " '" + filter1Value +
			 * "' ";
			 * 
			 * if (filter2Table.length() != 0 &&
			 * filter2Table.equals(entry.getKey())) sqlSourceTable += "and " +
			 * filter2Column + " " + filter2Operator + " '" + filter2Value +
			 * "' ";
			 * 
			 * sql = "select count(*) from (" + sqlTestTable + " minus " +
			 * sqlSourceTable + ")";
			 * 
			 * rset = stmt.executeQuery(sql); rset.next(); if
			 * (Integer.parseInt(rset.getString(1)) == 0) {
			 * System.out.println("SUCCES " + entry.getKey() +
			 * matrixColumnSeperator + column + " data in source."); } else {
			 * fail = true; System.out.println("FAILED datacompare for: " +
			 * entry.getKey() + matrixColumnSeperator + column);
			 * System.out.println(sql); }
			 */

		}
		return fail;
	}

	boolean tableExists(String owner, String table) throws Exception {
		rset = stmt
				.executeQuery("select count(*) from all_all_tables where owner='"
						+ owner + "' and table_name='" + table + "'");
		rset.next();
		if (rset.getString(1).equals("1"))
			return true;
		return false;
	}

	boolean tableColumnExists(String owner, String table, String column)
			throws Exception {
		String sql = "select count(*) from all_tab_columns where owner='"
				+ owner + "' and table_name='" + table
				+ "' and column_name = '" + column + "'";
		rset = stmt.executeQuery(sql);
		rset.next();
		if (rset.getString(1).equals("1"))
			return true;
		return false;
	}

}
