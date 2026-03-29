package net.transgressoft.musicott.test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testfx.framework.junit5.ApplicationExtension;

import java.lang.annotation.*;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@ExtendWith(ApplicationExtension.class)
public @interface JavaFxSpringTest {
    Class<?>[] classes() default {};
    String[] properties() default {};
}
