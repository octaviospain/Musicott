package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.persistence.json.JsonFileRepository;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.PlaylistTreeView;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static net.transgressoft.commons.fx.music.playlist.ObservablePlaylistSerializerKt.ObservablePlaylistMapSerializer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.context.annotation.ComponentScan.Filter;

/**
 * Integration test for {@link NavigationController}, validating the sidebar navigation rendering
 * and the visibility of the playlist tree using a Spring-integrated test context.
 */
@JavaFxSpringTest(classes = NavigationControllerITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NavigationControllerIT extends ApplicationTestBase<VBox> {

    @Autowired
    FxControllerAndView<NavigationController, VBox> navigationControllerAndView;

    @Override
    protected VBox javaFxComponent() {
        return navigationControllerAndView.getView().get();
    }

    @Test
    @DisplayName("NavigationController renders navigation sidebar with visible playlist tree")
    void rendersNavigationSidebarWithVisiblePlaylistTree(FxRobot fxRobot) {
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(fxRobot.lookup("#navigationPaneVBox").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#navigationVBox").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#playlistsVBox").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#taskProgressBar").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#newPlaylistButton").tryQuery()).isPresent();

        assertThat(fxRobot.lookup("#playlistTreeView").tryQuery()).isPresent();
        TreeView<?> treeView = fxRobot.lookup("#playlistTreeView").query();
        assertThat(treeView.isVisible()).isTrue();
    }

    @Test
    @DisplayName("NavigationController displays navigation mode list with All Tracks and Artists entries")
    void displaysNavigationModeListWithEntries(FxRobot fxRobot) {
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(fxRobot.lookup("#navigationModeListView").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("All tracks").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("Artists").tryQuery()).isPresent();
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                NavigationController.class,
                PlaylistTreeView.class
        })
})
class NavigationControllerITConfiguration {

    File playlistsFile;

    public NavigationControllerITConfiguration() throws IOException {
        playlistsFile = Files.createTempFile("playlists-nav-test", ".json").toFile();
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
        return new SimpleObjectProperty<>(this, "selected nav test playlist", Optional.empty());
    }

    @Bean
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    @Bean
    public KeyCombination.Modifier operativeSystemKeyModifier() {
        return KeyCombination.CONTROL_DOWN;
    }

    // destroyMethod = "" prevents Spring from auto-inferring the shutdown() method as the destroy callback,
    // which would call Platform.exit() and kill the JavaFX Application Thread between test classes
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
