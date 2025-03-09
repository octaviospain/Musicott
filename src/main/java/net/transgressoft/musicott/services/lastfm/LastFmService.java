package net.transgressoft.musicott.services.lastfm;

import net.transgressoft.commons.music.audio.AudioItem;
import net.transgressoft.commons.music.audio.ReactiveAudioItem;

import javafx.beans.property.ReadOnlyBooleanProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Encapsulates some operations to the external music service LastFM.
 *
 * @author Octavio Calleya
 */
public interface LastFmService {

    /**
     * Starts the authentication process in order to log into a LastFM user account asynchronously,
     * allowing this service to perform user account operations.
     *
     * @return A {@link CompletableFuture} instance with a boolean result indicating the success of the operation
     */
    CompletableFuture<Boolean> logIn();

    /**
     * Logs out from the user's LastFM account, disabling any further operation on the user account until authenticated again.
     */
    void logOut();

    /**
     * Performs a 'Now Playing' request, which updates the audio item that is being currently played on the user's profile.
     *
     * @param audioItem The {@link AudioItem} object
     *
     * @return A {@link CompletableFuture} instance with a boolean result indicating the success of the operation
     */
    CompletableFuture<Boolean> updateNowPlaying(ReactiveAudioItem<?> audioItem);

    /**
     * Performs a scrobble request, which updates on the user's profile that certain audio item have been listened.
     *
     * @param audioItem The {@link AudioItem} object
     *
     * @return A {@link CompletableFuture} instance with a boolean result indicating the success of the operation
     */
    CompletableFuture<Boolean> scrobble(ReactiveAudioItem<?> audioItem);

    /**
     * Performs a request in order to get the listened tracks from a given user in the last days given as parameter.
     *
     * @param user     The LastFM username to fetch the recent tracks of
     * @param limit    The number of results to fetch per page. Defaults to 50. Maximum is 200
     * @param lastDays Number of days since the current one given by {@link LocalDateTime#now()}
     *
     * @return A {@link CompletableFuture} instance with a {@link List} of {@link LastFmListenedTrack} objects
     */
    CompletableFuture<List<LastFmListenedTrack>> getRecentListenedTracksInLastDays(String user, int limit, int lastDays);

    /**
     * Performs a request in order to get recent listened tracks on a given user's profile between a given time.
     *
     * @param user  The LastFM username to fetch the recent tracks of
     * @param limit The number of results to fetch per page. Defaults to 50. Maximum is 200
     * @param from  The {@link LocalDateTime} representing the lower bound of the time range.
     * @param to    The {@link LocalDateTime} representing the upper bound of the time range.
     *
     * @return A {@link CompletableFuture} instance with a {@link List} of {@link LastFmListenedTrack} objects
     */
    CompletableFuture<List<LastFmListenedTrack>> getRecentListenedTracksBetweenTimeRange(String user, int limit, LocalDateTime from, LocalDateTime to);

    /**
     * The observable object which flags when the service is logged into the LastFM api or not.
     *
     * @return The {@link ReadOnlyBooleanProperty} object
     */
    ReadOnlyBooleanProperty loggedInProperty();
}
