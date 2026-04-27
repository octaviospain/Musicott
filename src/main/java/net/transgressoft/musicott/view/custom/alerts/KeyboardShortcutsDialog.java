package net.transgressoft.musicott.view.custom.alerts;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-maintaining keyboard shortcuts help dialog. Walks the supplied {@link MenuBar} at render
 * time and renders a two-column Action / Shortcut {@link TableView} populated with every menu
 * item that carries an accelerator. Adding new accelerators elsewhere in the menu tree is
 * automatically reflected here without any change to this class.
 *
 * @author Octavio Calleya
 */
public class KeyboardShortcutsDialog extends Dialog<Void> {

    private static final String DIALOG_STYLE = "/css/dialog.css";
    private static final String TABLE_STYLE = "/css/tracktable.css";
    private static final double DIALOG_PREF_WIDTH = 480;
    private static final double DIALOG_PREF_HEIGHT = 400;
    private static final double ACTION_COLUMN_WIDTH = 280;
    private static final double SHORTCUT_COLUMN_WIDTH = 160;

    public KeyboardShortcutsDialog(MenuBar rootMenuBar) {
        getDialogPane().getStylesheets().add(DIALOG_STYLE);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Keyboard Shortcuts");
        setResizable(false);

        TableView<ShortcutEntry> table = buildShortcutTable(collectShortcuts(rootMenuBar));
        table.getStylesheets().add(TABLE_STYLE);
        getDialogPane().setContent(table);
        getDialogPane().setPrefSize(DIALOG_PREF_WIDTH, DIALOG_PREF_HEIGHT);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Filter on DialogPane (not the scene) so ESC works regardless of scene-attachment timing.
        getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode() == KeyCode.ESCAPE) {
                close();
                ke.consume();
            }
        });
    }

    private List<ShortcutEntry> collectShortcuts(MenuBar menuBar) {
        List<ShortcutEntry> entries = new ArrayList<>();
        for (Menu menu : menuBar.getMenus()) {
            collectFromMenu(menu, entries);
        }
        return entries;
    }

    /**
     * Recursively walks a menu and its submenus, appending an entry for every item that has an
     * accelerator. Separators and items without an accelerator are skipped.
     */
    private void collectFromMenu(Menu menu, List<ShortcutEntry> entries) {
        for (MenuItem item : menu.getItems()) {
            if (item instanceof SeparatorMenuItem) {
                continue;
            }
            if (item instanceof Menu nested) {
                collectFromMenu(nested, entries);
                continue;
            }
            if (item.getAccelerator() != null) {
                entries.add(new ShortcutEntry(item.getText(), item.getAccelerator().getDisplayText()));
            }
        }
    }

    private TableView<ShortcutEntry> buildShortcutTable(List<ShortcutEntry> entries) {
        TableView<ShortcutEntry> table = new TableView<>(FXCollections.observableArrayList(entries));

        TableColumn<ShortcutEntry, String> actionColumn = new TableColumn<>("Action");
        actionColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().action()));
        actionColumn.setMinWidth(ACTION_COLUMN_WIDTH);
        actionColumn.setMaxWidth(ACTION_COLUMN_WIDTH);
        actionColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<ShortcutEntry, String> shortcutColumn = new TableColumn<>("Shortcut");
        shortcutColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().shortcut()));
        shortcutColumn.setMinWidth(SHORTCUT_COLUMN_WIDTH);
        shortcutColumn.setMaxWidth(SHORTCUT_COLUMN_WIDTH);
        shortcutColumn.setStyle("-fx-alignment: CENTER;");

        table.getColumns().add(actionColumn);
        table.getColumns().add(shortcutColumn);
        table.getColumns().forEach(c -> c.setReorderable(false));
        return table;
    }

    /**
     * Action label and its accelerator display string for one row of the help dialog.
     */
    public record ShortcutEntry(String action, String shortcut) {}
}
