package net.transgressoft.musicott.view.itunes;

import net.transgressoft.commons.music.itunes.ItunesImportPolicy;
import net.transgressoft.commons.music.itunes.ItunesLibraryParser;
import net.transgressoft.musicott.service.MediaImportService;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.testfx.api.FxRobot;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * End-to-end integration test for {@link ItunesImportWizard}, exercising the full
 * four-step wizard flow (library → playlists → policy → confirm), the per-page
 * validity rebinder, the FINISH-button rename to "Import", and the dispatch of
 * {@link MediaImportService#importSelectedPlaylists} via Mockito argument capture.
 *
 * @author Octavio Calleya
 */
@JavaFxSpringTest (classes = ItunesImportWizardIT.ItunesImportWizardITConfiguration.class)
@DisplayName ("ItunesImportWizard")
class ItunesImportWizardIT extends ApplicationTestBase<Parent> {

    @Autowired
    ItunesImportWizard wizard;
    @Autowired
    MediaImportService mediaImportService;
    @Autowired
    ItunesWizardLibraryStepController libraryStepController;
    @Autowired
    ItunesWizardPolicyStepController policyStepController;

    Path fixtureLibrary;

    @Override
    protected Parent javaFxComponent() {
        return new BorderPane();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        try {
            fixtureLibrary = Path.of(getClass().getResource("/itunes-library.xml").toURI());
        }
        catch (Exception ex) {
            throw new IllegalStateException("Could not locate /itunes-library.xml fixture", ex);
        }
        when(mediaImportService.parseLibrary(any()))
                .thenAnswer(invocation -> ItunesLibraryParser.INSTANCE.parse(invocation.getArgument(0)));
        when(mediaImportService.importSelectedPlaylists(anyList(), any(ItunesImportPolicy.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    @DisplayName ("ItunesImportWizard drives all four steps and dispatches importSelectedPlaylists with collected policy")
    void drivesAllFourStepsAndDispatchesImportSelectedPlaylistsWithCollectedPolicy(FxRobot fxRobot) {
        Platform.runLater(() -> wizard.show(testStage));
        waitForFxEvents();

        // The wizard runs as a modal owned by testStage; assert at-least-2 instead of exact 2 so
        // an unrelated framework window (e.g. headless Monocle bookkeeping) doesn't flake the test.
        long showingWindows = Window.getWindows().stream().filter(Window::isShowing).count();
        assertThat(showingWindows).isGreaterThanOrEqualTo(2);

        Platform.runLater(() -> libraryStepController.acceptLibraryFile(fixtureLibrary));
        waitForFxEvents();
        clickButton(fxRobot, ButtonBar.ButtonData.NEXT_FORWARD);

        Platform.runLater(() -> fxRobot.lookup("#selectAllButton").queryButton().fire());
        waitForFxEvents();
        clickButton(fxRobot, ButtonBar.ButtonData.NEXT_FORWARD);

        Platform.runLater(() -> {
            policyStepController.useItunesDbRadio.setSelected(true);
            policyStepController.writeMetadataSwitch.setSelected(false);
        });
        waitForFxEvents();
        clickButton(fxRobot, ButtonBar.ButtonData.NEXT_FORWARD);

        clickButton(fxRobot, ButtonBar.ButtonData.FINISH);
        waitForFxEvents();

        ArgumentCaptor<ItunesImportPolicy> policyCaptor = ArgumentCaptor.forClass(ItunesImportPolicy.class);
        verify(mediaImportService).importSelectedPlaylists(anyList(), policyCaptor.capture());
        ItunesImportPolicy captured = policyCaptor.getValue();
        assertThat(captured.getUseFileMetadata()).isFalse();
        assertThat(captured.getHoldPlayCount()).isTrue();
        assertThat(captured.getWriteMetadata()).isFalse();
    }

    @Test
    @DisplayName ("ItunesImportWizard renames the FINISH button text to Import after the first page is entered")
    void renamesFinishButtonTextToImportAfterFirstPageIsEntered(FxRobot fxRobot) {
        Platform.runLater(() -> wizard.show(testStage));
        waitForFxEvents();
        // FINISH is only added to the visible button bar once the wizard reaches the last
        // step (controlsfx removes it on non-terminal pages). Drive through to step 4 first;
        // the rename itself ran on step 1 entry and persists across page transitions.
        Platform.runLater(() -> libraryStepController.acceptLibraryFile(fixtureLibrary));
        waitForFxEvents();
        clickButton(fxRobot, ButtonBar.ButtonData.NEXT_FORWARD);
        Platform.runLater(() -> fxRobot.lookup("#selectAllButton").queryButton().fire());
        waitForFxEvents();
        clickButton(fxRobot, ButtonBar.ButtonData.NEXT_FORWARD);
        clickButton(fxRobot, ButtonBar.ButtonData.NEXT_FORWARD);

        Button finishButton = lookupButtonByData(fxRobot, ButtonBar.ButtonData.FINISH);
        assertThat(finishButton.getText()).isEqualTo("Import");

        clickButton(fxRobot, ButtonBar.ButtonData.CANCEL_CLOSE);
        waitForFxEvents();
    }

    @Test
    @DisplayName ("ItunesImportWizard cancel discards collected choices and does not call importSelectedPlaylists")
    void cancelDiscardsCollectedChoicesAndDoesNotCallImportSelectedPlaylists(FxRobot fxRobot) {
        Platform.runLater(() -> wizard.show(testStage));
        waitForFxEvents();
        Platform.runLater(() -> libraryStepController.acceptLibraryFile(fixtureLibrary));
        waitForFxEvents();
        clickButton(fxRobot, ButtonBar.ButtonData.NEXT_FORWARD);
        clickButton(fxRobot, ButtonBar.ButtonData.CANCEL_CLOSE);
        waitForFxEvents();

        verify(mediaImportService, never())
                .importSelectedPlaylists(anyList(), any(ItunesImportPolicy.class));
    }

    @Test
    @DisplayName ("ItunesImportWizard preserves later-step selections across Back navigation")
    void preservesLaterStepSelectionsAcrossBackNavigation(FxRobot fxRobot) {
        Platform.runLater(() -> wizard.show(testStage));
        waitForFxEvents();
        Platform.runLater(() -> libraryStepController.acceptLibraryFile(fixtureLibrary));
        waitForFxEvents();
        clickButton(fxRobot, ButtonBar.ButtonData.NEXT_FORWARD);
        Platform.runLater(() -> fxRobot.lookup("#selectAllButton").queryButton().fire());
        waitForFxEvents();
        clickButton(fxRobot, ButtonBar.ButtonData.NEXT_FORWARD);

        Platform.runLater(() -> policyStepController.useItunesDbRadio.setSelected(true));
        waitForFxEvents();

        clickButton(fxRobot, ButtonBar.ButtonData.BACK_PREVIOUS);
        clickButton(fxRobot, ButtonBar.ButtonData.NEXT_FORWARD);
        waitForFxEvents();

        assertThat(policyStepController.useItunesDbRadio.isSelected()).isTrue();

        clickButton(fxRobot, ButtonBar.ButtonData.CANCEL_CLOSE);
        waitForFxEvents();
    }

    private static void clickButton(FxRobot fxRobot, ButtonBar.ButtonData data) {
        Button btn = lookupButtonByData(fxRobot, data);
        Platform.runLater(btn::fire);
        waitForFxEvents();
    }

    private static Button lookupButtonByData(FxRobot fxRobot, ButtonBar.ButtonData data) {
        return fxRobot.lookup((Node node) -> node instanceof Button b
                && ButtonBar.getButtonData(b) == data).queryButton();
    }

    @JavaFxSpringTestConfiguration (includeFilters = {
            @Filter (type = FilterType.ASSIGNABLE_TYPE, classes = {
                    ItunesImportWizard.class,
                    ItunesWizardLibraryStepController.class,
                    ItunesWizardPlaylistsStepController.class,
                    ItunesWizardPolicyStepController.class,
                    ItunesWizardConfirmStepController.class
            })
    })
    static class ItunesImportWizardITConfiguration {

        @Bean
        public MediaImportService mediaImportService() {
            return mock(MediaImportService.class);
        }

        @Bean
        public ApplicationEventPublisher applicationEventPublisher() {
            return mock(ApplicationEventPublisher.class);
        }

        // destroyMethod = "" prevents Spring from auto-inferring the shutdown() method as the destroy callback,
        // which would call Platform.exit() and kill the JavaFX Application Thread between test classes
        @Bean (destroyMethod = "")
        public FxWeaver fxWeaver(ConfigurableApplicationContext applicationContext) {
            return new SpringFxWeaver(applicationContext);
        }

        @Bean
        @Scope (ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public <C, V extends Node> FxControllerAndView<C, V> controllerAndView(
                FxWeaver fxWeaver, InjectionPoint injectionPoint) {
            return new InjectionPointLazyFxControllerAndViewResolver(fxWeaver).resolve(injectionPoint);
        }
    }
}
