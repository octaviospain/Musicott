package net.transgressoft.musicott.test;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.stage.Stage;
import net.transgressoft.musicott.events.StageReadyEvent;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Utility class for launching JavaFX Spring test applications.
 * This class simplifies the process of testing JavaFX components with Spring integration.
 */
public class JavaFxViewTestLauncher {

    private static Class<?> currentConfigClass;

    static {
        System.setProperty("testfx.robot", "awt");
        System.setProperty("java.awt.headless", "false");
        System.setProperty("testfx.headless", "false");
        System.setProperty("prism.order", "sw");
        System.setProperty("glass.platform", "Gtk");
    }

    public static <T extends Parent> void launch(Class<?> configClass, String... args) {
        currentConfigClass = configClass;

        System.out.println("==============================================");
        System.out.println("LAUNCHING JAVAFX COMPONENT TEST");
        System.out.println("If you see module access errors, you need to add VM arguments:");
        System.out.println("--add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED");
        System.out.println("--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED");
        System.out.println("==============================================");

        Application.launch(JavaFxSpringTestApplication.class, args);
    }

    /**
     * Generic JavaFX Spring test application.
     */
    public static class JavaFxSpringTestApplication extends Application {

        private ConfigurableApplicationContext context;

        @Override
        public void init() {
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
            try {
                primaryStage.setTitle("JavaFX Component Test - " + currentConfigClass.getSimpleName());
                primaryStage.setWidth(800);
                primaryStage.setHeight(600);
                primaryStage.centerOnScreen();

                context.publishEvent(new StageReadyEvent(primaryStage));

                // Add a confirmation message for visibility
                System.out.println("✅ Stage created and about to be shown");

                // Force the stage to be shown and stay on top
                primaryStage.setAlwaysOnTop(true);
                primaryStage.toFront();

                System.out.println("✅ JavaFX window should now be visible");
            } catch (Exception e) {
                System.err.println("❌ ERROR showing JavaFX window: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public void stop() throws Exception {
            if (context != null) {
                context.close();
            }
            super.stop();
        }
    }
}