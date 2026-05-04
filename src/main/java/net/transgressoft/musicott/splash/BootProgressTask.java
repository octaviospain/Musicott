package net.transgressoft.musicott.splash;

import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;

import javafx.concurrent.Task;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Background work that boots Spring and force-instantiates the heavy data beans,
 * driving the splash through four progress stages.
 *
 * <p>The first stage corresponds to {@code SpringApplicationBuilder.run()} — Spring
 * eagerly builds the {@code musicLibrary} bean here, which loads audio, playlist,
 * and waveform JSON files synchronously via {@code FXMusicLibrary.builder().build()}.
 * Stages 2-4 request the dependent beans by type; their bean factory methods are
 * trivial unwrappers (no extra I/O) but the stages remain visible to the user as
 * UX/branding markers per the locked progress-stage decision.
 *
 * <p>The {@code springContextReady} flag is flipped to {@code true} immediately
 * after {@code SpringApplicationBuilder.run()} returns. The orchestrator's
 * {@code setOnFailed} handler reads this flag to bifurcate failure handling:
 * pre-Spring failures hard-exit; post-Spring failures publish an
 * {@code ExceptionEvent} via the now-ready Spring context.
 */
public class BootProgressTask extends Task<ConfigurableApplicationContext> {

    static final String STAGE_LIBRARY = "Loading library…";
    static final String STAGE_PLAYLISTS = "Loading playlists…";
    static final String STAGE_WAVEFORMS = "Loading waveforms…";
    static final String STAGE_UI = "Preparing UI…";

    private final Class<?> applicationClass;
    private final AtomicBoolean springContextReady = new AtomicBoolean(false);
    private final AtomicReference<ConfigurableApplicationContext> contextRef = new AtomicReference<>();

    public BootProgressTask(Class<?> applicationClass) {
        this.applicationClass = applicationClass;
    }

    /**
     * Returns whether the Spring context has been successfully constructed. Read by
     * the orchestrator's failure handler to decide between hard-exit and
     * {@code ExceptionEvent} routing.
     */
    public boolean isSpringContextReady() {
        return springContextReady.get();
    }

    /**
     * Returns the Spring context if it was successfully constructed before this Task
     * failed, or {@code null} if the failure happened before
     * {@link SpringApplicationBuilder#run(String...)} returned.
     *
     * <p>Used by {@code SplashOrchestrator.handleFailure} to publish an
     * {@code ExceptionEvent} on a failed Task — {@link Task#getValue()} returns
     * {@code null} on a failed Task per the JavaFX Task contract, so we cache the
     * context in this {@link AtomicReference} the moment Spring is up. The reference
     * is set BEFORE {@code springContextReady.set(true)} so any reader who sees
     * {@code isSpringContextReady() == true} is guaranteed to also see a non-null
     * context here.
     */
    public ConfigurableApplicationContext getContextOrNull() {
        return contextRef.get();
    }

    @Override
    protected ConfigurableApplicationContext call() throws Exception {
        // Stage 1 — Spring boot. Per RESEARCH C-SPLASH-1: SpringApplicationBuilder.run()
        // triggers musicLibrary() bean which loads ALL THREE JSON files synchronously
        // inside FXMusicLibrary.builder().build() (audio + playlist + waveform).
        updateMessage(STAGE_LIBRARY);
        updateProgress(0.0, 1.0);
        ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .sources(applicationClass)
                .run();
        // Cache the context for handleFailure BEFORE flipping the readiness flag.
        // Ordering matters: failures observed via setOnFailed run on a different thread,
        // but the JavaFX Task framework provides a happens-before edge from call() to
        // setOnFailed via task state transitions. Setting contextRef first means any
        // post-Spring failure (e.g. during prewarm later in this method, or during
        // setOnSucceeded scene-build) can find the live context to publish through.
        contextRef.set(context);
        springContextReady.set(true);
        updateProgress(0.25, 1.0);

        // Stage 2 — playlists. The bean is already instantiated by Spring eager init;
        // the access is a no-op trip but the message change is the user-visible signal.
        updateMessage(STAGE_PLAYLISTS);
        context.getBean(ObservablePlaylistHierarchy.class);
        updateProgress(0.5, 1.0);

        // Stage 3 — waveforms.
        updateMessage(STAGE_WAVEFORMS);
        context.getBean(AudioWaveformRepository.class);
        updateProgress(0.75, 1.0);

        // Stage 4 — UI prewarm signaling. The actual prewarm + scene build runs on the
        // FX thread inside the orchestrator's setOnSucceeded handler (FxWeaver
        // loadView is not safe off the FX thread per RESEARCH Open Question 2).
        updateMessage(STAGE_UI);
        // Audio library bean access (so the message-change is bracketed by a real bean
        // request, even though it is a no-op trip).
        context.getBean(ObservableAudioLibrary.class);
        updateProgress(1.0, 1.0);

        return context;
    }
}
