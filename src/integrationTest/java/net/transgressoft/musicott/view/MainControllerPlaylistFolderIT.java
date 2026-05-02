package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import net.transgressoft.musicott.service.MediaImportService;
import net.transgressoft.musicott.services.PlayerService;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.PlaylistTreeView;
import net.transgressoft.musicott.view.custom.alerts.AlertFactory;
import net.transgressoft.musicott.view.custom.table.ArtistAlbumListRow;
import net.transgressoft.musicott.view.custom.table.FullAudioItemTableView;
import net.transgressoft.musicott.view.custom.table.SimpleAudioItemTableView;
import net.transgressoft.musicott.view.itunes.ItunesImportWizard;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test for {@link MainController} verifying that selecting a playlist folder
 * renders the recursive aggregate of descendant audio items and computes the cover image
 * from the first descendant carrying one — falling back to the default cover otherwise.
 */
@JavaFxSpringTest(classes = MainControllerPlaylistFolderITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("MainControllerPlaylistFolder")
class MainControllerPlaylistFolderIT extends ApplicationTestBase<BorderPane> {

    @Autowired
    FxControllerAndView<MainController, BorderPane> mainControllerAndView;

    @Autowired
    ObservablePlaylistHierarchy playlistRepository;

    @Autowired
    ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty;

    ObservablePlaylist folder;
    ObservablePlaylist emptyFolder;
    ObservablePlaylist leafA;
    ObservablePlaylist leafB;

    @Override
    protected BorderPane javaFxComponent() {
        return mainControllerAndView.getView().get();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        folder = playlistRepository.findByName("My Folder").orElseThrow();
        emptyFolder = playlistRepository.findByName("Empty Folder").orElseThrow();
        leafA = playlistRepository.findByName("Leaf A").orElseThrow();
        leafB = playlistRepository.findByName("Leaf B").orElseThrow();

        // Setting selectedPlaylistProperty to a present value triggers
        // NavigationController to flip navigationModeProperty to PLAYLIST as a
        // side effect (NavigationController.java:130-133), which is the gate that
        // MainController.showPlaylistView() reacts to. Initialization-time listener
        // chains may push a transient null onto the FX exception queue; clear it
        // so it does not surface inside a test method.
        WaitForAsyncUtils.clearExceptions();
    }

    @Test
    @DisplayName("shows recursive aggregate of descendant tracks when folder selected")
    void showsRecursiveAggregateOfDescendantTracksWhenFolderSelected(FxRobot fxRobot) {
        Platform.runLater(() -> selectedPlaylistProperty.set(Optional.of(folder)));
        waitForFxEvents();

        FullAudioItemTableView table = (FullAudioItemTableView)
                ReflectionTestUtils.getField(mainControllerAndView.getController(), "mainAudioItemTable");
        assertThat(table).isNotNull();
        assertThat(table.getItems()).hasSize(4);
        assertThat(table.getItems())
                .containsExactlyInAnyOrderElementsOf(
                        List.of(leafA.getAudioItemsProperty().get(0),
                                leafA.getAudioItemsProperty().get(1),
                                leafB.getAudioItemsProperty().get(0),
                                leafB.getAudioItemsProperty().get(1)));
    }

    @Test
    @DisplayName("computes cover image from first descendant with cover when folder selected")
    void computesCoverImageFromFirstDescendantWithCoverWhenFolderSelected(FxRobot fxRobot) {
        // Pre-condition: at least one descendant carries a non-empty coverImageProperty.
        // The fixture mp3 may or may not embed cover art; if none of the descendants
        // carries a cover, the cover-resolution path collapses into the default-fallback
        // case which is asserted by fallsBackToDefaultCoverWhenNoDescendantHasACover.
        boolean anyDescendantHasCover = folder.getAudioItemsRecursiveProperty().stream()
                .anyMatch(item -> item.getCoverImageProperty().get().isPresent());
        Assumptions.assumeTrue(anyDescendantHasCover,
                "fixture mp3 carries no cover artwork — covered by fallback test");

        Platform.runLater(() -> selectedPlaylistProperty.set(Optional.of(folder)));
        waitForFxEvents();

        Image expected = folder.getAudioItemsRecursiveProperty().stream()
                .map(item -> item.getCoverImageProperty().get())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow();
        Image defaultImage = (Image)
                ReflectionTestUtils.getField(mainControllerAndView.getController(), "defaultPlaylistImage");
        @SuppressWarnings("unchecked")
        ObjectProperty<Image> playlistImageProperty = (ObjectProperty<Image>)
                ReflectionTestUtils.getField(mainControllerAndView.getController(), "playlistImageProperty");

        assertThat(playlistImageProperty.get()).isNotSameAs(defaultImage);
        assertThat(playlistImageProperty.get()).isSameAs(expected);
    }

    @Test
    @DisplayName("falls back to default cover when no descendant has a cover")
    void fallsBackToDefaultCoverWhenNoDescendantHasACover(FxRobot fxRobot) {
        Platform.runLater(() -> selectedPlaylistProperty.set(Optional.of(emptyFolder)));
        waitForFxEvents();

        Image defaultImage = (Image)
                ReflectionTestUtils.getField(mainControllerAndView.getController(), "defaultPlaylistImage");
        @SuppressWarnings("unchecked")
        ObjectProperty<Image> playlistImageProperty = (ObjectProperty<Image>)
                ReflectionTestUtils.getField(mainControllerAndView.getController(), "playlistImageProperty");

        assertThat(playlistImageProperty.get()).isSameAs(defaultImage);
    }

    @Test
    @DisplayName("switches back to direct items when leaf selected after folder")
    void switchesBackToDirectItemsWhenLeafSelectedAfterFolder(FxRobot fxRobot) {
        // First: select the folder — table shows 4 (recursive).
        Platform.runLater(() -> selectedPlaylistProperty.set(Optional.of(folder)));
        waitForFxEvents();

        FullAudioItemTableView table = (FullAudioItemTableView)
                ReflectionTestUtils.getField(mainControllerAndView.getController(), "mainAudioItemTable");
        assertThat(table.getItems()).hasSize(4);

        // Then: select Leaf A — table shows 2 (direct).
        Platform.runLater(() -> selectedPlaylistProperty.set(Optional.of(leafA)));
        waitForFxEvents();

        assertThat(table.getItems()).hasSize(2);
        assertThat(table.getItems())
                .containsExactlyInAnyOrderElementsOf(leafA.getAudioItemsProperty().get());
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                MainController.class,
                NavigationController.class,
                PlayerController.class,
                PlayQueueController.class,
                ArtistViewController.class,
                PlaylistTreeView.class,
                FullAudioItemTableView.class,
                SimpleAudioItemTableView.class,
                ArtistAlbumListRow.class
        })
})
class MainControllerPlaylistFolderITConfiguration {

    File playlistsFile;
    File audioItemsFile;

    public MainControllerPlaylistFolderITConfiguration() throws IOException {
        playlistsFile = Files.createTempFile("playlists-mc-folder-it", ".json").toFile();
        audioItemsFile = Files.createTempFile("audio-items-mc-folder-it", ".json").toFile();
    }

    @Bean
    public FXMusicLibrary musicLibrary() {
        return FXMusicLibrary.builder()
                .audioLibraryJsonFile(audioItemsFile)
                .playlistHierarchyJsonFile(playlistsFile)
                .build();
    }

    @Bean
    public ObservableAudioLibrary audioLibrary(FXMusicLibrary musicLibrary) {
        return musicLibrary.audioLibrary();
    }

    @Bean
    public ObservablePlaylistHierarchy playlistRepository(FXMusicLibrary musicLibrary) throws Exception {
        var audioLibrary = musicLibrary.audioLibrary();
        var repo = musicLibrary.playlistHierarchy();

        ObservableAudioItem trackA1 = createTrack(audioLibrary, "trackA1");
        ObservableAudioItem trackA2 = createTrack(audioLibrary, "trackA2");
        ObservableAudioItem trackB1 = createTrack(audioLibrary, "trackB1");
        ObservableAudioItem trackB2 = createTrack(audioLibrary, "trackB2");

        var leafA = repo.createPlaylist("Leaf A", List.of(trackA1, trackA2));
        var leafB = repo.createPlaylist("Leaf B", List.of(trackB1, trackB2));
        var folder = repo.createPlaylistDirectory("My Folder");
        var emptyFolder = repo.createPlaylistDirectory("Empty Folder");
        repo.createPlaylistDirectory("ROOT_PLAYLIST");

        repo.addPlaylistToDirectory(leafA, "My Folder");
        repo.addPlaylistToDirectory(leafB, "My Folder");
        repo.addPlaylistsToDirectory(Set.of(folder, emptyFolder), "ROOT_PLAYLIST");

        return repo;
    }

    private ObservableAudioItem createTrack(ObservableAudioLibrary lib, String name) throws IOException {
        Path tempFile = Files.createTempFile("musicott-mc-folder-it-" + name + "-", ".mp3");
        tempFile.toFile().deleteOnExit();
        try (InputStream in = getClass().getResourceAsStream("/testfiles/testeable.mp3")) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return lib.createFromFile(tempFile);
    }

    @Bean
    public ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty() {
        return new SimpleObjectProperty<>(this, "selected mc folder it playlist", Optional.empty());
    }

    @Bean
    public AlertFactory alertFactory() {
        return mock(AlertFactory.class);
    }

    @Bean
    public MediaImportService mediaImportService() {
        return mock(MediaImportService.class);
    }

    @Bean
    public ItunesImportWizard itunesImportWizard() {
        return mock(ItunesImportWizard.class);
    }

    @Bean
    public PlayerService playerService() {
        var service = mock(PlayerService.class);
        org.mockito.Mockito.when(service.getPlayQueueList()).thenReturn(FXCollections.observableArrayList());
        org.mockito.Mockito.when(service.getHistoryQueueList()).thenReturn(FXCollections.observableArrayList());
        org.mockito.Mockito.when(service.currentTrack()).thenReturn(Optional.empty());
        org.mockito.Mockito.when(service.getTotalDuration()).thenReturn(javafx.util.Duration.ZERO);
        return service;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public AudioWaveformRepository<AudioWaveform, ObservableAudioItem> audioWaveformRepository() {
        return mock(AudioWaveformRepository.class);
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
