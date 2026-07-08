package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.*;
import net.transgressoft.commons.fx.music.playlist.*;
import net.transgressoft.commons.music.m3u.M3uImportService;
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
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
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

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test verifying that the File menu exposes the playlist operations that the
 * playlist context menu offers — delete, export and import — and that the delete entry is
 * only enabled while a playlist is selected.
 */
@JavaFxSpringTest(classes = MainControllerPlaylistFileMenuITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("MainControllerPlaylistFileMenu")
class MainControllerPlaylistFileMenuIT extends ApplicationTestBase<BorderPane> {

    @Autowired
    FxControllerAndView<MainController, BorderPane> mainControllerAndView;

    @Autowired
    ObservablePlaylistHierarchy playlistRepository;

    @Autowired
    ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty;

    ObservablePlaylist playlist;

    @Override
    protected BorderPane javaFxComponent() {
        return mainControllerAndView.getView().get();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        playlist = playlistRepository.findByName("Playlist One").orElseThrow();
        Platform.runLater(() -> selectedPlaylistProperty.set(Optional.empty()));
        waitForFxEvents();
        WaitForAsyncUtils.clearExceptions();
    }

    private MenuItem fileMenuItem(String text) {
        Object menuBarController = ReflectionTestUtils.getField(mainControllerAndView.getController(), "menuBarController");
        Menu fileMenu = (Menu) ReflectionTestUtils.getField(menuBarController, "fileMenu");
        return fileMenu.getItems().stream()
                .filter(item -> text.equals(item.getText()))
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("exposes delete, export and import playlist entries in the File menu")
    void exposesPlaylistEntriesInFileMenu(FxRobot fxRobot) {
        waitForFxEvents();

        assertThat(fileMenuItem("Delete playlist").getOnAction()).isNotNull();
        assertThat(fileMenuItem("Export playlist(s) as m3u file(s)").getOnAction()).isNotNull();
        assertThat(fileMenuItem("Import playlist from m3u file...").getOnAction()).isNotNull();
    }

    @Test
    @DisplayName("disables the delete entry when no playlist is selected")
    void disablesDeleteEntryWhenNoPlaylistSelected(FxRobot fxRobot) {
        waitForFxEvents();

        assertThat(selectedPlaylistProperty.get()).isEmpty();
        assertThat(fileMenuItem("Delete playlist").isDisable()).isTrue();
    }

    @Test
    @DisplayName("enables the delete entry while a playlist is selected")
    void enablesDeleteEntryWhilePlaylistSelected(FxRobot fxRobot) {
        Platform.runLater(() -> selectedPlaylistProperty.set(Optional.of(playlist)));
        waitForFxEvents();

        assertThat(fileMenuItem("Delete playlist").isDisable()).isFalse();

        Platform.runLater(() -> selectedPlaylistProperty.set(Optional.empty()));
        waitForFxEvents();

        assertThat(fileMenuItem("Delete playlist").isDisable()).isTrue();
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                MainController.class,
                NavigationController.class,
                PlayerController.class,
                PlayQueueController.class,
                ArtistViewController.class,
                AlbumViewController.class,
                GenreViewController.class,
                PlaylistTreeView.class,
                FullAudioItemTableView.class,
                SimpleAudioItemTableView.class,
                ArtistAlbumListRow.class
        })
})
class MainControllerPlaylistFileMenuITConfiguration {

    @Bean
    public FXMusicLibrary musicLibrary() {
        return FXMusicLibrary.builder().build();
    }

    @Bean
    public ObservableAudioLibrary audioLibrary(FXMusicLibrary musicLibrary) {
        return musicLibrary.audioLibrary();
    }

    @Bean
    public ObservablePlaylistHierarchy playlistRepository(FXMusicLibrary musicLibrary) {
        var repo = musicLibrary.playlistHierarchy();
        var playlistOne = repo.createPlaylist("Playlist One");
        var playlistTwo = repo.createPlaylist("Playlist Two");
        repo.createPlaylistDirectory("ROOT_PLAYLIST");
        repo.addPlaylistsToDirectory(java.util.Set.of(playlistOne, playlistTwo), "ROOT_PLAYLIST");
        return repo;
    }

    @Bean
    public ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty() {
        return new SimpleObjectProperty<>(this, "selected mc file menu it playlist", Optional.empty());
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
        org.mockito.Mockito.when(service.getTotalDuration()).thenReturn(java.time.Duration.ZERO);
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
    public Supplier<DirectoryChooser> directoryChooserSupplier() {
        return () -> mock(DirectoryChooser.class);
    }

    @Bean
    public Supplier<FileChooser> fileChooserSupplier() {
        return () -> mock(FileChooser.class);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public M3uImportService<ObservableAudioItem, ObservablePlaylist> m3uImportService() {
        return mock(M3uImportService.class);
    }

    @Bean
    public KeyCombination.Modifier operativeSystemKeyModifier() {
        return KeyCombination.CONTROL_DOWN;
    }

    @Bean
    public net.transgressoft.musicott.search.SearchCoordinator searchCoordinator() {
        return mock(net.transgressoft.musicott.search.SearchCoordinator.class);
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
