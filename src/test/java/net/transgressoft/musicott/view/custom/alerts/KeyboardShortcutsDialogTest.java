package net.transgressoft.musicott.view.custom.alerts;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Unit test for {@link KeyboardShortcutsDialog}, verifying that the dialog walks the live
 * menu bar to render rows for every {@link MenuItem} carrying an accelerator and excludes
 * items without one.
 */
@ExtendWith(ApplicationExtension.class)
class KeyboardShortcutsDialogTest {

    MenuBar rootMenuBar;
    MenuItem muteMenuItem;
    MenuItem keyboardShortcutsMenuItem;
    Dialog<Void> openedDialog;

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MenuBarController.fxml"));
        rootMenuBar = loader.load();

        // Wire only the two accelerators relevant to this test, mirroring the production
        // MainController.MenuBarController.initAccelerators wiring for these two items.
        muteMenuItem = findMenuItem("muteMenuItem");
        muteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.M));

        keyboardShortcutsMenuItem = findMenuItem("keyboardShortcutsMenuItem");
        keyboardShortcutsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.SLASH, KeyCombination.SHIFT_DOWN));
        keyboardShortcutsMenuItem.setOnAction(e -> {
            openedDialog = new KeyboardShortcutsDialog(rootMenuBar);
            openedDialog.show();
        });

        stage.setScene(new Scene(rootMenuBar, 600, 40));
        stage.show();
    }

    @AfterEach
    void closeOpenedDialog() {
        if (openedDialog != null && openedDialog.isShowing()) {
            Platform.runLater(openedDialog::close);
            waitForFxEvents();
        }
        openedDialog = null;
    }

    @Test
    @DisplayName("opens dialog when triggered and renders mute row from menu walk")
    @SuppressWarnings("unchecked")
    void opensDialogWhenTriggeredAndRendersMuteRowFromMenuWalk(FxRobot fxRobot) throws Exception {
        waitForFxEvents();

        // Fire the accelerator action directly — equivalent to the user pressing Shift+/.
        // TestFX key dispatch through the menu accelerator system is unreliable in headless
        // Monocle, so we exercise the production action wiring (the same lambda that
        // MainController.initKeyboardShortcutsAction installs).
        Platform.runLater(() -> keyboardShortcutsMenuItem.fire());
        waitForFxEvents();

        Stage dialogStage = waitForDialogStage("Keyboard Shortcuts");
        assertThat(dialogStage).as("Keyboard Shortcuts dialog stage").isNotNull();
        assertThat(dialogStage.isShowing()).isTrue();

        TableView<KeyboardShortcutsDialog.ShortcutEntry> table =
                fxRobot.from(dialogStage.getScene().getRoot()).lookup(".table-view").queryAs(TableView.class);
        assertThat(table.getItems()).extracting("action").contains("Mute");
        assertThat(table.getItems()).extracting("shortcut").contains("M");
    }

    @Test
    @DisplayName("dialog rows include every menu item with an accelerator and exclude items without one")
    @SuppressWarnings("unchecked")
    void dialogRowsIncludeEveryMenuItemWithAnAcceleratorAndExcludeItemsWithoutOne(FxRobot fxRobot) throws Exception {
        waitForFxEvents();

        Platform.runLater(() -> keyboardShortcutsMenuItem.fire());
        waitForFxEvents();

        Stage dialogStage = waitForDialogStage("Keyboard Shortcuts");
        TableView<KeyboardShortcutsDialog.ShortcutEntry> table =
                fxRobot.from(dialogStage.getScene().getRoot()).lookup(".table-view").queryAs(TableView.class);

        // Mute (M) and Keyboard Shortcuts (SLASH+SHIFT) both have accelerators wired in this test
        // setup — both must appear. About Musicott has no accelerator wired here — must be absent.
        assertThat(table.getItems()).extracting("action")
                .contains("Mute", "Keyboard Shortcuts")
                .doesNotContain("About Musicott");
    }

    private MenuItem findMenuItem(String fxId) {
        return rootMenuBar.getMenus().stream()
                .flatMap(menu -> menu.getItems().stream())
                .filter(item -> fxId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("MenuItem not found: " + fxId));
    }

    private Stage waitForDialogStage(String title) throws Exception {
        // Poll for up to 5 seconds for the dialog stage to appear in the window registry.
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        while (System.currentTimeMillis() < deadline) {
            waitForFxEvents();
            Optional<Stage> match = Window.getWindows().stream()
                    .filter(Stage.class::isInstance)
                    .map(Stage.class::cast)
                    .filter(s -> title.equals(s.getTitle()))
                    .filter(Stage::isShowing)
                    .findFirst();
            if (match.isPresent()) {
                return match.get();
            }
            Thread.sleep(50);
        }
        return null;
    }
}
