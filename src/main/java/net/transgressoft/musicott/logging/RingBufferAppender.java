package net.transgressoft.musicott.logging;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.nio.charset.StandardCharsets;

/**
 * Logback appender that encodes each log event using a configured {@link PatternLayoutEncoder}
 * and stores the resulting string in the shared {@link RingBufferHolder}. This class is
 * instantiated exclusively by the logback framework via {@code logback.xml} — it must not
 * carry any Spring stereotype annotation, as doing so would create a separate Spring-managed
 * instance that logback never uses.
 *
 * <p>Thread safety: {@link AppenderBase#doAppend} holds the appender's intrinsic lock before
 * delegating to {@link #append}, preventing concurrent logback writes through the same appender.
 * {@link RingBufferHolder} uses its own {@link java.util.concurrent.locks.ReentrantLock} to
 * protect the buffer from concurrent reads by the JavaFX Application Thread.
 *
 * @author Octavio Calleya
 */
public class RingBufferAppender extends AppenderBase<ILoggingEvent> {

    PatternLayoutEncoder encoder;

    /**
     * Sets the encoder used to convert log events to strings. Logback resolves the nested
     * {@code <encoder>} XML element by calling this setter during configuration.
     *
     * @param encoder the pattern layout encoder to use
     */
    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Returns the currently configured encoder.
     *
     * @return the pattern layout encoder, or {@code null} if not yet configured
     */
    public PatternLayoutEncoder getEncoder() {
        return encoder;
    }

    /**
     * Validates the encoder is present and starts the appender. Reports a logback error and
     * returns without starting if no encoder has been configured.
     */
    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder set for appender [" + name + "]");
            return;
        }
        super.start();
    }

    /**
     * Encodes the log event using the configured encoder and writes the resulting UTF-8 string
     * to {@link RingBufferHolder#INSTANCE}, passing the event level for WARN+ERROR counting.
     *
     * @param event the logging event to encode and store
     */
    @Override
    protected void append(ILoggingEvent event) {
        byte[] bytes = encoder.encode(event);
        String line = new String(bytes, StandardCharsets.UTF_8);
        RingBufferHolder.INSTANCE.add(line, event.getLevel());
    }
}
