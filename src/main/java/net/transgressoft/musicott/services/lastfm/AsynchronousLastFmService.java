package net.transgressoft.musicott.services.lastfm;

import net.transgressoft.commons.music.audio.ReactiveAudioItem;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/**
 * Encapsulates some operations to the external music service LastFM.
 *
 * @author Octavio Calleya
 */
public class AsynchronousLastFmService implements LastFmService {

    private static final Logger LOG = LoggerFactory.getLogger(AsynchronousLastFmService.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(LastFmService.class);

    //  Keys used to store and retrieve some values into the {@link Preferences}
    private static final String SESSION_KEY = "lastfm_session_key";

    private static final String LASTFM_AUTHENTICATION_URL = "https://www.last.fm/api/auth/?api_key=%s&token=%s";

    // List of audio items that were not possible to scrobble successfully, saved to be
    // scrobbled later, ordered by timestamp in 'UNIX seconds'. See addTrackToScrobbleLater method
    private final List<Map<Integer, ReactiveAudioItem<?>>> audioItemBatches = new ArrayList<>();

    private final String apiKey;
    private final Supplier<Stage> stageSupplier;
    private final LastFmApi lastFmApi;
    private final BooleanProperty loggedInProperty;

    private String sessionKey;

    /**
     * Default constructor
     *
     * @param apiKey        The api key of this application in LastFM
     * @param apiSecret     The api secret of this application in LastFM
     * @param stageSupplier A {@link Supplier<Stage>} that will be used to get the {@link Stage} instance
     *                      where the authentication page will be shown inside a {@link WebView}
     */
    public AsynchronousLastFmService(String apiKey, String apiSecret, Supplier<Stage> stageSupplier) {
        this.apiKey = apiKey;
        this.stageSupplier = stageSupplier;
        lastFmApi = new LastFmApi(apiKey, apiSecret);
        audioItemBatches.add(new HashMap<>());
        loggedInProperty = new SimpleBooleanProperty(this, "logged in LastFM", false);

        // If the session key is available, the service is ready
        if (preferences.get(SESSION_KEY, null) != null) {
            LOG.info("LastFM session key loaded");
            loggedInProperty.set(true);
            sessionKey = preferences.get(SESSION_KEY, "");
        }
    }

    @Override
    public CompletableFuture<Boolean> logIn() {
        String storedSessionKey = preferences.get(SESSION_KEY, "");

        if (! storedSessionKey.equals(sessionKey)) {
            return startLastFmAuthenticationProcess();
        } else {
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            result.complete(true);
            return result;
        }
    }

    /**
     * Performs the authentication process.
     *
     * @return A {@link CompletableFuture} instance with a boolean result indicating the success of the operation
     */
    private CompletableFuture<Boolean> startLastFmAuthenticationProcess() {
        return lastFmApi.logIn(showLastFmUrlAuthentication())
                .thenCompose(sessionResponse -> {
                    if (! LastFmApi.OK.equals(sessionResponse.body().getStatus())) {
                        LOG.error("LastFM session request failed");
                        return CompletableFuture.completedFuture(false);
                    } else {
                        sessionKey = sessionResponse.body().getSession().getKey();
                        loggedInProperty.set(true);
                        return CompletableFuture.completedFuture(true);
                    }
                });
    }

    /**
     * Performs the 2nd step of the authentication process displaying a {@link WebView} with
     * LastFM log in page requesting the user to grant permission to this application.
     *
     * @return The function that receives the token and returns a {@link CompletableFuture<String>}
     * that if completed successfully, has the token too, but it is known that the user granted permission
     * to this application by then.
     */
    private Function<String, CompletableFuture<String>> showLastFmUrlAuthentication() {
        return token -> {
            CompletableFuture<String> authenticatedToken = new CompletableFuture<>();
            Platform.runLater(() -> {
                // If the WebView is closed manually it must have been before the authentication has been granted, since
                // it is closed only automatically when process was successful, the result is completed with a false value.
                Stage stage = stageSupplier.get();
                stage.setOnCloseRequest(event -> {
                    if (! authenticatedToken.isDone()) {
                        authenticatedToken.complete(null);
                    }
                });
                WebView webView = new WebView();
                WebEngine webEngine = webView.getEngine();
                webEngine.documentProperty().addListener(authenticationProcessPageListener(stage, token, authenticatedToken));
                webEngine.load(String.format(LASTFM_AUTHENTICATION_URL, apiKey, token));

                stage.setScene(new Scene(webView));
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.show();
                stage.toFront();
            });
            return authenticatedToken;
        };
    }

    /**
     * The {@link ChangeListener<Document>} in order to react to changes in the web pages displayed.
     * If the user successfully logs in and grants authorization, the confirmation page will
     * contain <tt>"You have granted permission"</tt> message.
     * When this happens the session key request is performed.
     * If the success does not happen for after a certain time
     *
     * @param stage              The {@link Stage} object where the {@link WebView} is shown
     * @param token              The api token obtained in the 1st step of the authentication process
     * @param authenticatedToken A {@link CompletableFuture} instance to be updated with the result of the process
     *
     * @return The {@link ChangeListener<Document>} for the {@link WebView} document's change
     */
    private ChangeListener<Document> authenticationProcessPageListener(Stage stage, String token, CompletableFuture<String> authenticatedToken) {
        return (observable, oldPage, newPage) -> {
            if (newPage != null) {

                // It is known that that the success message is contained in a <p> tag
                // This can change if LastFM updates the content of the page and needs to be maintained
                final String SUCCESS_MESSAGE = "You have granted permission";
                var nodeList = newPage.getElementsByTagName("p");

                for (int i = 0; i < nodeList.getLength(); i++) {
                    var item = nodeList.item(i);
                    if (item.getTextContent().contains(SUCCESS_MESSAGE)) {

                        Platform.runLater(stage::close);
                        authenticatedToken.complete(token);
                        break;
                    }
                }
            }
        };
    }

    @Override
    public void logOut() {
        sessionKey = null;
        preferences.put(SESSION_KEY, null);
        loggedInProperty.set(false);
    }

    @Override
    public CompletableFuture<Boolean> updateNowPlaying(ReactiveAudioItem<?> audioItem) {
        CompletableFuture<Boolean> updateNowPlayingResult = new CompletableFuture<>();
        if (loggedInProperty.get()) {

            lastFmApi.updateNowPlaying(audioItem, sessionKey)
                    .whenComplete((response, exception) -> {
                        if (! LastFmApi.OK.equals(response.body().getStatus()) || exception != null) {
                            updateNowPlayingResult.complete(false);
                            LOG.error("LastFM updateNowPlaying request failed", exception);
                        } else {
                            updateNowPlayingResult.complete(true);
                            LOG.info("LastFM updateNowPlaying request successful {}", response.body().getNowPlaying());
                        }
                    });
        }
        return updateNowPlayingResult;
    }

    @Override
    public CompletableFuture<Boolean> scrobble(ReactiveAudioItem<?> audioItem) {
        CompletableFuture<Boolean> scrobbleResult = new CompletableFuture<>();
        if (loggedInProperty().get()) {

            int timeStamp = (int) (System.currentTimeMillis() / 1000);
            lastFmApi.scrobbleTracks(Collections.singletonMap(timeStamp, audioItem), sessionKey)
                    .whenComplete((response, exception) -> {
                        if (! LastFmApi.OK.equals(response.body().getStatus()) || exception != null) {
                            scrobbleResult.complete(false);
                            LOG.error("LastFM scrobble request failed", exception);
                            addTrackToScrobbleLater(audioItem, timeStamp);
                        } else {
                            LOG.info("LastFM updateNowPlaying request successful {}", response.body().getScrobbles());

                            // Given that the request was successful, scrobble items saved for later
                            scrobbleAudioItemsSavedForLater();
                            scrobbleResult.complete(true);
                        }
                    });
        }
        return scrobbleResult;
    }

    /**
     * Stores an {@link ReactiveAudioItem<?>} that hasn't been scrobbled successfully in order to scrobble then in batch.
     * The batch can have a size up to 50 items to be scrobbled on the LastFM service, so if a batch is filled,
     * other one is created an added to the list.
     *
     * @param audioItem The {@link ReactiveAudioItem<?>} to add
     * @param timeStamp The time in UNIX timestamp format (integer number of seconds since 00:00:00, January 1st 1970 UTC).
     *                  This must be in the UTC time zone.
     *
     * @see <a href="https://www.last.fm/api/show/track.scrobble">LastFM API documentation</a>
     */
    private void addTrackToScrobbleLater(ReactiveAudioItem<?> audioItem, int timeStamp) {
        ListIterator<Map<Integer, ReactiveAudioItem<?>>> batchListIterator = audioItemBatches.listIterator();
        boolean done = false;

        // Iterate over the existing Map of AudioItems in order to add the new one on the batch that
        // has less than 50 items
        while (batchListIterator.hasNext() && ! done) {
            Map<Integer, ReactiveAudioItem<?>> trackBatch = batchListIterator.next();

            if (trackBatch.size() < 50) {
                trackBatch.put(timeStamp, audioItem);
                done = true;
            } else if (! batchListIterator.hasNext()) {
                Map<Integer, ReactiveAudioItem<?>> newMapBatch = new TreeMap<>();
                newMapBatch.put(timeStamp, audioItem);
                batchListIterator.add(newMapBatch);
                done = true;
            }
        }
    }

    private void scrobbleAudioItemsSavedForLater() {
        audioItemBatches.stream()
                .filter(mapBatch -> ! mapBatch.isEmpty())
                .forEach(audioItems -> lastFmApi.scrobbleTracks(audioItems, sessionKey));
        audioItemBatches.clear();
        audioItemBatches.add(new HashMap<>());
    }

    @Override
    public CompletableFuture<List<LastFmListenedTrack>> getRecentListenedTracksInLastDays(String user, int limit, int lastDays) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateMinusLastDays = now.minusDays(lastDays);
        return getRecentListenedTracksBetweenTimeRange(user, limit, dateMinusLastDays, now);
    }

    @Override
    public CompletableFuture<List<LastFmListenedTrack>> getRecentListenedTracksBetweenTimeRange(String user, int limit, LocalDateTime from, LocalDateTime to) {
        long fromEpocSeconds = from.toEpochSecond(ZoneOffset.ofTotalSeconds(0));
        long toEpocSeconds = to.toEpochSecond(ZoneOffset.ofTotalSeconds(0));

        return lastFmApi.getRecentTracks(user, limit, fromEpocSeconds, toEpocSeconds)
                .whenComplete((response, exception) -> {
                    if (response.body().getListenedTracks() == null || exception != null) {
                        LOG.error("LastFM get recent tracks request failed", exception);
                    } else {
                        LOG.info("LastFM get recent tracks request successful {}", response.body());
                    }
                })
                .thenApply(this::fromResponseToListenedTracks);
    }

    private List<LastFmListenedTrack> fromResponseToListenedTracks(HttpResponse<LastFmRecentTracksResponse> lastFmRecentTracksResponse) {
        return lastFmRecentTracksResponse.body().getListenedTracks();
    }

    @Override
    public ReadOnlyBooleanProperty loggedInProperty() {
        return loggedInProperty;
    }
}
