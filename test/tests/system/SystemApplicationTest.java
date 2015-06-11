package tests.system;

import javafx.application.Application;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.api.FxAssert;

import org.loadui.testfx.controls.impl.EnabledMatcher;

import com.musicott.MainApp;

public class SystemApplicationTest extends ApplicationTest {

	private Application frame;
	private Stage stage;
	
	@Before
	public void setUp() throws InterruptedException {
		Thread.sleep(500);
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		frame = new MainApp();
		this.stage = stage;
		frame.start(this.stage);
		WaitForAsyncUtils.waitForFxEvents();
	}

	@Test
	public void testImportCollection() {
		clickOn("#menuFile");
		clickOn("#menuOpenImport");
		testImportView();		
		clickOn("#openButton");
		press(KeyCode.ENTER);
		clickOn("#importButton");
		testImportProgressView();
	}
	
	public void testImportView() {
		FxAssert.verifyThat("#alsoAddLabel", NodeMatchers.hasText("Also add:"));
		FxAssert.verifyThat("#infoLabel", NodeMatchers.hasText("Choose the folder of your music collection. Musicott will scan all the files in the subsequent folders."));
		FxAssert.verifyThat("#folderLabel", NodeMatchers.hasText(""));
		FxAssert.verifyThat("#openButton", NodeMatchers.isVisible());
		FxAssert.verifyThat("#openButton", NodeMatchers.hasText("Open..."));
		FxAssert.verifyThat("#cancelButton", NodeMatchers.isVisible());
		FxAssert.verifyThat("#cancelButton", NodeMatchers.hasText("Cancel"));
		FxAssert.verifyThat("#importButton", NodeMatchers.isVisible());
		FxAssert.verifyThat("#importButton", NodeMatchers.hasText("Import"));
		FxAssert.verifyThat("#importButton", NodeMatchers.isDisabled());
		FxAssert.verifyThat("#cbM4a", NodeMatchers.isVisible());
		FxAssert.verifyThat("#cbM4a", NodeMatchers.isEnabled());
		FxAssert.verifyThat("#cbFlac", NodeMatchers.isVisible());
		FxAssert.verifyThat("#cbFlac", NodeMatchers.isEnabled());
		FxAssert.verifyThat("#cbWav", NodeMatchers.isVisible());
		FxAssert.verifyThat("#cbWav", NodeMatchers.isEnabled());
	}
	
	public void testImportProgressView() {
		FxAssert.verifyThat("#counterLabel", NodeMatchers.isVisible());
		FxAssert.verifyThat("#pBar", NodeMatchers.isVisible());
	}
}