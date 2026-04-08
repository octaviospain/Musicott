package net.transgressoft.musicott.test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
@EnableAutoConfiguration
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
