package tests.system;

import javafx.application.Application;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import org.testfx.matcher.base.NodeMatchers;

import static org.testfx.api.FxAssert.verifyThat;

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
		clickOn("#menuItemImport");
		testImportView();		
		clickOn("#openButton");
		press(KeyCode.ENTER);
		clickOn("#importButton");
		testImportProgressView();
	}
	
	public void testImportView() {
		verifyThat("#alsoAddLabel", NodeMatchers.hasText("Also add:"));
		verifyThat("#infoLabel", NodeMatchers.hasText("Choose the folder of your music collection. Musicott will scan all the files in the subsequent folders."));
		verifyThat("#folderLabel", NodeMatchers.hasText(""));
		verifyThat("#openButton", NodeMatchers.isVisible());
		verifyThat("#openButton", NodeMatchers.hasText("Open..."));
		verifyThat("#cancelButton", NodeMatchers.isVisible());
		verifyThat("#cancelButton", NodeMatchers.hasText("Cancel"));
		verifyThat("#importButton", NodeMatchers.isVisible());
		verifyThat("#importButton", NodeMatchers.hasText("Import"));
		verifyThat("#importButton", NodeMatchers.isDisabled());
		verifyThat("#cbM4a", NodeMatchers.isVisible());
		verifyThat("#cbM4a", NodeMatchers.isEnabled());
		verifyThat("#cbFlac", NodeMatchers.isVisible());
		verifyThat("#cbFlac", NodeMatchers.isEnabled());
		verifyThat("#cbWav", NodeMatchers.isVisible());
		verifyThat("#cbWav", NodeMatchers.isEnabled());
	}
	
	public void testImportProgressView() {
		verifyThat("#pBar", NodeMatchers.isVisible());
	}
}
