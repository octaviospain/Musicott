package net.transgressoft.musicott.view.custom.table;

import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
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
import org.testfx.api.FxRobot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * UI test verifying the layout behavior of {@link FullAudioItemTableView} under window resize:
 * Title-first column ordering, slack distribution across the absorber columns when the table
 * widens, and engagement of the TableView's native horizontal scrollbar when the scene narrows
 * below the sum of column pref widths.
 *
 * <p>The unplayable / not-on-disk dimmed-text rendering is verified by visual UAT — automated
 * inspection of computed text fills under headless Monocle was attempted but proved unreliable
 * (the CSS lookup index does not consistently populate pseudo-class node sets in headless mode,
 * and cell-factory NPEs from sparse mocks abort the assumption).
 */
@JavaFxSpringTest(classes = FullAudioItemTableViewLayoutUITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("FullAudioItemTableViewLayout")
class FullAudioItemTableViewLayoutUIT extends ApplicationTestBase<StackPane> {

    @Autowired
    FullAudioItemTableView fullAudioItemTableView;

    @Override
    protected StackPane javaFxComponent() {
        // Wrap in a fresh StackPane each call so JavaFX accepts the autowired (singleton)
        // TableView as a child of a new scene on every beforeEach() — the bare TableView would
        // throw "already set as root of another scene" on the second test method.
        return new StackPane(fullAudioItemTableView);
    }

    @Test
    @DisplayName("places Title column at index 0")
    void placesTitleColumnAtIndex0() {
        assertThat(fullAudioItemTableView.getColumns().get(0).getText()).isEqualTo("Title");
    }

    @Test
    @DisplayName("distributes slack across Artist Album and Genre when scene widens")
    void distributesSlackAcrossAbsorbersWhenSceneWidens(FxRobot robot) {
        // The redistribute listener installed in FullAudioItemTableView's constructor grows
        // Artist + Album + Genre proportionally when the table widens beyond the sum of
        // non-absorber column widths. Drive that pathway by widening the stage.
        TableColumn<?, ?> artistCol = column("Artist");
        TableColumn<?, ?> albumCol = column("Album");
        TableColumn<?, ?> genreCol = column("Genre");
        double artistBaseline = artistCol.getPrefWidth();
        double albumBaseline = albumCol.getPrefWidth();
        double genreBaseline = genreCol.getPrefWidth();

        Stage stage = (Stage) robot.window(0);
        double originalStageWidth = stage.getWidth();
        // Use a width comfortably above the threshold but small enough to avoid the headless
        // Monocle buffer overflow seen at very large stage sizes.
        Platform.runLater(() -> stage.setWidth(2200.0));
        waitForFxEvents();

        try {
            assertThat(artistCol.getPrefWidth() + albumCol.getPrefWidth() + genreCol.getPrefWidth())
                    .as("Sum of absorber prefWidths should grow above the baseline (%.0fpx) when the stage widens",
                            artistBaseline + albumBaseline + genreBaseline)
                    .isGreaterThan(artistBaseline + albumBaseline + genreBaseline);
        } finally {
            // Restore the original stage size so subsequent tests don't render against an
            // oversized Monocle framebuffer (which throws BufferOverflowException).
            Platform.runLater(() -> stage.setWidth(originalStageWidth));
            waitForFxEvents();
        }
    }

    @Test
    @DisplayName("engages native horizontal scrollbar when scene narrows below sum of column pref widths")
    void engagesNativeHorizontalScrollbarWhenSceneNarrows(FxRobot robot) {
        // With UNCONSTRAINED_RESIZE_POLICY the TableView's own horizontal scrollbar engages
        // when the scene width drops below the sum of column pref widths. Assert by reading
        // the rendered TableView width relative to the column-width sum — when the table is
        // narrower than its content the native hbar is up.
        Stage stage = (Stage) robot.window(0);
        Platform.runLater(() -> stage.setWidth(800.0));
        waitForFxEvents();

        double tableWidth = fullAudioItemTableView.getWidth();
        double columnsTotalWidth = fullAudioItemTableView.getColumns().stream()
                .mapToDouble(TableColumn::getWidth).sum();
        assertThat(columnsTotalWidth)
                .as("Sum of column widths (%.0fpx) must exceed table width (%.0fpx) so the native hbar engages",
                        columnsTotalWidth, tableWidth)
                .isGreaterThan(tableWidth);
    }

    private TableColumn<?, ?> column(String header) {
        return fullAudioItemTableView.getColumns().stream()
                .filter(c -> header.equals(c.getText()))
                .findFirst()
                .orElseThrow();
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {FullAudioItemTableView.class})
})
class FullAudioItemTableViewLayoutUITConfiguration {

    @Bean
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    // destroyMethod = "" prevents Spring from auto-inferring shutdown() as the destroy callback,
    // which would call Platform.exit() and kill the JavaFX Application Thread between test classes.
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
