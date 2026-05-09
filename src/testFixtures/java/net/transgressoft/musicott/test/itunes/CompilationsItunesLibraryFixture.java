package net.transgressoft.musicott.test.itunes;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListParser;

import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Materializes the committed Compilations iTunes XML against generated audio files.
 */
public final class CompilationsItunesLibraryFixture {

    private static final String XML_RESOURCE = "/itunes/compilations-library.xml";
    private static final String SAMPLE_AUDIO_RESOURCE = "/testfiles/testeable.mp3";

    private CompilationsItunesLibraryFixture() {
    }

    public static PreparedLibrary prepare(Path tempDir) {
        try {
            Files.createDirectories(tempDir);
            Path audioDir = tempDir.resolve("audio");
            Files.createDirectories(audioDir);
            NSDictionary root = readXmlResource();
            NSDictionary tracks = tracksDictionary(root);
            for (String trackKey : tracks.allKeys()) {
                rebaseTrackLocation((NSDictionary) tracks.objectForKey(trackKey), audioDir);
            }

            Path runtimeXml = tempDir.resolve("compilations-library-runtime.xml");
            PropertyListParser.saveAsXML(root, runtimeXml.toFile());
            return new PreparedLibrary(runtimeXml, CompilationsItunesLibraryExpectations.load());
        } catch (Exception ex) {
            throw new IllegalStateException("Could not prepare Compilations iTunes fixture", ex);
        }
    }

    private static NSDictionary readXmlResource() throws Exception {
        try (InputStream input = CompilationsItunesLibraryFixture.class.getResourceAsStream(XML_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Could not locate " + XML_RESOURCE);
            }
            return (NSDictionary) PropertyListParser.parse(input);
        }
    }

    private static NSDictionary tracksDictionary(NSDictionary root) {
        NSObject tracks = root.objectForKey("Tracks");
        if (tracks instanceof NSDictionary tracksDictionary) {
            return tracksDictionary;
        }
        throw new IllegalStateException("Fixture XML does not contain a Tracks dictionary");
    }

    private static void rebaseTrackLocation(NSDictionary track, Path audioDir) {
        try {
            String trackId = text(track, "Track ID");
            Path generated = audioDir.resolve("track-" + trackId + ".mp3");
            try (InputStream input = CompilationsItunesLibraryFixture.class.getResourceAsStream(SAMPLE_AUDIO_RESOURCE)) {
                if (input == null) {
                    throw new IllegalStateException("Could not locate " + SAMPLE_AUDIO_RESOURCE);
                }
                Files.copy(input, generated, StandardCopyOption.REPLACE_EXISTING);
            }
            track.put("Location", new NSString(generated.toUri().toString()));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not materialize audio file for track " + text(track, "Track ID"), ex);
        }
    }

    private static String text(NSDictionary track, String key) {
        NSObject value = track.objectForKey(key);
        return value == null ? "" : value.toString();
    }

    /**
     * Prepared runtime XML and its assertions sidecar.
     */
    public record PreparedLibrary(Path xmlPath, CompilationsItunesLibraryExpectations expectations) {
    }
}
