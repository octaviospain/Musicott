package net.transgressoft.musicott.config;

import net.transgressoft.musicott.services.lastfm.AsynchronousLastFmService;
import net.transgressoft.musicott.services.lastfm.LastFmService;

import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.function.Supplier;

/**
 * @author Octavio Calleya
 */
@Configuration
@PropertySource (value = "classpath:lastfm.properties", ignoreResourceNotFound = true)
public class LastFmConfiguration {

    @Autowired
    Supplier<Stage> stageSupplier;

    @Bean
    @Conditional(LastFmPropertiesCondition.class)
    public LastFmService lastFmService(@Value ("${lastfm.api-key}") String apiKey, @Value ("${lastfm.api-secret}") String apiSecret) {
        return new AsynchronousLastFmService(apiKey, apiSecret, stageSupplier);
    }
}
