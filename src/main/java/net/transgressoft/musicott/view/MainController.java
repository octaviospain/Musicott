package net.transgressoft.musicott.view;

import de.codecentric.centerdevice.MenuToolkit;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxmlView;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.audio.AudioFileType;
import net.transgressoft.musicott.config.*;
import net.transgressoft.musicott.config.*;
import net.transgressoft.musicott.events.*;
import net.transgressoft.musicott.service.MediaImportService;
import net.transgressoft.musicott.view.custom.ApplicationImage;
import net.transgressoft.musicott.view.custom.alerts.AlertFactory;
import net.transgressoft.musicott.view.custom.table.FullAudioItemTableView;
import org.apache.commons.io.FileUtils;
import org.fxmisc.easybind.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

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

    private final ObservableAudioLibrary audioRepository;
    private final ObservablePlaylistHierarchy playlistRepository;

    private final PlayerController playerController;
    private final PreferencesController preferencesController;
    private final NavigationController navigationController;
    private final ArtistViewController artistViewController;
    private final AlertFactory alertFactory;
    private final SettingsRepository settingsRepository;
    private final MediaImportService mediaImportService;
    private final ApplicationContext applicationContext;

    @Autowired
    private FxControllerAndView<ItunesPlaylistsPickerController, Parent> itunesPickerControllerAndView;

    private final ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty;

    private Subscription playlistCoverSubscription;
    private Subscription playlistItemsSubscription;

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
    
    @Autowired
    private KeyCombination.Modifier operativeSystemKeyModifier;

    /**
     * The table where tracks are displayed
     */
    private FullAudioItemTableView mainAudioItemTable;

    /**
     * The TextField that is shown to set a new playlist name or rename an existing one
     */
    private TextField playlistTitleTextField;

    /**
     * The image that is shown in the bottom right corner of the table when hovering over a track
     */
    private ImageView miniatureCoverImageView;

    /**
     * The object property of the image shown in the trackHoveredCoverImageView object
     */
    private ObjectProperty<Image> miniatureCoverImageProperty;

    /**
     * The object property of the image of the playlist in the playlistImageView
     */
    private ObjectProperty<Image> playlistImageProperty;

    private AudioItemTableViewContextMenu audioItemContextMenu;

    @Autowired
    public MainController(ObservableAudioLibrary audioRepository,
                          ObservablePlaylistHierarchy playlistRepository,
                          PlayerController playerController,
                          PreferencesController preferencesController,
                          NavigationController navigationController,
                          ArtistViewController artistViewController,
                          AlertFactory alertFactory,
                          MediaImportService mediaImportService,
                          SettingsRepository settingsRepository,
                          ApplicationContext applicationContext) {
        this.audioRepository = audioRepository;
        this.playlistRepository = playlistRepository;
        this.playerController = playerController;
        this.preferencesController = preferencesController;
        this.navigationController = navigationController;
        this.artistViewController = artistViewController;
        this.alertFactory = alertFactory;
        this.mediaImportService = mediaImportService;
        this.settingsRepository = settingsRepository;
        this.applicationContext = applicationContext;
        this.selectedPlaylistProperty = navigationController.selectedPlaylistProperty();
    }

    @FXML
    public void initialize() throws IOException {
        initMenuBar();
        initAudioItemTable();
        initializeMiniatureCoverImageView();
        initializeInfoPaneFields();

        playlistImageProperty = new SimpleObjectProperty<>(this, "playlist image", defaultPlaylistImage);
        playlistImageView.imageProperty().bind(playlistImageProperty);

        navigationController.setOnNewPlaylistAction(e -> changeViewToPlaylistCreationMode(playlistRepository::createPlaylist));
        navigationController.setOnNewFolderPlaylistAction(e -> changeViewToPlaylistCreationMode(playlistRepository::createPlaylistDirectory));
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

        selectedPlaylistProperty.addListener((_, _, newPlaylist) -> {
            if (newPlaylist.isPresent() && navigationController.navigationModeProperty().get() == PLAYLIST) {
                showPlaylistView();
            }
        });

        audioRepository.getEmptyLibraryProperty().addListener((observable, wasEmpty, isEmpty) -> {
            if (Boolean.TRUE.equals(isEmpty)) {
                miniatureCoverImageView.imageProperty().unbind();
                miniatureCoverImageView.setImage(defaultPlaylistImage);
                menuBarController.playPauseMenuItem.setDisable(true);
            } else {
                menuBarController.playPauseMenuItem.setDisable(false);
            }
        });

        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() >= 3) {
                applicationContext.publishEvent(new SearchTextTypedEvent(newValue, this));
            } else if (newValue.isEmpty() || newValue.length() < 3) {
                applicationContext.publishEvent(new SearchTextTypedEvent("", this));
            }
        });

        audioItemContextMenu = new AudioItemTableViewContextMenu();

        hideTableInfoPane();
    }

    private void initMenuBar() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MenuBarController.fxml"));
        menuBarController = new MenuBarController();
        loader.setController(menuBarController);
        loader.load();
    }

    private void initAudioItemTable() {
        mainAudioItemTable = applicationContext.getBean(FullAudioItemTableView.class);
        mainAudioItemTable.setSourceItems(audioRepository.getAudioItemsProperty());

        GridPane.setHgrow(mainAudioItemTable, Priority.ALWAYS);
        GridPane.setVgrow(mainAudioItemTable, Priority.ALWAYS);
    }

    private void initializeInfoPaneFields() {
        var tableItemsProperty = new SimpleListProperty<>(mainAudioItemTable.getItems());
        playRandomButton.visibleProperty().bind(tableItemsProperty.emptyProperty().not());
        playlistTracksNumberLabel.textProperty().bind(map(tableItemsProperty.sizeProperty(), s -> s + " tracks"));

        playRandomButton.setOnAction(e -> selectedPlaylistProperty.get().ifPresent(
            playlist -> applicationContext.publishEvent(new PlayPlaylistRandomlyEvent(playlist, this))));

        playlistSizeLabel.textProperty().bind(map(tableItemsProperty, (ObservableList<ObservableAudioItem> audioItems) -> {
            var sizeSum = audioItems.stream().mapToLong(ObservableAudioItem::getLength).sum();
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
        playlistTitleTextField.setOnKeyPressed(this::changePlaylistNameTextFieldHandler);
    }

    private void changePlaylistNameTextFieldHandler(KeyEvent event) {
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
    }

    private boolean isValidPlaylistName(String newName) {
        return ! newName.isEmpty() && ! navigationController.containsPlaylistName(newName);
    }

    private void initializeMiniatureCoverImageView() {
        miniatureCoverImageView = new ImageView();
        miniatureCoverImageProperty = new SimpleObjectProperty<>(this, "miniature cover image", defaultPlaylistImage);
        miniatureCoverImageView.imageProperty().bind(miniatureCoverImageProperty);
        miniatureCoverImageView.setFitWidth(HOVER_COVER_SIZE);
        miniatureCoverImageView.setFitHeight(HOVER_COVER_SIZE);
        miniatureCoverImageView.translateXProperty().bind(
            map(tableStackPane.widthProperty(),
                width -> (width.doubleValue() / 2) - (HOVER_COVER_SIZE / 2.0) - 10
            ));
        miniatureCoverImageView.translateYProperty().bind(
            map(tableStackPane.heightProperty(),
                height -> (height.doubleValue() / 2) - (HOVER_COVER_SIZE / 2.0) - 27
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
        mainAudioItemTable.setSourceItems(audioRepository.getAudioItemsProperty());
        tableStackPane.getChildren().remove(artistsLayout);
        miniatureCoverImageView.setVisible(true);
        hideTableInfoPane();
        showTablePane();
    }

    private void showTablePane() {
        if (!tableStackPane.getChildren().contains(mainAudioItemTable)) {
            tableStackPane.getChildren().add(mainAudioItemTable);
        }
        if (!tableStackPane.getChildren().contains(miniatureCoverImageView)) {
            tableStackPane.getChildren().add(miniatureCoverImageView);
        }
    }

    private void showArtistsView() {
        hideTableInfoPane();
        miniatureCoverImageView.setVisible(false);
        tableStackPane.getChildren().remove(mainAudioItemTable);
        tableStackPane.getChildren().remove(miniatureCoverImageView);
//        artistViewController.checkForNullSelectedArtist();
        if (!tableStackPane.getChildren().contains(artistsLayout)) {
            tableStackPane.getChildren().remove(tableBorderPane);
            tableStackPane.getChildren().add(artistsLayout);
        }
    }

    private void showPlaylistView() {
        if (selectedPlaylistProperty.get().isPresent()) {
            var playlist = selectedPlaylistProperty.get().get();

            if (playlistCoverSubscription != null)
                playlistCoverSubscription.unsubscribe();
            if (playlistItemsSubscription != null)
                playlistItemsSubscription.unsubscribe();

            playlistCoverSubscription = subscribe(playlist.getCoverImageProperty(),
                playlistCover -> playlistImageProperty.set(playlistCover.orElse(defaultPlaylistImage))
            );

            tableStackPane.getChildren().remove(artistsLayout);
            replacePlaylistTextFieldByTitleLabel();
            playlistTitleLabel.textProperty().unbind();
            playlistTitleLabel.textProperty().bind(playlist.getNameProperty());

            mainAudioItemTable.setSourceItems(playlist.getAudioItemsProperty());
            boolean hasItems = ! playlist.getAudioItemsProperty().isEmpty();
            miniatureCoverImageView.setVisible(hasItems);
            playlistTracksNumberLabel.setVisible(hasItems);
            playlistSizeLabel.setVisible(hasItems);

            playlistItemsSubscription = subscribe(playlist.getAudioItemsProperty(), audioItems -> {
                boolean notEmpty = ! audioItems.isEmpty();
                miniatureCoverImageView.setVisible(notEmpty);
                playlistTracksNumberLabel.setVisible(notEmpty);
                playlistSizeLabel.setVisible(notEmpty);
            });

            showTablePane();
        } else {
            // If there was no present playlist object in the observable, something went wrong, since
            // this method is to be executed when a playlist is selected
            logger.warn("Attempt to show playlist unsuccessful");
        }
    }

    public void showNavigationPane() {
        if (!wrapperBorderPane.getChildren().equals(navigationLayout)) {
            wrapperBorderPane.setLeft(navigationLayout);
        }
    }

    public void hideNavigationPane() {
        wrapperBorderPane.getChildren().remove(navigationLayout);
    }

    public void showTableInfoPane() {
        if (!contentBorderPane.getChildren().contains(tableInfoHBox)) {
            contentBorderPane.setTop(tableInfoHBox);
        }
    }

    public void hideTableInfoPane() {
        contentBorderPane.getChildren().remove(tableInfoHBox);
    }

    public void selectCurrentPlayingTrack() {
        var currentPlayingTrack = playerController.currentTrack();
        currentPlayingTrack.ifPresent(track -> {
            var mode = navigationController.navigationModeProperty().getValue();
            if (mode == ALL_AUDIO_ITEMS || mode == PLAYLIST) {
                mainAudioItemTable.selectFocusAndScroll(track);
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
        changeViewToPlaylistCreationMode(playlistRepository::createPlaylist);
    }

    @EventListener(classes = CreatePlaylistDirectoryEvent.class)
    public void createPlaylistFolderEventListener() {
        changeViewToPlaylistCreationMode(playlistRepository::createPlaylistDirectory);
    }

    private void changeViewToPlaylistCreationMode(Function<String, ObservablePlaylist> playlistCreationFunction) {
        mainAudioItemTable.setSourceItems(FXCollections.observableArrayList());
        tableStackPane.getChildren().remove(artistsLayout);
        replacePlaylistTitleLabelByTextField();
        playlistImageProperty.set(defaultPlaylistImage);
        miniatureCoverImageView.setVisible(false);
        playlistTracksNumberLabel.setVisible(false);
        playlistSizeLabel.setVisible(false);
        showTablePane();

        playlistTitleTextField.clear();
        playlistTitleTextField.setOnKeyPressed(keyEvent ->
                onPlaylistTitleTextFieldKeyPressed(keyEvent, playlistCreationFunction));
    }

    private void onPlaylistTitleTextFieldKeyPressed(KeyEvent event, Function<String, ObservablePlaylist> playlistCreationFunction) {
        String newPlaylistName = playlistTitleTextField.getText();

        // If the user presses Enter, we check if the name is valid and create the playlist
        if (event.getCode() == KeyCode.ENTER && isValidPlaylistName(newPlaylistName)) {
            try {
                var newPlaylist = playlistCreationFunction.apply(newPlaylistName);

                subscribe(newPlaylist.getCoverImageProperty(),
                    playlistCover -> playlistImageProperty.set(playlistCover.orElse(defaultPlaylistImage))
                );
                replacePlaylistTextFieldByTitleLabel();
                // Necessary to enable changing the name of the playlist afterward
                playlistTitleTextField.setOnKeyPressed(this::changePlaylistNameTextFieldHandler);

                navigationController.addNewPlaylist(newPlaylist);
            } catch (IllegalArgumentException exception) {
                logger.error("Attempted to create playlist with existing name", exception);
                applicationContext.publishEvent(new ExceptionEvent(exception, this));
            } finally {
                event.consume();
            }
        // If the user presses Escape, we cancel the creation of the playlist
        } else if (event.getCode() == KeyCode.ESCAPE) {
            replacePlaylistTextFieldByTitleLabel();
            selectedPlaylistProperty.get().ifPresentOrElse(navigationController::selectPlaylist, navigationController::selectFirstPlaylist);
            event.consume();
        }
    }

    @EventListener(classes = DeleteSelectedPlaylistEvent.class)
    public void deleteSelectedPlaylistEventListener() {
        var selectedPlaylists = navigationController.selectedPlaylists();
        if (selectedPlaylists.isEmpty()) {
            return;
        }

        // Show confirmation for the first selected playlist (representative)
        var first = selectedPlaylists.getFirst();
        int totalItems = selectedPlaylists.stream().mapToInt(p -> p.getAudioItemsProperty().size()).sum();
        boolean hasFolder = selectedPlaylists.stream().anyMatch(ObservablePlaylist::isDirectory);
        int nestedCount = selectedPlaylists.stream()
                .filter(ObservablePlaylist::isDirectory)
                .mapToInt(p -> p.getPlaylists().size()).sum();

        String name = selectedPlaylists.size() == 1 ? first.getName() : selectedPlaylists.size() + " playlists";
        Alert confirmation = alertFactory.deletePlaylistConfirmationAlert(name, totalItems, hasFolder, nestedCount);
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                List.copyOf(selectedPlaylists).forEach(navigationController::deletePlaylist);
            }
        });
        showAllAudioItemsView();
    }

    @EventListener
    public void updateFloatingTrackCoverEventListener(AudioItemHoveredEvent audioItemHoveredEvent) {
        var audioItem = audioItemHoveredEvent.audioItem;
        miniatureCoverImageProperty.setValue(audioItem.getCoverImageProperty().get().orElse(defaultPlaylistImage));
    }

    @EventListener(classes = EditionFinishedEvent.class)
    public void editionFinishedEventListener() {
        menuBarController.selectAllMenuItem.setDisable(false);
        menuBarController.unselectMenuItem.setDisable(false);
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
            initFileMenuActions();
            initEditMenuActions();
            initControlsMenuActions();
            initViewMenuActions();
            initAboutMenuAction();
            bindShowHideTableInfo();
            initMenuBarItems();
            initAccelerators();
        }

        private void initAboutMenuAction() {
            aboutMenuItem.setOnAction(e -> {
                if (aboutWindowAlert == null) {
                    aboutWindowAlert = alertFactory.aboutWindowAlert();
                }
                aboutWindowAlert.showAndWait();
            });
        }

        private void initFileMenuActions() {
            var acceptedAudioFileExtensions = settingsRepository.getAcceptedAudioFileExtensions();
            openFileMenuItem.setOnAction(e -> {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Open file(s)...");
                chooser.getExtensionFilters().addAll(buildAudioExtensionFilters());
                var filesToOpen = chooser.showOpenMultipleDialog(rootBorderPane.getScene().getWindow());
                if (filesToOpen != null && ! filesToOpen.isEmpty()) {
                    mediaImportService.importFiles(filesToOpen);
                }
            });
            importFolderMenuItem.setOnAction(e -> {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle("Choose folder");
                File directory = chooser.showDialog(rootBorderPane.getScene().getWindow());
                if (directory != null)
                    mediaImportService.importDirectory(directory, acceptedAudioFileExtensions);
            });
            importItunesMenuItem.setOnAction(e -> {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Choose iTunes library XML");
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("iTunes library", "*.xml"));
                File file = chooser.showOpenDialog(rootBorderPane.getScene().getWindow());
                if (file != null) {
                    var library = mediaImportService.parseLibrary(file.toPath());
                    var controller = itunesPickerControllerAndView.getController();
                    controller.pickPlaylists(library.getPlaylists());

                    var view = itunesPickerControllerAndView.getView().orElseThrow();
                    var stage = new Stage();
                    stage.setScene(new Scene(view));
                    controller.setStage(stage);
                    stage.showAndWait();
                }
            });
            preferencesMenuItem.setOnAction(e -> preferencesController.show());
            newPlaylistMenuItem.setOnAction(e -> changeViewToPlaylistCreationMode(playlistRepository::createPlaylist));
            newPlaylistFolderMenuItem.setOnAction(e -> changeViewToPlaylistCreationMode(playlistRepository::createPlaylistDirectory));
            closeMenuItem.setOnAction(e -> applicationContext.publishEvent(new StopApplicationEvent(this)));
        }

        private List<ExtensionFilter> buildAudioExtensionFilters() {
            var acceptedAudioFileExtensions = settingsRepository.getAcceptedAudioFileExtensions();

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

        private void initViewMenuActions() {
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

        private void initMenuBarItems() {
            // If the host is running an Apple OS, we need to set the menu bar in a different way using the native menu bar
            if (operativeSystemKeyModifier.equals(KeyCombination.META_DOWN)) {
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
        }

        private void initAccelerators() {
            KeyCombination.Modifier shiftDown = KeyCombination.SHIFT_DOWN;
            openFileMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, operativeSystemKeyModifier));
            importFolderMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, operativeSystemKeyModifier, shiftDown));
            importItunesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.I, operativeSystemKeyModifier, shiftDown));
            preferencesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, operativeSystemKeyModifier));
            editMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.I, operativeSystemKeyModifier));
            deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.BACK_SPACE, operativeSystemKeyModifier));
            playPauseMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.SPACE));
            previousMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.LEFT, operativeSystemKeyModifier));
            nextMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.RIGHT, operativeSystemKeyModifier));
            increaseVolumeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.UP, operativeSystemKeyModifier));
            decreaseVolumeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DOWN, operativeSystemKeyModifier));
            selectCurrentTrackMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, operativeSystemKeyModifier));
            newPlaylistMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, operativeSystemKeyModifier));
            newPlaylistFolderMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, operativeSystemKeyModifier, shiftDown));
            showHideNavigationPaneMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.R, operativeSystemKeyModifier, shiftDown));
            showHideTableInfoPaneMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.U, operativeSystemKeyModifier, shiftDown));
            selectAllMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.A, operativeSystemKeyModifier));
            unselectMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.A, operativeSystemKeyModifier, shiftDown));
            findMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, operativeSystemKeyModifier));
            closeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, operativeSystemKeyModifier));
        }

        private void initControlsMenuActions() {
            playPauseMenuItem.textProperty().bind(
                    map(playerController.playButtonSelectedProperty(),
                            play -> Boolean.TRUE.equals(play) ? "Pause" : "Play"));
            playPauseMenuItem.setOnAction(e -> playerController.playPause());
            previousMenuItem.disableProperty().bind(playerController.previousButtonDisabledProperty());
            previousMenuItem.setOnAction(e -> playerController.previous());

            nextMenuItem.disableProperty().bind(playerController.nextButtonDisabledProperty());
            nextMenuItem.setOnAction(e -> playerController.next());

            increaseVolumeMenuItem.setOnAction(e -> playerController.increaseVolume());
            decreaseVolumeMenuItem.setOnAction(e -> playerController.decreaseVolume());

            selectCurrentTrackMenuItem.setOnAction(e -> selectCurrentPlayingTrack());
        }

        private void initEditMenuActions() {
            // When editing tracks or using the search text field, Select All and Deselect All are disabled because
            // their Accelerators can be used when typing text: CTRL + A and CTRL + SHIFT + A, which are used to select or deselect text
            // inside a text field too, by default.
            // Menus are enabled again when the search text field lost focus or the edition was completed, which is handled in
            // trackEditingFinishedEventListener
            editMenuItem.setOnAction(e ->
                selectedAudioItems().ifPresent(audioItems -> {
                    applicationContext.publishEvent(new OpenAudioItemEditorView(new HashSet<>(audioItems), this));
                    selectAllMenuItem.setDisable(true);
                    unselectMenuItem.setDisable(true);
                }));
            searchTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (Boolean.TRUE.equals(newValue)) {
                    selectAllMenuItem.setDisable(true);
                    unselectMenuItem.setDisable(true);
                } else {
                    selectAllMenuItem.setDisable(false);
                    unselectMenuItem.setDisable(false);
                }
            });

            deleteMenuItem.setOnAction(this::deleteSelectedAudioItems);
            selectAllMenuItem.setOnAction(this::selectAllTracks);
            unselectMenuItem.setOnAction(this::unselectTracks);
            findMenuItem.setOnAction(e -> searchTextField.requestFocus());
        }

        private Optional<ObservableList<ObservableAudioItem>> selectedAudioItems() {
            var mode = navigationController.navigationModeProperty().getValue();
            return switch (mode) {
                case ALL_AUDIO_ITEMS, PLAYLIST -> Optional.ofNullable(mainAudioItemTable.getSelectionModel().getSelectedItems());
                case ARTISTS -> Optional.ofNullable(artistViewController.getSelectedTracks());
            };
        }

        private void deleteSelectedAudioItems(ActionEvent event) {
            selectedAudioItems().ifPresent(audioItems -> audioRepository.removeAll(new HashSet<>(audioItems)));
        }

        private void selectAllTracks(ActionEvent event) {
            var mode = navigationController.navigationModeProperty().get();
            switch (mode) {
                case ALL_AUDIO_ITEMS, PLAYLIST:
                    mainAudioItemTable.getSelectionModel().selectAll();
                    break;
                case ARTISTS:
                    artistViewController.selectAllTracks();
                    break;
            }
        }

        private void unselectTracks(ActionEvent event) {
            var mode = navigationController.navigationModeProperty().get();
            switch (mode) {
                case ALL_AUDIO_ITEMS, PLAYLIST:
                    mainAudioItemTable.getSelectionModel().clearSelection();
                    break;
                case ARTISTS:
                    artistViewController.deselectAllTracks();
                    break;
            }
        }
    }

    @EventListener
    public void showTableViewContextMenuEventListener(ShowTableViewContextMenuEvent event) {
        audioItemContextMenu.show(event.selectedAudioItems, (Node) event.getSource(), event.screenX, event.screenY);
    }

    private class AudioItemTableViewContextMenu extends ContextMenu {

        private final Menu addToPlaylistMenu;
        private final MenuItem deleteFromPlaylistMenuItem;
        private final List<MenuItem> playlistsInMenu = new ArrayList<>();

        private ObservableList<ObservableAudioItem> selection;

        public AudioItemTableViewContextMenu() {
            super();
            addToPlaylistMenu = new Menu("Add to playlist");

            MenuItem playMenuItem = new MenuItem("Play");
            playMenuItem.setOnAction(event -> {
                if (! selection.isEmpty()) {
                    applicationContext.publishEvent(new PlayItemEvent(selection, this));
                }
            });

            MenuItem editMenuItem = new MenuItem("Edit");
            editMenuItem.setOnAction(event -> {
                if (! selection.isEmpty()) {
                    applicationContext.publishEvent(new OpenAudioItemEditorView(new HashSet<>(selection), this));
                }
            });

            MenuItem deleteMenuItem = new MenuItem("Delete");
            deleteMenuItem.setOnAction(event -> {
                if (! selection.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete selected track(s)?");
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            var selectionSet = new HashSet<>(selection);
                            // TODO spread removal throw places if necessary with an event
                            applicationContext.publishEvent(new DeleteAudioItemsEvent(selectionSet, this));
                            audioRepository.removeAll(selectionSet);
                        }
                    });
                }
            });

            MenuItem addToQueueMenuItem = new MenuItem("Add to play queue");
            addToQueueMenuItem.setOnAction(event -> {
                if (! selection.isEmpty())
                    applicationContext.publishEvent(new AddToPlayQueueEvent(selection, this));
            });

            deleteFromPlaylistMenuItem = new MenuItem("Delete from playlist");
            deleteFromPlaylistMenuItem.setId("deleteFromPlaylistMenuItem");
            deleteFromPlaylistMenuItem.setOnAction(event ->
                    selectedPlaylistProperty.get().ifPresent(audioPlaylist -> {
                        audioPlaylist.getAudioItemsProperty().removeAll(selection);
                    }));

            getItems().addAll(playMenuItem, editMenuItem, deleteMenuItem, addToQueueMenuItem, new SeparatorMenuItem());
            getItems().addAll(deleteFromPlaylistMenuItem, addToPlaylistMenu);
        }

        public void show(ObservableList<ObservableAudioItem> selection, Node anchor, double screenX, double screenY) {
            this.selection = selection;
            playlistsInMenu.clear();
            addToPlaylistMenu.getItems().clear();

            // Populate "Add to Playlist" submenu unconditionally in all navigation views (D-07)
            navigationController.playlistsProperty().stream()
                    .filter(p -> ! p.isDirectory())
                    .forEach(this::addPlaylistToMenuList);
            addToPlaylistMenu.getItems().addAll(playlistsInMenu);

            // "Delete from playlist" only makes sense when viewing a specific non-folder playlist
            var selectedPlaylist = selectedPlaylistProperty.get();
            deleteFromPlaylistMenuItem.setVisible(
                selectedPlaylist.isPresent() && ! selectedPlaylist.get().isDirectory());

            super.show(anchor, screenX, screenY);
        }

        private void addPlaylistToMenuList(ObservablePlaylist playlist) {
            Optional<ObservablePlaylist> selectedPlaylist = selectedPlaylistProperty.get();
            // In playlist view, exclude the current playlist from the submenu (can't add tracks to itself)
            if (selectedPlaylist.isEmpty() || ! selectedPlaylist.get().equals(playlist)) {
                MenuItem playlistMenuItem = new MenuItem(playlist.getName());
                playlistMenuItem.setOnAction(event -> playlist.getAudioItemsProperty().addAll(mainAudioItemTable.getSelectionModel().getSelectedItems()));
                playlistsInMenu.add(playlistMenuItem);
            }
        }
    }
}
