package net.transgressoft.musicott.view;

import de.codecentric.centerdevice.MenuToolkit;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import net.rgielen.fxweaver.core.FxmlView;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItemJsonRepository;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistJsonRepository;
import net.transgressoft.commons.music.audio.AudioFileType;
import net.transgressoft.musicott.config.SettingsRepository;
import net.transgressoft.musicott.events.*;
import net.transgressoft.musicott.services.MediaImportService;
import net.transgressoft.musicott.view.custom.ApplicationImage;
import net.transgressoft.musicott.view.custom.alerts.AlertFactory;
import net.transgressoft.musicott.view.custom.table.FullAudioItemTableView;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static net.transgressoft.musicott.config.SettingsRepository.*;
import static net.transgressoft.musicott.view.NavigationController.NavigationMode.ALL_AUDIO_ITEMS;
import static net.transgressoft.musicott.view.NavigationController.NavigationMode.PLAYLIST;
import static org.fxmisc.easybind.EasyBind.map;
import static org.fxmisc.easybind.EasyBind.subscribe;

@FxmlView("/fxml/MainController.fxml")
@Controller
public class MainController {

    private static final int HOVER_COVER_SIZE = 100;

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final Image defaultPlaylistImage = ApplicationImage.DEFAULT_COVER.get();

    private final ObservableAudioItemJsonRepository audioRepository;
    private final ObservablePlaylistJsonRepository playlistRepository;

    private final PlayerController playerController;
    private final PreferencesController preferencesController;
    private final NavigationController navigationController;
    private final ArtistViewController artistViewController;
    private final AlertFactory alertFactory;

    private final ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty;

    /**
     * Handles the action of changing the name of a playlist
     */
    private final EventHandler<KeyEvent> changePlaylistNameTextFieldHandler = changePlaylistNameTextFieldHandler();

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private MenuBarController menuBarController;

    @FXML
    private BorderPane rootBorderPane;
    @FXML
    private VBox headerVBox;
    @FXML
    private BorderPane tableBorderPane;
    @FXML
    private BorderPane wrapperBorderPane;
    @FXML
    private BorderPane contentBorderPane;
    @FXML
    private ImageView playlistImageView;
    @FXML
    private Label playlistTracksNumberLabel;
    @FXML
    private Label playlistSizeLabel;
    @FXML
    private Label playlistTitleLabel;
    @FXML
    private HBox tableInfoHBox;
    @FXML
    private GridPane playlistInfoGridPane;
    @FXML
    private Button playRandomButton;
    @FXML
    private StackPane tableStackPane;
    @FXML
    private SplitPane artistsLayout;
    @FXML
    private VBox navigationLayout;
    @FXML
    private GridPane playerLayout;
    @FXML
    private TextField searchTextField;

    /**
     * The table where tracks are displayed
     */
    private FullAudioItemTableView mainTrackTable;

    /**
     * The TextField that is shown to set a new playlist name or rename an existing one
     */
    private TextField playlistTitleTextField;

    /**
     * The image that is shown on the bottom right corner of the table when hovering over a track
     */
    private ImageView trackHoveredCoverImageView;

    /**
     * The object property of the image shown in the trackHoveredCoverImageView object
     */
    private ObjectProperty<Image> trackHoveredCoverImageProperty;

    /**
     * The object property of the image of the playlist in the playlistImageView
     */
    private ObjectProperty<Image> playlistImageProperty;

    private ListProperty<ObservableAudioItem> showingTracksProperty;
    @Autowired
    private SettingsRepository settingsRepository;
    @Autowired
    private MediaImportService mediaImportService;

    @Autowired
    public MainController(ObservableAudioItemJsonRepository audioRepository,
                          ObservablePlaylistJsonRepository playlistRepository,
                          PlayerController playerController,
                          PreferencesController preferencesController,
                          NavigationController navigationController,
                          ArtistViewController artistViewController,
                          AlertFactory alertFactory,
                          ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty,
                          MediaImportService mediaImportService) {
        this.audioRepository = audioRepository;
        this.playlistRepository = playlistRepository;
        this.playerController = playerController;
        this.preferencesController = preferencesController;
        this.navigationController = navigationController;
        this.artistViewController = artistViewController;
        this.alertFactory = alertFactory;
        this.selectedPlaylistProperty = selectedPlaylistProperty;
        this.mediaImportService = mediaImportService;
    }

    @FXML
    public void initialize() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MenuBarController.fxml"));
        menuBarController = new MenuBarController();
        loader.setController(menuBarController);
        loader.load();

        mainTrackTable = new FullAudioItemTableView(selectedPlaylistProperty, searchTextField.textProperty(),
                playlistRepository.getPlaylistsProperty(), applicationEventPublisher);
        showingTracksProperty = new SimpleListProperty<>(this, "showing audio items", FXCollections.emptyObservableList());



        GridPane.setHgrow(mainTrackTable, Priority.ALWAYS);
        GridPane.setVgrow(mainTrackTable, Priority.ALWAYS);

        initializeTrackHoveredCoverImageView();
        playlistImageProperty = new SimpleObjectProperty<>(this, "playlist image", defaultPlaylistImage);
        playlistImageView.imageProperty().bind(playlistImageProperty);

        initializeInfoPaneFields();

        hideTableInfoPane();

        navigationController.setOnNewPlaylistAction(e -> changeViewToPlaylistCreationMode());
        navigationController.setOnNewFolderPlaylistAction(e -> changeViewToPlaylistCreationMode());
        navigationController.navigationModeProperty().addListener((obs, oldMode, newMode) -> {
            switch (newMode) {
                case ALL_AUDIO_ITEMS:
                    showAllAudioItemsView();
                    break;
                case ARTISTS:
                    showArtistsView();
                    break;
                case PLAYLIST:
                    showPlaylistView();
                    break;
                default:
            }
        });

        audioRepository.emptyLibraryProperty().addListener((observable, wasEmpty, isEmpty) -> {
            if (Boolean.TRUE.equals(isEmpty)) {
                trackHoveredCoverImageView.setImage(defaultPlaylistImage);
                menuBarController.playPauseMenuItem.setDisable(true);
            } else {
                menuBarController.playPauseMenuItem.setDisable(false);
            }
        });
    }

    private void initializeInfoPaneFields() {
        playRandomButton.visibleProperty().bind(showingTracksProperty.emptyProperty().not());
        playRandomButton.setOnAction(e -> selectedPlaylistProperty.get().ifPresent(
            playlist -> applicationEventPublisher.publishEvent(new PlayPlaylistRandomlyEvent(playlist, this))));
        playlistTracksNumberLabel.textProperty().bind(map(showingTracksProperty.sizeProperty(), s -> s + " tracks"));
        playlistSizeLabel.textProperty().bind(map(showingTracksProperty, (ObservableList<ObservableAudioItem> tracks) -> {
            var sizeSum = tracks.stream().mapToLong(ObservableAudioItem::getLength).sum();
            return FileUtils.byteCountToDisplaySize(sizeSum);
        }));
        playlistTitleLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {   // double click to edit the playlist name
                replacePlaylistTitleLabelByTextField();
                playlistTitleTextField.setText(playlistTitleLabel.getText());
            }
        });

        initializePlaylistTitleTextField();
    }

    private void initializePlaylistTitleTextField() {
        playlistTitleTextField = new TextField();
        playlistTitleTextField.setMaxWidth(350);
        playlistTitleTextField.setPrefHeight(25);
        playlistTitleTextField.setPadding(new Insets(-10, 0, -10, 0));
        playlistTitleTextField.setFont(new Font("Avenir", 19));
        VBox.setMargin(playlistTitleTextField, new Insets(30, 0, 5, 15));
        playlistTitleTextField.setOnKeyPressed(changePlaylistNameTextFieldHandler);
    }

    private EventHandler<KeyEvent> changePlaylistNameTextFieldHandler() {
        return event -> {
            if (event.getCode() == KeyCode.ENTER) {
                selectedPlaylistProperty.get().ifPresent(playlist -> {
                    var newName = playlistTitleTextField.getText();
                    if (isValidPlaylistName(newName) || playlist.getNameProperty().get().equals(newName)) {
                        playlist.setName(newName);
                        replacePlaylistTextFieldByTitleLabel();
                    }
                    event.consume();
                });
            } else if (event.getCode() == KeyCode.ESCAPE) {
                replacePlaylistTextFieldByTitleLabel();
                event.consume();
            }
        };
    }

    /**
     * Ensures that a string for a playlist is valid, checking if
     * it is empty, or another playlist has the same name.
     *
     * @param newName The name of the playlist to check
     * @return {@code true} if its a valid name, {@code false} otherwise
     */
    private boolean isValidPlaylistName(String newName) {
        return !newName.isEmpty() && !navigationController.containsPlaylistName(newName);
    }

    private void initializeTrackHoveredCoverImageView() {
        trackHoveredCoverImageView = new ImageView();
        trackHoveredCoverImageProperty = new SimpleObjectProperty<>(this, "track hovered cover image", defaultPlaylistImage);
        trackHoveredCoverImageView.imageProperty().bind(trackHoveredCoverImageProperty);
        trackHoveredCoverImageView.setFitWidth(HOVER_COVER_SIZE);
        trackHoveredCoverImageView.setFitHeight(HOVER_COVER_SIZE);
        trackHoveredCoverImageView.translateXProperty().bind(
            map(tableStackPane.widthProperty(),
                width -> (width.doubleValue() / 2) - (HOVER_COVER_SIZE / 2) - 10
            ));
        trackHoveredCoverImageView.translateYProperty().bind(
            map(tableStackPane.heightProperty(),
                height -> (height.doubleValue() / 2) - (HOVER_COVER_SIZE / 2) - 27
            ));
    }

    /**
     * Puts a text field to edit the name of the playlist
     */
    private void replacePlaylistTitleLabelByTextField() {
        showTableInfoPane();
        if (!playlistInfoGridPane.getChildren().contains(playlistTitleTextField)) {
            playlistInfoGridPane.getChildren().remove(playlistTitleLabel);
            playlistInfoGridPane.add(playlistTitleTextField, 0, 0);
            playlistTitleTextField.requestFocus();
        }
    }

    /**
     * Removes the text field and shows the label with the title of the selected or entered playlist
     */
    private void replacePlaylistTextFieldByTitleLabel() {
        showTableInfoPane();
        if (!playlistInfoGridPane.getChildren().contains(playlistTitleLabel)) {
            playlistInfoGridPane.getChildren().remove(playlistTitleTextField);
            playlistInfoGridPane.add(playlistTitleLabel, 0, 0);
        }
    }

    private void showAllAudioItemsView() {
        tableStackPane.getChildren().remove(artistsLayout);
        trackHoveredCoverImageView.setVisible(true);
        hideTableInfoPane();
        showTablePane();
    }

    private void showTablePane() {
        if (!tableStackPane.getChildren().contains(mainTrackTable)) {
            tableStackPane.getChildren().add(mainTrackTable);
        }
        if (!tableStackPane.getChildren().contains(trackHoveredCoverImageView)) {
            tableStackPane.getChildren().add(trackHoveredCoverImageView);
        }
    }

    private void showArtistsView() {
        hideTableInfoPane();
        trackHoveredCoverImageView.setVisible(false);
        tableStackPane.getChildren().remove(mainTrackTable);
        tableStackPane.getChildren().remove(trackHoveredCoverImageView);
        artistViewController.checkSelectedArtist();
        if (!tableStackPane.getChildren().contains(artistsLayout)) {
            tableStackPane.getChildren().remove(tableBorderPane);
            tableStackPane.getChildren().add(artistsLayout);
            logger.debug("Showing artists view pane");
        }
    }

    private void showPlaylistView() {
        if (selectedPlaylistProperty.get().isPresent()) {
            //            trackHoveredCoverImageView.setVisible(false); // TODO Changed, now showing, test.

            var playlist = selectedPlaylistProperty.get().get();
            subscribe(playlist.getCoverImageProperty(),
                playlistCover -> playlistImageProperty.set(playlistCover.orElse(defaultPlaylistImage))
            );

            replacePlaylistTextFieldByTitleLabel();
            // TODO this may be unnecessary and only set operation here and at inside changePlaylistNameTextFieldHandler would work
            playlistTitleLabel.textProperty().bind(playlist.getNameProperty());

            showingTracksProperty.clear();
            showingTracksProperty.bind(playlist.getAudioItemsProperty());  // TODO test if is necessary to unbind somewhere when changing content
        } else {
            // If there was no present playlist object in the observable, something went wrong, since
            // this method is to be executed when a playlist is selected
            logger.warn("Attempt to show playlist unsuccessful");
        }
    }

    public void showNavigationPane() {
        if (!wrapperBorderPane.getChildren().equals(navigationLayout)) {
            wrapperBorderPane.setLeft(navigationLayout);
            logger.debug("Showing navigation pane");
        }
    }

    public void hideNavigationPane() {
        if (wrapperBorderPane.getChildren().contains(navigationLayout)) {
            wrapperBorderPane.getChildren().remove(navigationLayout);
            logger.debug("Showing navigation pane");
        }
    }

    public void showTableInfoPane() {
        if (!contentBorderPane.getChildren().contains(tableInfoHBox)) {
            contentBorderPane.setTop(tableInfoHBox);
            logger.debug("Showing info pane");
        }
    }

    public void hideTableInfoPane() {
        if (contentBorderPane.getChildren().contains(tableInfoHBox)) {
            contentBorderPane.getChildren().remove(tableInfoHBox);
            logger.debug("Hiding info pane");
        }
    }

    private Optional<ObservableList<ObservableAudioItem>> selectedAudioItems() {
        var mode = navigationController.navigationModeProperty().getValue();
        return switch (mode) {
            case ALL_AUDIO_ITEMS, PLAYLIST -> Optional.ofNullable(mainTrackTable.getSelectionModel().getSelectedItems());
            case ARTISTS -> Optional.ofNullable(artistViewController.getSelectedTracks());
        };
    }

    public void deleteSelectedAudioItems() {
        selectedAudioItems().ifPresent(audioItems -> audioRepository.removeAll(new HashSet<>(audioItems)));
    }

    public void selectAllTracks() {
        var mode = navigationController.navigationModeProperty().get();
        switch (mode) {
            case ALL_AUDIO_ITEMS, PLAYLIST:
                mainTrackTable.getSelectionModel().selectAll();
                break;
            case ARTISTS:
                artistViewController.selectAllTracks();
                break;
        }
    }

    public void unselectTracks() {
        var mode = navigationController.navigationModeProperty().get();
        switch (mode) {
            case ALL_AUDIO_ITEMS, PLAYLIST:
                mainTrackTable.getSelectionModel().clearSelection();
                break;
            case ARTISTS:
                artistViewController.deselectAllTracks();
                break;
        }
    }

    public void selectCurrentPlayingTrack() {
        var currentPlayingTrack = playerController.currentTrack();
        currentPlayingTrack.ifPresent(track -> {
            var mode = navigationController.navigationModeProperty().getValue();
            if (mode == ALL_AUDIO_ITEMS || mode == PLAYLIST) {
                mainTrackTable.selectFocusAndScroll(track);
            } else if (mode == NavigationController.NavigationMode.ARTISTS) {
                artistViewController.findAudioItemInArtistViewAndSelect(track);
            }
        });
    }

    /**
     * Handles the naming of a new playlist placing a {@link TextField} on top
     * of the playlist label asking the user for the name.
     */
    @EventListener(classes = CreatePlaylistEvent.class)
    public void createPlaylistEventListener() {
        changeViewToPlaylistCreationMode();
    }

    private void changeViewToPlaylistCreationMode() {
        showingTracksProperty.clear();
        tableStackPane.getChildren().remove(artistsLayout);
        replacePlaylistTitleLabelByTextField();

        playlistTitleTextField.clear();
        playlistTitleTextField.setOnKeyPressed(onPlaylistTitleTextFieldKeyPressed());
    }

    private EventHandler<KeyEvent> onPlaylistTitleTextFieldKeyPressed() {
        return event -> {
            String newPlaylistName = playlistTitleTextField.getText();
            if (event.getCode() == KeyCode.ENTER && isValidPlaylistName(newPlaylistName)) {
                try {
                    ObservablePlaylist newPlaylist = playlistRepository.createPlaylist(newPlaylistName);

                    subscribe(newPlaylist.getCoverImageProperty(),
                        playlistCover -> playlistImageProperty.set(playlistCover.orElse(defaultPlaylistImage))
                    );
                    replacePlaylistTextFieldByTitleLabel();
                    // Necessary to enable changing the name of the playlist afterwards
                    playlistTitleTextField.setOnKeyPressed(changePlaylistNameTextFieldHandler);

                    navigationController.addNewPlaylist(newPlaylist);
                } catch (IllegalArgumentException exception) {
                    logger.error("Attempted to create playlist with existing name", exception);
                    applicationEventPublisher.publishEvent(new ExceptionEvent(exception, this));
                } finally {
                    event.consume();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                replacePlaylistTextFieldByTitleLabel();
                selectedPlaylistProperty.get().ifPresentOrElse(navigationController::selectPlaylist, navigationController::selectFirstPlaylist);
                event.consume();
            }
        };
    }

    @EventListener(classes = CreatePlaylistDirectoryEvent.class)
    public void createPlaylistFolderEventListener() {
        changeViewToPlaylistFolderCreationMode();
    }

    private void changeViewToPlaylistFolderCreationMode() {
        showingTracksProperty.clear();
        tableStackPane.getChildren().remove(artistsLayout);
        replacePlaylistTitleLabelByTextField();

        playlistTitleTextField.clear();
        playlistTitleTextField.setOnKeyPressed(onPlaylistFolderTitleTextFieldKeyPressed());
    }

    private EventHandler<KeyEvent> onPlaylistFolderTitleTextFieldKeyPressed() {
        return event -> {
            String newPlaylistName = playlistTitleTextField.getText();
            if (event.getCode() == KeyCode.ENTER && isValidPlaylistName(newPlaylistName)) {
                try {
                    ObservablePlaylist newPlaylistDirectory = playlistRepository.createPlaylistDirectory(newPlaylistName);

                    subscribe(newPlaylistDirectory.getCoverImageProperty(),
                        playlistCover -> playlistImageProperty.set(playlistCover.orElse(defaultPlaylistImage))
                    );
                    replacePlaylistTextFieldByTitleLabel();
                    // Necessary to enable changing the name of the playlist afterwards
                    playlistTitleTextField.setOnKeyPressed(changePlaylistNameTextFieldHandler);

                    navigationController.addNewPlaylist(newPlaylistDirectory);
                } catch (IllegalArgumentException exception) {
                    logger.error("Attempted to create playlist directory with existing name", exception);
                    applicationEventPublisher.publishEvent(new ExceptionEvent(exception, this));
                } finally {
                    event.consume();
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                replacePlaylistTextFieldByTitleLabel();
                selectedPlaylistProperty.get()
                    .ifPresentOrElse(navigationController::selectPlaylist, navigationController::selectFirstPlaylist);
                event.consume();
            }
        };
    }

    @EventListener(classes = DeleteSelectedPlaylistEvent.class)
    public void deleteSelectedPlaylistEventListener() {
        selectedPlaylistProperty.get().ifPresent(selectedPlaylist -> {
            playlistRepository.remove(selectedPlaylist);
            navigationController.deletePlaylist(selectedPlaylist);
        });
        showAllAudioItemsView();
    }

    @EventListener
    public void updateFloatingTrackCoverEventListener(AudioItemHoveredEvent audioItemHoveredEvent) {
        var audioItem = audioItemHoveredEvent.audioItem;
        trackHoveredCoverImageProperty.setValue(audioItem.getCoverImageProperty().get().orElse(defaultPlaylistImage));
    }

    @EventListener(classes = EditionFinishedEvent.class)
    public void editionFinishedEventListener() {
        menuBarController.selectAllMenuItem.setDisable(false);
        menuBarController.unselectMenuItem.setDisable(false);
    }

    public void focusSearchField() {
        searchTextField.requestFocus();
    }

    public class MenuBarController {

        @FXML
        private MenuBar rootMenuBar;
        @FXML
        private Menu fileMenu;
        @FXML
        private MenuItem openFileMenuItem;
        @FXML
        private MenuItem importFolderMenuItem;
        @FXML
        private MenuItem importItunesMenuItem;
        @FXML
        private MenuItem newPlaylistMenuItem;
        @FXML
        private MenuItem newPlaylistFolderMenuItem;
        @FXML
        private MenuItem preferencesMenuItem;
        @FXML
        private MenuItem closeMenuItem;
        @FXML
        private Menu editMenu;
        @FXML
        private MenuItem editMenuItem;
        @FXML
        private MenuItem deleteMenuItem;
        @FXML
        private MenuItem selectAllMenuItem;
        @FXML
        private MenuItem unselectMenuItem;
        @FXML
        private MenuItem findMenuItem;
        @FXML
        private Menu controlsMenu;
        @FXML
        private MenuItem playPauseMenuItem;
        @FXML
        private MenuItem previousMenuItem;
        @FXML
        private MenuItem nextMenuItem;
        @FXML
        private MenuItem increaseVolumeMenuItem;
        @FXML
        private MenuItem decreaseVolumeMenuItem;
        @FXML
        private MenuItem selectCurrentTrackMenuItem;
        @FXML
        private Menu viewMenu;
        @FXML
        private MenuItem showHideNavigationPaneMenuItem;
        @FXML
        private MenuItem showHideTableInfoPaneMenuItem;
        @FXML
        private Menu aboutMenu;
        @FXML
        private MenuItem aboutMenuItem;

        private Alert aboutWindowAlert;

        @FXML
        public void initialize() {
            setFileMenuActions();
            setEditMenuActions();
            setControlsMenuActions();
            setViewMenuActions();
            setAboutMenuAction();
            bindShowHideTableInfo();

            if (OS_SPECIFIC_KEY_MODIFIER.equals(KeyCodeCombination.META_DOWN)) {
                MenuToolkit menuToolkit = MenuToolkit.toolkit();
                Menu appMenu = new Menu("Musicott");
                appMenu.getItems().addAll(preferencesMenuItem, new SeparatorMenuItem());
                appMenu.getItems().add(menuToolkit.createQuitMenuItem("Musicott"));
                Menu windowMenu = new Menu("Window");
                windowMenu.getItems().addAll(menuToolkit.createMinimizeMenuItem(), menuToolkit.createCloseWindowMenuItem());
                windowMenu.getItems().addAll(menuToolkit.createZoomMenuItem(), new SeparatorMenuItem());
                windowMenu.getItems().addAll(menuToolkit.createHideOthersMenuItem(), menuToolkit.createUnhideAllMenuItem());
                windowMenu.getItems().addAll(menuToolkit.createBringAllToFrontItem());

                fileMenu.getItems().remove(5, 8);
                menuToolkit.setApplicationMenu(appMenu);
                rootMenuBar.getMenus().add(0, appMenu);
                rootMenuBar.getMenus().add(5, windowMenu);
                menuToolkit.autoAddWindowMenuItems(windowMenu);
                menuToolkit.setGlobalMenuBar(rootMenuBar);
            } else {
                headerVBox.getChildren().add(rootMenuBar);
            }

            setAccelerators();
        }

        private void setAboutMenuAction() {
            aboutMenuItem.setOnAction(e -> {
                if (aboutWindowAlert == null) {
                    aboutWindowAlert = alertFactory.aboutWindowAlert();
                }
                aboutWindowAlert.showAndWait();
            });
        }

        private void setFileMenuActions() {
            openFileMenuItem.setOnAction(e -> {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Open file(s)...");
                chooser.getExtensionFilters().addAll(buildAudioExtensionFilters());
                List<File> filesToOpen = chooser.showOpenMultipleDialog(rootBorderPane.getScene().getWindow());
                mediaImportService.importFiles(filesToOpen);
            });
            importFolderMenuItem.setOnAction(e -> {
                //            LOG.debug("Choosing folder to being imported");
                //            DirectoryChooser chooser = new DirectoryChooser();
                //            chooser.setTitle("Choose folder");
                //            File folder = chooser.showDialog(primaryStage);
                //            if (folder != null)
                //                countFilesToImport(folder);
            });
            importItunesMenuItem.setOnAction(e -> {
                //            LOG.debug("Choosing Itunes xml file");
                //            File xmlFile = selectItunesFile();
                //            if (xmlFile != null)
                //                taskDemon.importFromItunesLibrary(xmlFile.getAbsolutePath());
            });
            preferencesMenuItem.setOnAction(e -> preferencesController.show());
            newPlaylistMenuItem.setOnAction(e -> changeViewToPlaylistCreationMode());
            newPlaylistFolderMenuItem.setOnAction(e -> changeViewToPlaylistCreationMode());
            closeMenuItem.setOnAction(e -> {
                // TODO free resources, finish tasks
                Platform.exit();
            });
        }

        private List<ExtensionFilter> buildAudioExtensionFilters() {
            Set<AudioFileType> acceptedAudioFileExtensions = settingsRepository.getAcceptedAudioFileExtensions();

            if (acceptedAudioFileExtensions.isEmpty()) {
                return Collections.emptyList();
            }

            List<ExtensionFilter> filters = new ArrayList<>();
            List<String> allExtensions = new ArrayList<>();
            Map<AudioFileType, String> extensionMap = new EnumMap<>(AudioFileType.class);

            // First, build all the wildcard strings and store them
            for (AudioFileType type : acceptedAudioFileExtensions) {
                String wildcardExtension = "*." + type.getExtension();
                extensionMap.put(type, wildcardExtension);
                allExtensions.add(wildcardExtension);
            }

            // Add the "All Supported" filter first
            StringBuilder allSupportedTitle = new StringBuilder("All Supported (");
            Iterator<AudioFileType> iterator = acceptedAudioFileExtensions.iterator();

            while (iterator.hasNext()) {
                allSupportedTitle.append("*.").append(iterator.next().getExtension());
                if (iterator.hasNext()) {
                    allSupportedTitle.append(", ");
                }
            }
            allSupportedTitle.append(")");

            filters.add(new ExtensionFilter(allSupportedTitle.toString(),
                    allExtensions.toArray(new String[0])));

            // Add individual filters for each extension
            for (AudioFileType type : acceptedAudioFileExtensions) {
                String extension = type.getExtension();
                String wildcardExtension = extensionMap.get(type);
                filters.add(new ExtensionFilter(
                        extension + " files (*." + extension + ")",
                        wildcardExtension
                ));
            }

            return filters;
        }

        private void setViewMenuActions() {
            showHideNavigationPaneMenuItem.setOnAction(e -> {
                if (showHideNavigationPaneMenuItem.getText().startsWith("Show")) {
                    showHideNavigationPaneMenuItem.setText("Hide navigation pane");
                    showNavigationPane();
                } else if (showHideNavigationPaneMenuItem.getText().startsWith("Hide")) {
                    showHideNavigationPaneMenuItem.setText("Show navigation pane");
                    hideNavigationPane();
                }
            });

            showHideTableInfoPaneMenuItem.setOnAction(e -> {
                if (showHideTableInfoPaneMenuItem.getText().startsWith("Show")) {
                    showHideTableInfoPaneMenuItem.setText("Hide table information pane");
                    showTableInfoPane();
                } else if (showHideTableInfoPaneMenuItem.getText().startsWith("Hide")) {
                    showHideTableInfoPaneMenuItem.setText("Show table information pane");
                    hideTableInfoPane();
                }
            });
        }

        private void bindShowHideTableInfo() {
            showHideTableInfoPaneMenuItem.disableProperty().bind(
                map(navigationController.navigationModeProperty(), menu -> !menu.equals(PLAYLIST)));
        }

        private void setAccelerators() {
            KeyCombination.Modifier shiftDown = KeyCombination.SHIFT_DOWN;
            openFileMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, OS_SPECIFIC_KEY_MODIFIER));
            importFolderMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, OS_SPECIFIC_KEY_MODIFIER, shiftDown));
            importItunesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.I, OS_SPECIFIC_KEY_MODIFIER, shiftDown));
            preferencesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, OS_SPECIFIC_KEY_MODIFIER));
            editMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.I, OS_SPECIFIC_KEY_MODIFIER));
            deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.BACK_SPACE, OS_SPECIFIC_KEY_MODIFIER));
            playPauseMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.SPACE));
            previousMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.LEFT, OS_SPECIFIC_KEY_MODIFIER));
            nextMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.RIGHT, OS_SPECIFIC_KEY_MODIFIER));
            increaseVolumeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.UP, OS_SPECIFIC_KEY_MODIFIER));
            decreaseVolumeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DOWN, OS_SPECIFIC_KEY_MODIFIER));
            selectCurrentTrackMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, OS_SPECIFIC_KEY_MODIFIER));
            newPlaylistMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, OS_SPECIFIC_KEY_MODIFIER));
            newPlaylistFolderMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, OS_SPECIFIC_KEY_MODIFIER, shiftDown));
            showHideNavigationPaneMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.R, OS_SPECIFIC_KEY_MODIFIER, shiftDown));
            showHideTableInfoPaneMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.U, OS_SPECIFIC_KEY_MODIFIER, shiftDown));
            selectAllMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.A, OS_SPECIFIC_KEY_MODIFIER));
            unselectMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.A, OS_SPECIFIC_KEY_MODIFIER, shiftDown));
            findMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, OS_SPECIFIC_KEY_MODIFIER));
            closeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, OS_SPECIFIC_KEY_MODIFIER));
        }

        private void setControlsMenuActions() {
            playPauseMenuItem.textProperty().bind(map(playerController.playButtonSelectedProperty(), play -> play ? "Pause" : "Play"));
            playPauseMenuItem.setOnAction(e -> playerController.playPause());
            previousMenuItem.disableProperty().bind(playerController.previousButtonDisabledProperty());
            previousMenuItem.setOnAction(e -> playerController.previous());

            nextMenuItem.disableProperty().bind(playerController.nextButtonDisabledProperty());
            nextMenuItem.setOnAction(e -> playerController.next());

            increaseVolumeMenuItem.setOnAction(e -> playerController.increaseVolume());
            decreaseVolumeMenuItem.setOnAction(e -> playerController.decreaseVolume());

            selectCurrentTrackMenuItem.setOnAction(e -> selectCurrentPlayingTrack());
        }

        private void setEditMenuActions() {
            // When editing tracks or using the search text field, Select All and Deselect All are disabled because
            // their Accelerators can be used when typing text: CTRL + A and CTRL + SHIFT + A, which are used to select or deselect text
            // inside a text field too, by default.
            // Menus are enabled again when the search text field lost focus or the edition was completed, which is handled in
            // trackEditingFinishedEventListener
            editMenuItem.setOnAction(e ->
                selectedAudioItems().ifPresent(audioItems -> {
                    applicationEventPublisher.publishEvent(new OpenAudioItemEditorView(new HashSet<>(audioItems), this));
                    selectAllMenuItem.setDisable(true);
                    unselectMenuItem.setDisable(true);
                }));
            searchTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    selectAllMenuItem.setDisable(true);
                    unselectMenuItem.setDisable(true);
                } else {
                    selectAllMenuItem.setDisable(false);
                    unselectMenuItem.setDisable(false);
                }
            });

            deleteMenuItem.setOnAction(e -> deleteSelectedAudioItems());
            selectAllMenuItem.setOnAction(e -> selectAllTracks());
            unselectMenuItem.setOnAction(e -> unselectTracks());
            findMenuItem.setOnAction(e -> focusSearchField());
        }
    }
}
