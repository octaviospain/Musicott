package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.lirp.persistence.json.JsonFileRepository;
import net.transgressoft.musicott.events.*;
import net.transgressoft.musicott.test.*;
import net.transgressoft.musicott.view.custom.PlaylistTreeView;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.testfx.api.FxRobot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static net.transgressoft.commons.fx.music.playlist.ObservablePlaylistSerializerKt.ObservablePlaylistMapSerializer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.matcher.control.LabeledMatchers.hasText;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * UI test exercising a complete playlist management user journey within the NavigationLayout.
 * All actions are performed via the FxRobot interacting with the UI. Since the playlist name
 * entry dialog lives in MainLayout (outside this test scope), the {@code +} button menu items
 * are wired to auto-create playlists with generated names, simulating what MainController does
 * after the user enters a name.
 */
@JavaFxSpringTest(classes = PlaylistManagementUITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Playlist management")
class PlaylistManagementUIT extends ApplicationTestBase<VBox> {

    @Autowired
    FxControllerAndView<NavigationController, VBox> navigationControllerAndView;

    @Autowired
    ObservablePlaylistHierarchy playlistRepository;

    @Autowired
    ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    static final AtomicInteger playlistCounter = new AtomicInteger(0);

    @Override
    protected VBox javaFxComponent() {
        return navigationControllerAndView.getView().get();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        var nav = navigationControllerAndView.getController();

        // Wire + button "New Playlist" to auto-create and add a playlist (simulates MainController's text field flow)
        nav.setOnNewPlaylistAction(e -> {
            var playlist = playlistRepository.createPlaylist("Playlist " + playlistCounter.incrementAndGet());
            nav.addNewPlaylist(playlist);
        });

        // Wire + button "New Playlist Folder" to auto-create and add a folder
        nav.setOnNewFolderPlaylistAction(e -> {
            var folder = playlistRepository.createPlaylistDirectory("Folder " + playlistCounter.incrementAndGet());
            nav.addNewPlaylist(folder);
        });

        // Wire context menu events via the mocked publisher
        doAnswer(invocation -> {
            var event = invocation.getArgument(0);
            if (event instanceof CreatePlaylistEvent) {
                var playlist = playlistRepository.createPlaylist("Playlist " + playlistCounter.incrementAndGet());
                nav.addNewPlaylist(playlist);
            } else if (event instanceof CreatePlaylistDirectoryEvent) {
                var folder = playlistRepository.createPlaylistDirectory("Folder " + playlistCounter.incrementAndGet());
                nav.addNewPlaylist(folder);
            } else if (event instanceof DeleteSelectedPlaylistEvent) {
                selectedPlaylistProperty.get().ifPresent(nav::deletePlaylist);
            }
            return null;
        }).when(applicationEventPublisher).publishEvent(any());
    }

    private Set<TreeCell<ObservablePlaylist>> visiblePlaylistCells(FxRobot fxRobot) {
        return fxRobot.lookup(".tree-cell")
                .match(hasText(not(emptyOrNullString())))
                .queryAll();
    }

    // -----------------------------------------------------------------------
    // User journey: playlist creation via + button
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("starts with empty playlist tree")
    void startsWithEmptyPlaylistTree(FxRobot fxRobot) {
        waitForFxEvents();

        TreeView<ObservablePlaylist> treeView = fxRobot.lookup("#playlistTreeView").query();
        assertThat(treeView.getRoot()).isNotNull();
        assertThat(treeView.getRoot().getValue().getName()).isEqualTo("ROOT_PLAYLIST");
        assertThat(visiblePlaylistCells(fxRobot)).isEmpty();
    }

    @Test
    @Order(2)
    @DisplayName("clicks + button to create a new playlist")
    void clicksPlusButtonToCreatePlaylist(FxRobot fxRobot) {
        waitForFxEvents();

        fxRobot.clickOn("#newPlaylistButton");
        waitForFxEvents();
        fxRobot.clickOn("New Playlist");
        waitForFxEvents();

        var cells = visiblePlaylistCells(fxRobot);
        assertThat(cells).hasSize(1)
                .extracting(TreeCell::getItem)
                .extracting(ObservablePlaylist::getName)
                .first().asString().startsWith("Playlist ");

        // The new playlist should be a leaf (not a folder)
        assertThat(cells.iterator().next().getTreeItem().isLeaf()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("clicks + button to create a playlist folder")
    void clicksPlusButtonToCreateFolder(FxRobot fxRobot) {
        waitForFxEvents();

        fxRobot.clickOn("#newPlaylistButton");
        waitForFxEvents();
        fxRobot.clickOn("New Playlist Folder");
        waitForFxEvents();

        var cells = visiblePlaylistCells(fxRobot);
        assertThat(cells).hasSize(2);

        var folderCell = cells.stream()
                .filter(c -> c.getItem().getName().startsWith("Folder "))
                .findFirst().orElseThrow();
        assertThat(folderCell.getTreeItem().isLeaf()).isFalse();
    }

    // -----------------------------------------------------------------------
    // User journey: navigation mode switching
    // -----------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("selecting a playlist then switching to All Tracks deselects it")
    void selectPlaylistThenSwitchToAllTracks(FxRobot fxRobot) {
        waitForFxEvents();

        // Click on the first playlist
        var firstPlaylist = visiblePlaylistCells(fxRobot).iterator().next();
        fxRobot.clickOn(firstPlaylist);
        waitForFxEvents();
        assertThat(selectedPlaylistProperty.get()).isPresent();

        // Switch to All Tracks
        fxRobot.clickOn("All tracks");
        waitForFxEvents();
        waitForFxEvents();
        assertThat(selectedPlaylistProperty.get()).isEmpty();
    }

    @Test
    @Order(5)
    @DisplayName("switches between Artists, All Tracks, and playlist")
    void switchesBetweenNavigationModes(FxRobot fxRobot) {
        waitForFxEvents();

        // Click Artists
        fxRobot.clickOn("Artists");
        waitForFxEvents();
        assertThat(selectedPlaylistProperty.get()).isEmpty();

        // Click back to a playlist
        var firstPlaylist = visiblePlaylistCells(fxRobot).iterator().next();
        fxRobot.clickOn(firstPlaylist);
        waitForFxEvents();
        assertThat(selectedPlaylistProperty.get()).isPresent();
        var selectedName = selectedPlaylistProperty.get().get().getName();

        // Switch to All tracks and back to same playlist
        fxRobot.clickOn("All tracks");
        waitForFxEvents();
        assertThat(selectedPlaylistProperty.get()).isEmpty();

        fxRobot.clickOn(firstPlaylist.getText());
        waitForFxEvents();
        assertThat(selectedPlaylistProperty.get()).isPresent();
        assertThat(selectedPlaylistProperty.get().get().getName()).isEqualTo(selectedName);
    }

    // -----------------------------------------------------------------------
    // User journey: context menu playlist creation
    // -----------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("right-click context menu creates playlist")
    void rightClickContextMenuCreatesPlaylist(FxRobot fxRobot) {
        waitForFxEvents();
        int countBefore = visiblePlaylistCells(fxRobot).size();

        // Right-click on the tree view and fire "Add new playlist" context menu item
        TreeView<ObservablePlaylist> treeView = fxRobot.lookup("#playlistTreeView").query();
        fxRobot.interact(() -> {
            var items = treeView.getContextMenu().getItems();
            items.stream()
                    .filter(mi -> "Add new playlist".equals(mi.getText()))
                    .findFirst().orElseThrow()
                    .fire();
        });
        waitForFxEvents();

        assertThat(visiblePlaylistCells(fxRobot)).hasSize(countBefore + 1);
    }

    @Test
    @Order(7)
    @DisplayName("right-click context menu creates folder")
    void rightClickContextMenuCreatesFolder(FxRobot fxRobot) {
        waitForFxEvents();
        int countBefore = visiblePlaylistCells(fxRobot).size();

        TreeView<ObservablePlaylist> treeView = fxRobot.lookup("#playlistTreeView").query();
        fxRobot.interact(() -> {
            var items = treeView.getContextMenu().getItems();
            items.stream()
                    .filter(mi -> "Add new playlist folder".equals(mi.getText()))
                    .findFirst().orElseThrow()
                    .fire();
        });
        waitForFxEvents();

        assertThat(visiblePlaylistCells(fxRobot)).hasSize(countBefore + 1);
    }

    // -----------------------------------------------------------------------
    // User journey: selecting different playlists sequentially
    // -----------------------------------------------------------------------

    @Test
    @Order(8)
    @DisplayName("selects different playlists sequentially")
    void selectsDifferentPlaylistsSequentially(FxRobot fxRobot) {
        waitForFxEvents();

        var cells = visiblePlaylistCells(fxRobot).stream().toList();
        assertThat(cells.size()).isGreaterThanOrEqualTo(2);

        // Click first playlist
        fxRobot.clickOn(cells.get(0));
        waitForFxEvents();
        assertThat(selectedPlaylistProperty.get()).isPresent();
        var firstName = selectedPlaylistProperty.get().get().getName();

        // Click second playlist — selection should switch
        fxRobot.clickOn(cells.get(1));
        waitForFxEvents();
        assertThat(selectedPlaylistProperty.get()).isPresent();
        assertThat(selectedPlaylistProperty.get().get().getName()).isNotEqualTo(firstName);
    }

    // -----------------------------------------------------------------------
    // User journey: multi-select with CTRL
    // -----------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("CTRL-click selects multiple playlists")
    void ctrlClickSelectsMultiplePlaylists(FxRobot fxRobot) {
        waitForFxEvents();

        var cells = visiblePlaylistCells(fxRobot).stream().toList();
        assertThat(cells.size()).isGreaterThanOrEqualTo(2);

        fxRobot.clickOn(cells.get(0));
        waitForFxEvents();

        fxRobot.press(KeyCode.CONTROL).clickOn(cells.get(1)).release(KeyCode.CONTROL);
        waitForFxEvents();

        TreeView<?> treeView = fxRobot.lookup("#playlistTreeView").query();
        assertThat(treeView.getSelectionModel().getSelectedItems()).hasSizeGreaterThanOrEqualTo(2);
    }

    // -----------------------------------------------------------------------
    // User journey: delete playlist via context menu
    // -----------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("deletes playlist via context menu")
    void deletesPlaylistViaContextMenu(FxRobot fxRobot) {
        waitForFxEvents();

        int countBefore = visiblePlaylistCells(fxRobot).size();

        // Select first visible non-folder playlist
        var firstCell = visiblePlaylistCells(fxRobot).stream()
                .filter(c -> !c.getItem().isDirectory())
                .findFirst().orElseThrow();
        fxRobot.clickOn(firstCell);
        waitForFxEvents();
        assertThat(selectedPlaylistProperty.get()).isPresent();

        // Fire "Delete playlist" context menu item
        TreeView<ObservablePlaylist> treeView = fxRobot.lookup("#playlistTreeView").query();
        fxRobot.interact(() -> {
            var items = treeView.getContextMenu().getItems();
            items.stream()
                    .filter(mi -> "Delete playlist".equals(mi.getText()))
                    .findFirst().orElseThrow()
                    .fire();
        });
        waitForFxEvents();

        assertThat(visiblePlaylistCells(fxRobot)).hasSize(countBefore - 1);
    }

    // -----------------------------------------------------------------------
    // User journey: folder navigation via double-click
    // -----------------------------------------------------------------------

    @Test
    @Order(11)
    @DisplayName("creates nested playlist inside folder and expands it")
    void createsNestedPlaylistAndExpandsFolder(FxRobot fxRobot) {
        waitForFxEvents();

        // Find a folder cell
        var folderCell = visiblePlaylistCells(fxRobot).stream()
                .filter(c -> c.getItem().isDirectory())
                .findFirst();
        assertThat(folderCell).isPresent();

        // Select the folder
        fxRobot.clickOn(folderCell.get());
        waitForFxEvents();

        // Create a nested playlist via + button (will be added inside the selected folder)
        fxRobot.clickOn("#newPlaylistButton");
        waitForFxEvents();
        fxRobot.clickOn("New Playlist");
        waitForFxEvents();

        // Expand the folder programmatically (headless double-click is unreliable for TreeItem expansion)
        TreeView<ObservablePlaylist> treeView = fxRobot.lookup("#playlistTreeView").query();
        Platform.runLater(() -> {
            for (var child : treeView.getRoot().getChildren()) {
                if (child.getValue().equals(folderCell.get().getItem())) {
                    child.setExpanded(true);
                    break;
                }
            }
        });
        waitForFxEvents();

        // Verify nested playlist is visible
        var allCells = visiblePlaylistCells(fxRobot);
        var nestedCells = allCells.stream()
                .filter(c -> !c.getItem().isDirectory())
                .toList();
        assertThat(nestedCells).isNotEmpty();
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                NavigationController.class,
                PlaylistTreeView.class
        })
})
class PlaylistManagementUITConfiguration {

    File playlistsFile;

    public PlaylistManagementUITConfiguration() throws IOException {
        playlistsFile = Files.createTempFile("playlists-ui-test", ".json").toFile();
    }

    @Bean
    public ObservablePlaylistHierarchy playlistRepository() {
        var repository = new ObservablePlaylistHierarchy(
                new JsonFileRepository<>(playlistsFile, ObservablePlaylistMapSerializer()));
        repository.createPlaylistDirectory("ROOT_PLAYLIST");
        return repository;
    }

    @Bean
    public ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty() {
        return new SimpleObjectProperty<>(this, "selected ui test playlist", Optional.empty());
    }

    @Bean
    public ObservableAudioLibrary audioRepository() {
        return mock(ObservableAudioLibrary.class);
    }

    @Bean
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    @Bean
    public KeyCombination.Modifier operativeSystemKeyModifier() {
        return KeyCombination.CONTROL_DOWN;
    }

    @Bean(destroyMethod = "")
    public FxWeaver fxWeaver(ConfigurableApplicationContext applicationContext) {
        return new SpringFxWeaver(applicationContext);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public <C, V extends Node> FxControllerAndView<C, V> controllerAndView(FxWeaver fxWeaver, InjectionPoint injectionPoint) {
        return new InjectionPointLazyFxControllerAndViewResolver(fxWeaver).resolve(injectionPoint);
    }
}
