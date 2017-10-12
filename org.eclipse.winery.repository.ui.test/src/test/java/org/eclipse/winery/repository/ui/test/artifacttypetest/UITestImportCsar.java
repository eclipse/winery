package org.eclipse.winery.repository.ui.test.artifacttypetest;

import org.eclipse.winery.repository.ui.test.TestSettings;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.firefox.FirefoxDriver;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 * this test imports a CSAR file  
 *
 */

public class UITestImportCsar extends TestSettings {

	private static FirefoxDriver driver;

	@BeforeClass
	public static void init() throws Exception {
		settings();
		driver = new FirefoxDriver();
		driver.manage().timeouts().implicitlyWait(10, SECONDS);
	}

	@AfterClass
	public static void shutdown() throws Exception {
		//driver.close();
	}

	@Test
	//check if CSAR import works, e.g. by adding a .txt file 
	public void testImportCsar() throws Exception {
		driver.get("http://localhost:4200/#/other");
		driver.findElement(By.xpath("//a[@class='btn btn-default'][contains(text(), 'Artifact Types')]")).click();
		driver.findElement(By.id("sectionsImportCsarBtn")).click();
//		driver.findElement(By.xpath("//winery-modal-body//div//winery-uploader//input[@type='file']")).click();
		driver.findElement(By.xpath("//input[@type='file']")).click();
		//driver.findElement(By.name("Testfälle")).click();
		

		//element = driver.findElement(By.id("componentName"));
		//element.sendKeys("Hallo");
		//driver.findElement(By.id("namespace")).sendKeys("Hallo");
		//element = driver.findElement(By.xpath("//button[@class='btn btn-primary'][contains(text(), 'Add')]"));
		//element.click();
		//driver.findElement(By.cssSelector("btn btn-default sidebar-btn")).click();
		//driver.findElement(By.id("sectionsAddNewBtn")).click();
	}
}
