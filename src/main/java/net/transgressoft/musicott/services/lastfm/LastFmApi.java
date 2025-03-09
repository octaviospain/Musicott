package net.transgressoft.musicott.services.lastfm;

import net.transgressoft.commons.music.audio.ReactiveAudioItem;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.io.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Performs operations on the LastFM API asynchronously.
 *
 * @author Octavio Calleya
 * @see <a href="https://www.last.fm/api/">LastFM API documentation</a>
 */
public class LastFmApi {

    // Successful request status response
    public static final String OK = "ok";

    private static final Logger LOG = LoggerFactory.getLogger(LastFmApi.class.getName());

    private static final String EXCEPTION_MESSAGE = "LastFM API call %s exception";

    // The LastFM API endpoint
    private static final String API_ROOT_URL = "https://ws.audioscrobbler.com/2.0/";

    // The different 'Methods' used to query the API
    private static final String GET_TOKEN = "auth.gettoken";
    private static final String GET_SESSION = "auth.getSession";
    private static final String UPDATE_PLAYING = "track.updateNowPlaying";
    private static final String TRACK_SCROBBLE = "track.scrobble";
    private static final String GET_RECENT_TRACKS = "user.getRecentTracks";

    // The application credentials needed to communicate with the LastFM API
    private final String apiKey;
    private final String apiSecret;

    public LastFmApi(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    /**
     * Performs the 1st step in the LastFM API authentication process, requesting a token.
     * The token is valid for 60 minutes from the moment it is granted and can be used only once on  an authentication process.
     * It will be consumed when creating the web service session in the 2nd step, where the user grants authorization to this application
     * by logging into LastFM with her/his account on a specified we page of the form
     * </p>
     *
     * <pre>
     *       {@code http://www.last.fm/api/auth/?api_key=xxxxxxxxxxx&token=xxxxxxxx}
     * </pre>
     * <p>
     * This 2nd step is performed somehow, for instance, showing a {@link javafx.scene.web.WebView} with the mentioned URL or
     * redirecting the user using the installed navigator. If the authorization succeeds, performs the 3rd step requesting a session key
     * which is given in the returned object.
     *
     * @param userAuthenticationFunction The function that receives the token and returns a {@link CompletableFuture<String>}
     *                                   that if completed successfully, has the token too, but it is known that the user granted permission
     *                                   to this application by then.
     *
     * @return A {@link CompletableFuture} object with the {@link HttpResponse} of the asynchronous API request containing a
     * {@link LastFmTokenResponse} entity which is a wrapper of the response.
     * This wrapper contains the token, a 32-character ASCII hexadecimal MD5
     *
     * @see <a href="https://www.last.fm/api/desktopauth">LastFM API Desktop Authentication</a>
     * @see <a href="https://www.last.fm/api/show/auth.getToken">auth.gettoken LastFM API method reference</a>
     */
    public CompletableFuture<HttpResponse<LastFmSessionResponse>> logIn(Function<String, CompletableFuture<String>> userAuthenticationFunction) {
        return makeRequest(new TreeMap<>(), GET_TOKEN, LastFmTokenResponse.class)
                .thenCompose(tokenResponse -> {
                    if (! LastFmApi.OK.equals(tokenResponse.body().getStatus()))
                        throw new CompletionException("LastFM token request failed", null);
                    return userAuthenticationFunction.apply(tokenResponse.body().getToken());
                })
                .thenCompose(this::sessionRequest);
    }

    /**
     * Performs the 3rd step in the LastFM API authentication process, requesting a session key.
     * Session keys have an infinite lifetime by default. It is recommended to store the key securely.
     *
     * @param token The authentication token obtained during the the {@link #logIn(Function)} method call.
     *
     * @return A {@link CompletableFuture} object with the {@link HttpResponse} of the asynchronous API request containing a
     * {@link LastFmSessionResponse} entity which is a wrapper of the response.
     *
     * @see <a href="https://www.last.fm/api/desktopauth">LastFM API Desktop Authentication</a>
     * @see <a href="https://www.last.fm/api/show/auth.getSession">auth.getSession LastFM API method reference</a>
     */
    private CompletableFuture<HttpResponse<LastFmSessionResponse>> sessionRequest(String token) {
        Objects.requireNonNull(token, "token cannot be null");

        TreeMap<String, String> parameters = new TreeMap<>();
        parameters.put("token", token);

        return makeRequest(parameters, GET_SESSION, LastFmSessionResponse.class);
    }

    /**
     * Performs a 'Now Playing' request, which updates the audio that is being currently played on the user's profile.
     *
     * @param audioItem  An {@link ReactiveAudioItem<?>} object with the information of the track
     * @param sessionKey A session key generated by authenticating a user via the authentication protocol
     *
     * @return A {@link CompletableFuture} object with the {@link HttpResponse} of the asynchronous API request containing a
     * {@link LastFmUpdateNowPlayingResponse} entity which is a wrapper of the response.
     *
     * @see <a href="https://www.last.fm/api/show/track.updateNowPlaying">auth.updateNowPlaying LastFM API method reference</a>
     */
    public CompletableFuture<HttpResponse<LastFmUpdateNowPlayingResponse>> updateNowPlaying(ReactiveAudioItem<?> audioItem, String sessionKey) {
        Objects.requireNonNull(sessionKey, "sessionKey cannot be null");

        TreeMap<String, String> parameters = new TreeMap<>();
        parameters.put("artist", audioItem.getArtist().getName());
        parameters.put("track", audioItem.getTitle());
        parameters.put("sk", sessionKey);

        return makeRequest(parameters, UPDATE_PLAYING, LastFmUpdateNowPlayingResponse.class);
    }

    /**
     * Performs a scrobble request, which updates on the user's profile that certain audio items have been listened.
     *
     * @param audioItems A {@link Map} of {@link ReactiveAudioItem<?>} ordered by timestamps, in UNIX timestamp format
     *                   (integer number of seconds since 00:00:00, January 1st 1970 UTC). This must be in the UTC time zone.
     * @param sessionKey A session key generated by authenticating a user via the authentication protocol
     *
     * @return A {@link CompletableFuture} object with the {@link HttpResponse} of the asynchronous API request containing a
     * {@link LastFmScrobbleResponse} entity which is a wrapper of the response.
     *
     * @see <a href="https://www.last.fm/api/show/track.scrobble">auth.scrobble LastFM API method reference</a>
     */
    public CompletableFuture<HttpResponse<LastFmScrobbleResponse>> scrobbleTracks(Map<Integer, ReactiveAudioItem<?>> audioItems, String sessionKey) {
        TreeMap<String, String> parameters = new TreeMap<>();
        int i = 0;
        for (Map.Entry<Integer, ReactiveAudioItem<?>> entry : audioItems.entrySet()) {
            int timeStamp = entry.getKey();
            ReactiveAudioItem<?> audioItem = entry.getValue();
            parameters.put("artist[" + i + "]", audioItem.getArtist().getName());
            parameters.put("track[" + i + "]", audioItem.getTitle());
            parameters.put("timestamp[" + i + "]", Integer.toString(timeStamp));
            i++;
        }
        parameters.put("sk", sessionKey);

        return makeRequest(parameters, TRACK_SCROBBLE, LastFmScrobbleResponse.class);
    }

    /**
     * Performs a request in order to get a list of recent listened tracks by given user. This API method does not require authentication.
     *
     * @param user          The LastFM username to fetch the recent tracks of
     * @param limit         The number of results to fetch per page. Defaults to 50. Maximum is 200
     * @param fromTimeStamp Beginning timestamp of a range - only display scrobbles after this time, in UNIX timestamp format
     *                      (integer number of seconds since 00:00:00, January 1st 1970 UTC). This must be in the UTC time zone.
     * @param toTimesTamp   End timestamp of a range - only display scrobbles before this tim, in UNIX timestamp
     *                      format as <tt><fromTimestamp/tt>}
     *
     * @return A {@link CompletableFuture} object with the {@link HttpResponse} of the asynchronous API request containing a
     * {@link LastFmRecentTracksResponse} entity which is a wrapper of the response.
     *
     * @see <a href="https://www.last.fm/api/show/user.getRecentTracks">auth.scrobble LastFM API method reference</a>
     */
    public CompletableFuture<HttpResponse<LastFmRecentTracksResponse>> getRecentTracks(String user, int limit,
                                                                                       long fromTimeStamp, long toTimesTamp) {

        Objects.requireNonNull(user, "userName cannot be null");

        // Require timestamps within valid time range
        Objects.checkIndex((int) fromTimeStamp, (int) toTimesTamp);
        // Require limit to be between 0 and 200 inclusive
        Objects.checkIndex(limit, 201);

        TreeMap<String, String> parameters = new TreeMap<>();
        parameters.put("user", user);
        parameters.put("from", String.valueOf(fromTimeStamp));
        parameters.put("to", String.valueOf(toTimesTamp));

        return makeRequest(parameters, GET_RECENT_TRACKS, LastFmRecentTracksResponse.class);
    }

    /**
     * Add necessary API credentials and signature to the parameters be sent in the requests.
     *
     * @param parameters The {@link TreeMap} of parameters
     * @param method     The API 'method' call
     *
     * @throws NoSuchAlgorithmException If the MD5 hashing algorithm neede form the signature is not available
     */
    private void addApiParameters(TreeMap<String, String> parameters, String method) throws NoSuchAlgorithmException {
        parameters.put("method", method);
        parameters.put("api_key", apiKey);
        parameters.put("api_sig", buildSignature(parameters));
    }

    /**
     * Constructs the API method signature (a hash) given the parameters of the request, that are expected to be
     * ordered.
     *
     * @param params The {@link TreeMap} from which the signature is constructed
     *
     * @return A <tt>String</tt> of 32 hexadecimal characters the signature
     *
     * @throws NoSuchAlgorithmException If the MD5 hashing algorithm neede form the signature is not available
     * @see <a href="https://www.last.fm/api/desktopauth#_6-sign-your-calls">LastFM API method signature reference</a>
     */
    private String buildSignature(TreeMap<String, String> params) throws NoSuchAlgorithmException {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet())
            stringBuilder.append(entry.getKey()).append(entry.getValue());
        stringBuilder.append(apiSecret);
        return getMd5Hash(stringBuilder.toString());
    }

    /**
     * Constructs an MD5 hash of a given <tt>String</tt>.
     *
     * @param message The message <tt>String</tt>
     *
     * @return The MD5 hash
     *
     * @throws NoSuchAlgorithmException If the hashing algorithm is not available
     */
    private String getMd5Hash(String message) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] array = messageDigest.digest(message.getBytes());
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : array)
            stringBuilder.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);

        return stringBuilder.toString();
    }

    /**
     * Creates an {@link HttpRequest} object given ordered parameters.
     *
     * @param parameters A {@link TreeMap} of parameters
     *
     * @return The {@link HttpRequest} object
     */
    private HttpRequest buildRequest(TreeMap<String, String> parameters) {
        LOG.info("LastFM API request created\n{}", parameters);
        return HttpRequest.newBuilder()
                .uri(URI.create(API_ROOT_URL))
                .POST(ofFormData(parameters))
                .build();
    }

    /**
     * Converts a {@link Map} into a {@link java.net.http.HttpRequest.BodyPublisher} that contains a <tt>String</tt>
     * representing the keys and values of it, in the expected format that an HTTP request expects.
     * Example:
     *
     * <pre>{@code
     * method=getSession&token=1982798zx902&api_key=zxlkalkwiojdlk
     * }
     *
     * @param data The {@link Map} to convert
     * @return An {@link java.net.http.HttpRequest.BodyPublisher} containing the <tt>String</tt> with the parameters
     */
    private HttpRequest.BodyPublisher ofFormData(Map<String, String> data) {
        var builder = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

    /**
     * Performs the the HTTP request asynchronously to the LastFM API and returns a {@link CompletableFuture} with the {@link HttpResponse}
     * wrapping the body into a known entity.
     *
     * @param parameters     The {@link TreeMap} of request parameters
     * @param apiMethod      The LastFM api method request
     * @param responseEntity The {@link Class} to deserialize the response body into
     * @param <T>            The type argument of the method
     *
     * @return A {@link CompletableFuture} object with the {@link HttpResponse} of the asynchronous API request
     */
    private <T> CompletableFuture<HttpResponse<T>> makeRequest(TreeMap<String, String> parameters, String apiMethod, Class<T> responseEntity) {
        try {
            addApiParameters(parameters, apiMethod);
            var request = buildRequest(parameters);
            return HttpClient.newHttpClient()
                    .sendAsync(request, new XmlBodyHandler<>(responseEntity))
                    .thenApply(response -> {
                        LOG.info("LastFM API response status: {}, body: \n{}", response.statusCode(), response.body());
                        return response;
                    });
        }
        catch (NoSuchAlgorithmException exception) {
            LOG.error(String.format(EXCEPTION_MESSAGE, apiMethod), exception);
            return CompletableFuture.failedFuture(exception);
        }
    }

    /**
     * Converts an XML message coming from and {@link HttpResponse} to a known type using Jackson library.
     *
     * @param <T> The known type to convert the XML message into.
     *
     * @see java.net.http.HttpResponse.BodyHandler
     * @see <a href="https://github.com/FasterXML/jackson-dataformat-xml">Jackson</a>
     */
    private static class XmlBodyHandler<T> implements HttpResponse.BodyHandler<T> {

        private final Class<T> targetClass;

        public XmlBodyHandler(Class<T> targetClass) {
            this.targetClass = targetClass;
        }

        @Override
        public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
            return asXml(this.targetClass);
        }

        private static <W> HttpResponse.BodySubscriber<W> asXml(Class<W> targetType) {
            HttpResponse.BodySubscriber<InputStream> upstream = HttpResponse.BodySubscribers.ofInputStream();

            return HttpResponse.BodySubscribers.mapping(
                    upstream,
                    inputStream -> toSupplierOfType(inputStream, targetType).get()
            );
        }

        private static <W> Supplier<W> toSupplierOfType(InputStream inputStream, Class<W> targetType) {
            return () -> {
                String xmlString = null;
                try (InputStream stream = inputStream) {
                    xmlString = CharStreams.toString(new InputStreamReader(stream));
                    LOG.debug("Deserializing XML response:\n{}", xmlString);
                    return new XmlMapper().readValue(xmlString, targetType);
                }
                catch (IOException exception) {
                    LOG.error("Exception trying to deserialize XML response {}", xmlString, exception);
                    throw new UncheckedIOException(exception);
                }
            };
        }
    }
}
