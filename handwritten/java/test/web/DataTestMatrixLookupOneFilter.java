package test.web;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DataTestMatrixLookupOneFilter {
	static DataTestMatrix DataTestMatrix = new DataTestMatrix();

	@Test
	public void init() throws Exception {
		// TODO: Make Selenium download te Molgenis Matrix export file
		DataTestMatrix.file = "/pheno_one_filter.txt";
		DataTestMatrix.getMatrixColumnsIndex();
		if (DataTestMatrix.getDbTablesColumns() == false)
			Assert.assertFalse(true);
	}

	@Test(dependsOnMethods = { "init" })
	public void testMakeGlobalTable() throws Exception {
		DataTestMatrix.makeGlobalTable();
	}

	@Test(dependsOnMethods = { "testMakeGlobalTable" })
	public void testFillGlobalTable() throws Exception {
		DataTestMatrix.fillGlobalTable();
	}

	@Test(dependsOnMethods = { "testFillGlobalTable" })
	public void testMakeTables() throws Exception {
		DataTestMatrix.makeTables();
	}

	@Test(dependsOnMethods = { "testMakeTables" })
	public void testFillTables() throws Exception {
		DataTestMatrix.fillTables();
	}

	@Test(dependsOnMethods = { "testFillTables" })
	public void testCompareTablesOneFilter() throws Exception {
		if (DataTestMatrix.compareFilteredTables("BEZOEK1", "GEWICHT", "=",
				"50", null, null, null, null) == false)
			Assert.assertFalse(true);
	}

}