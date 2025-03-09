package net.transgressoft.musicott.test;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.transgressoft.musicott.events.StageReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * Utility class for launching JavaFX Spring test applications.
 * This class simplifies the process of testing JavaFX components with Spring integration.
 */
public class JavaFxViewTestLauncher {

    // Static reference to configuration class that will be used by the test application
    private static Class<?> currentConfigClass;

    /**
     * Launches a JavaFX Spring test application.
     *
     * @param configClass The Spring configuration class
     * @param args Command line arguments
     * @throws Exception If an error occurs during launch
     */
    public static <T extends Parent> void launch(Class<?> configClass, String... args) throws Exception {
        // Set the configuration class to be used by the application
        currentConfigClass = configClass;

        // Launch the test application
        ApplicationTest.launch(JavaFxSpringTestApplication.class, args);
    }

    /**
     * Generic JavaFX Spring test application.
     */
    public static class JavaFxSpringTestApplication extends Application {

        private ConfigurableApplicationContext context;

        @Override
        public void init() {
            // Make sure configuration class is set
            if (currentConfigClass == null) {
                throw new IllegalStateException("Configuration class not set. Call JavaFxSpringTestLauncher.launch() first.");
            }

            // Create Spring application context with the specified configuration
            context = new SpringApplicationBuilder()
                    .sources(currentConfigClass)
                    .run();
        }

        @Override
        public void start(Stage primaryStage) {
            context.publishEvent(new StageReadyEvent(primaryStage));
        }

        @Override
        public void stop() throws Exception {
            if (context != null) {
                context.close();
            }
            super.stop();
        }
    }

    /**
     * Generic component initializer for JavaFX Spring test applications.
     *
     * @param <T> The type of JavaFX component to initialize
     */
    @Component
    public static class FxViewInitializer<T extends Parent> implements ApplicationListener<StageReadyEvent> {

        private final T component;

        @Autowired
        public FxViewInitializer(T component) {
            this.component = component;
        }

        @Override
        public void onApplicationEvent(StageReadyEvent event) {
            var scene = new Scene(component);
            event.primaryStage.setScene(scene);
            event.primaryStage.show();
        }
    }
}