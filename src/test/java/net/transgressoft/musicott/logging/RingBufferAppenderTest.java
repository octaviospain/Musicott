package net.transgressoft.musicott.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RingBufferAppender}, pinning the exact-pattern encoding contract
 * before the production class is created.
 */
@DisplayName("RingBufferAppender")
class RingBufferAppenderTest {

    LoggerContext loggerContext;
    PatternLayoutEncoder encoder;
    RingBufferAppender appender;

    @BeforeEach
    void setUp() {
        RingBufferHolder.INSTANCE.resetForTest();

        loggerContext = new LoggerContext();

        encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%-5level %logger{36} - %msg%n");
        encoder.start();

        appender = new RingBufferAppender();
        appender.setContext(loggerContext);
        appender.setEncoder(encoder);
        appender.start();
    }

    @Test
    @DisplayName("appends INFO event encoded with exact console pattern and no timestamp or thread")
    void appendsInfoEventEncodedWithExactConsolePatternAndNoTimestampOrThread() {
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger("net.transgressoft.test");
        LoggingEvent event = new LoggingEvent(
            "net.transgressoft.test",
            logger,
            Level.INFO,
            "hello world",
            null,
            null
        );

        appender.doAppend(event);

        List<String> snapshot = RingBufferHolder.INSTANCE.snapshot();
        assertThat(snapshot).hasSize(1);

        String line = snapshot.get(0);
        // Pattern: %-5level %logger{36} - %msg%n
        // Level left-padded to 5: "INFO " (INFO is 4 chars so it gets one trailing space)
        assertThat(line).startsWith("INFO ");
        assertThat(line).contains("net.transgressoft.test");
        assertThat(line).contains(" - ");
        assertThat(line).contains("hello world");
        assertThat(line).endsWith("\n");
        // No timestamp or thread token
        assertThat(line).doesNotContainPattern("\\d{2}:\\d{2}:\\d{2}");
        assertThat(line).doesNotContainPattern("\\[.*\\]");
    }

    @Test
    @DisplayName("appending a WARN event increments RingBufferHolder warnErrorCount")
    void appendingWarnEventIncrementsWarnErrorCount() {
        long countBefore = RingBufferHolder.INSTANCE.warnErrorCount();

        ch.qos.logback.classic.Logger logger = loggerContext.getLogger("net.transgressoft.test");
        LoggingEvent event = new LoggingEvent(
            "net.transgressoft.test",
            logger,
            Level.WARN,
            "a warning message",
            null,
            null
        );

        appender.doAppend(event);

        assertThat(RingBufferHolder.INSTANCE.warnErrorCount())
            .as("WARN event routed through the appender must increment warnErrorCount")
            .isEqualTo(countBefore + 1);
    }
}
