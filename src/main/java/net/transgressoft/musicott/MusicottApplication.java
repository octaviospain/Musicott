package net.transgressoft.musicott;

import net.transgressoft.musicott.config.*;
import net.transgressoft.musicott.events.StopApplicationEvent;
import net.transgressoft.musicott.splash.SplashOrchestrator;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

@Configuration
@Import(ApplicationConfiguration.class)
@ComponentScan
public class MusicottApplication {

    public static void main(String[] args) {
        Application.launch(SpringbootJavaFxApplication.class, args);
    }

    public static class SpringbootJavaFxApplication extends Application {

        @Override
        public void init() {
            // Empty by design. Spring boot moved to a background Task spawned from
            // start() so the splash window can render before any blocking I/O runs.
            // SplashOrchestrator owns the Task lifecycle and the post-boot scene
            // initialization handoff to PrimaryStageInitializer.
        }

        @Override
        public void start(Stage primaryStage) {
            if (Boolean.getBoolean("musicott.splash.disabled")) {
                // Synchronous bypass path used by all four test source sets. Mirrors
                // the previous init() + start() behavior without splash so tests do
                // not pay the 800ms minimum display gate.
                ConfigurableApplicationContext context = new SpringApplicationBuilder()
                        .sources(MusicottApplication.class)
                        .run();
                PrimaryStageInitializer initializer = context.getBean(PrimaryStageInitializer.class);
                initializer.initializePrimaryStage(primaryStage);
                primaryStage.show();
                return;
            }
            // Splash path. Orchestrator spawns the boot Task on a background thread,
            // dismisses the splash via fade once the main scene is laid out, and
            // bifurcates failure handling between pre-Spring (hard exit) and
            // post-Spring (ExceptionEvent) routes.
            new SplashOrchestrator(MusicottApplication.class).orchestrate(primaryStage);
        }

        @Override
        public void stop() {
            stopApplication(new StopApplicationEvent(this));
        }

        @Bean
        public FxWeaver fxWeaver(ConfigurableApplicationContext applicationContext) {
            return new SpringFxWeaver(applicationContext);
        }

        @Bean
        public static PropertySourcesPlaceholderConfigurer properties() {
            PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
            YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(new ClassPathResource("application.yml"));
            java.util.Properties yamlProperties = java.util.Objects.requireNonNull(
                    yaml.getObject(), "application.yml could not be parsed");
            propertySourcesPlaceholderConfigurer.setProperties(yamlProperties);
            return propertySourcesPlaceholderConfigurer;
        }

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public <C, V extends Node> FxControllerAndView<C, V> controllerAndView(FxWeaver fxWeaver, InjectionPoint injectionPoint) {
            return new InjectionPointLazyFxControllerAndViewResolver(fxWeaver).resolve(injectionPoint);
        }

        @Bean
        public HostServices hostServices() {
            return getHostServices();
        }

        @Bean
        public Supplier<Stage> stageSupplier() {
            return Stage::new;
        }

        @Bean
        public ApplicationPaths applicationPaths() {
            Path defaultApplicationDirectory = Paths.get((System.getProperty("user.home")), ".config", "musicott");
            return new ApplicationPaths(
                    defaultApplicationDirectory.resolve("audioItems.json"),
                    defaultApplicationDirectory.resolve("playlists.json"),
                    defaultApplicationDirectory.resolve("waveforms.json")
            );
        }

        @EventListener
        public void stopApplication(StopApplicationEvent event) {
            Platform.exit();
            System.exit(0);
        }
    }
}
