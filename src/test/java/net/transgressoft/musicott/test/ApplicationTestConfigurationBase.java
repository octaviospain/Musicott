package net.transgressoft.musicott.test;

import javafx.scene.Parent;
import net.transgressoft.musicott.test.JavaFxViewTestLauncher.FxViewInitializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

public abstract class ApplicationTestConfigurationBase {

    @Bean
    @ConditionalOnMissingBean
    public <T extends Parent> FxViewInitializer<T> javaFxComponentInitializer(T component) {
        return new FxViewInitializer<>(component);
    }
}
