package net.transgressoft.musicott.test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@Profile("test")
@ComponentScan(useDefaultFilters = false)
@EnableAutoConfiguration(exclude = {
        WebMvcAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        JmxAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        EmbeddedWebServerFactoryCustomizerAutoConfiguration.class,
        ErrorMvcAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        ValidationAutoConfiguration.class,
        TaskExecutionAutoConfiguration.class,
        TaskSchedulingAutoConfiguration.class,
        RabbitAutoConfiguration.class,
        MongoAutoConfiguration.class
})
public @interface JavaFxSpringTestConfiguration {
    /**
     * Base packages to scan for components.
     * @return base packages
     */
    @AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
    String[] basePackages() default {};

    /**
     * Base package classes to scan for components.
     * @return base package classes
     */
    @AliasFor(annotation = ComponentScan.class, attribute = "basePackageClasses")
    Class<?>[] basePackageClasses() default {};

    /**
     * Classes to include in component scanning.
     * @return classes to include
     */
    @AliasFor(annotation = ComponentScan.class, attribute = "includeFilters")
    ComponentScan.Filter[] includeFilters() default {
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {})
    };

    /**
     * Whether to use default filters for component scanning.
     * @return true to use default filters
     */
    @AliasFor(annotation = ComponentScan.class, attribute = "useDefaultFilters")
    boolean useDefaultFilters() default false;
}
