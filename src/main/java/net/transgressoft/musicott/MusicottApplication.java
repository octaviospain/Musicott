package net.transgressoft.musicott;

import net.transgressoft.musicott.config.*;
import net.transgressoft.musicott.config.*;
import net.transgressoft.musicott.events.StageReadyEvent;
import net.transgressoft.musicott.events.StopApplicationEvent;

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

        private ConfigurableApplicationContext context;

        @Override
        public void init() {
            this.context = new SpringApplicationBuilder()
                    .sources(MusicottApplication.class)
                    .run();
        }

        @Override
        public void start(Stage primaryStage) {
            context.publishEvent(new StageReadyEvent(primaryStage));
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
            propertySourcesPlaceholderConfigurer.setProperties(yaml.getObject());
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
