package com.tightdb.lib;

import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

import com.tightdb.generated.EmployeeView;

public class TableQueryTest extends AbstractTableTest {

	@Test
	public void shouldMatchOnSimpleNumberCriteria() {
		assertEquals(1, employees.salary.equal(30000).findAll().size());

		assertEquals(2, employees.salary.lessThan(30000).findAll().size());
		assertEquals(3, employees.salary.lessThanOrEqual(30000).findAll().size());

		assertEquals(3, employees.salary.greaterThan(5000).findAll().size());
		assertEquals(3, employees.salary.greaterThanOrEqual(10000).findAll().size());
	}

	@Test
	public void shouldMatchOnSimpleStringCriteria() {
		assertEquals(1, employees.firstName.eq("John").findAll().size());
		assertEquals(2, employees.firstName.startsWith("J").findAll().size());
		assertEquals(1, employees.firstName.endsWith("hny").findAll().size());
	}

	@Test
	public void shouldMatchOnCombinedAndOrCriteria() {
		EmployeeView nikoOrJohn = employees.firstName.startsWith("Nik").lastName.contains("vski")
				.or().firstName.eq("John").findAll();

		assertEquals(2, nikoOrJohn.size());
	}

	@Test
	public void shouldMatchOnCriteriaEndingWithGroup() {
		EmployeeView niko = employees.where()
		.firstName.startsWith("Nik")
		.salary.eq(30000)
		.group()
			.lastName.contains("vski")
			.or()
			.firstName.eq("John")
		.endGroup()
		.findAll();

		assertEquals(1, niko.size());
	}

	@Test
	public void shouldMatchOnCriteriaBeginingWithGroup() {
		EmployeeView niko = employees.where()
		.group()
			.lastName.contains("vski")
			.or()
			.firstName.eq("John")
		.endGroup()
		.firstName.startsWith("Nik")
		.salary.eq(30000)
		.findAll();

		assertEquals(1, niko.size());
	}

	@Test
	public void shouldMatchOnCriteriaHavingGroupInMiddle() {
		EmployeeView niko = employees.where()
			.firstName.startsWith("Nik")
			.group()
				.lastName.contains("vski")
				.or()
				.firstName.eq("John")
			.endGroup()
			.salary.eq(30000)
			.findAll();

		assertEquals(1, niko.size());
	}

	@Test
	public void shouldMatchMultipleQueriesWithoutInterference() {
		EmployeeView niko1 = employees.firstName.startsWith("Nik").group().lastName.contains("vski")
				.or().firstName.eq("John").endGroup().findAll();
		EmployeeView niko2 = employees.where().group().lastName.contains("vski")
				.or().firstName.eq("John").endGroup().firstName.startsWith("Nik")
				.findAll();

		assertEquals(1, niko1.size());
		assertEquals(1, niko2.size());
	}

}