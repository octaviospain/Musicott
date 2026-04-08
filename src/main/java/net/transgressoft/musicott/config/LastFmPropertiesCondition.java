package net.transgressoft.musicott.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Activates Last.fm integration beans only when both {@code lastfm.api-key}
 * and {@code lastfm.api-secret} properties are present in the environment.
 */
public class LastFmPropertiesCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var env = context.getEnvironment();
        return env.containsProperty("lastfm.api-key") && env.containsProperty("lastfm.api-secret");
    }
}
