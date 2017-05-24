/*
 * This file is part of Musicott software.
 *
 * Musicott software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Musicott library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Musicott. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package com.transgressoft.musicott.util;

import com.google.common.base.*;
import com.google.common.collect.*;
import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.view.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.image.*;
import javafx.stage.*;
import javafx.stage.Stage;
import org.apache.commons.lang3.text.*;
import org.slf4j.Logger;

import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.Map.*;
import java.util.Optional;
import java.util.logging.*;
import java.util.regex.*;
import java.util.stream.*;

import static com.transgressoft.musicott.model.CommonObject.*;
import static com.transgressoft.musicott.view.EditController.*;

/**
 * Class that does some useful operations with files, directories, strings
 * or other operations utilities to be used for the application
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 */
public class Utils {

    private static final String LOGGING_PROPERTIES = "resources/config/logging.properties";
    private static final String LOG_FILE = "Musicott-main-log.txt";

    @Inject
    private static Injector injector;

    /**
     * Private constructor to hide the implicit public one.
     */
    private Utils() {}

    /**
     * Returns the names of the artists that are involved in a {@link Track},
     * that is, every artist that could appear in the {@link Track#artist} variable,
     * or {@link Track#albumArtist} or in the {@link Track#name}.
     *
     * <h3>Example</h3>
     *
     * <p>The following Track intance: <pre>   {@code
     *
     *   Track.name = "Who Controls (Adam Beyer Remix)"
     *   Track.artist = "David Meiser, Black Asteroid & Tiga"
     *   Track.albumArtist = "Ida Engberg"
     *
     *   }</pre>
     * ... produces the following (without order): <pre>   {@code
     *      [David Meiser, Black Asteroid, Tiga, Adam Beyer, Ida Engberg]
     *   }</pre>
     *
     * @param track The {@code Track} object
     *
     * @return A {@code Set} object with the names of the artists
     *
     * @since 0.10-b
     */
    public static Set<String> getArtistsInvolvedInTrack(Track track) {
        Set<String> artistsInvolved = new HashSet<>();
        List<String> albumArtistNames = Splitter.on(CharMatcher.anyOf(",&")).trimResults().omitEmptyStrings()
                                                .splitToList(track.getAlbumArtist());
        artistsInvolved.addAll(albumArtistNames);
        artistsInvolved.addAll(getArtistNamesInTrackArtist(track.getArtist()));
        artistsInvolved.addAll(getArtistNamesInTrackName(track.getName()));
        return artistsInvolved.stream().map(WordUtils::capitalize).collect(Collectors.toSet());
    }

    /**
     * Splits a given string if it's separated by ',' or '&' characters, or by
     * the words 'versus' or 'vs', that are commonly used inside a song artist field indicating
     * a non-solo artist work.
     *
     * <h3>Example</h3>
     *
     * <p>The given track artist field: <pre>   {@code
     *   "David Meiser, Black Asteroid & Tiga"
     *   }</pre>
     * ... produces the following set (without order): <pre>   {@code
     *      [David Meiser, Black Asteroid, Tiga]
     *   }</pre>
     *
     * @param string The {@code String} where to find artist names
     *
     * @return An {@link ImmutableSet} with the artists found
     *
     * @since 0.10-b
     */
    private static ImmutableSet<String> getArtistNamesInTrackArtist(String string) {
        ImmutableSet<String> artistsInvolved;
        Optional<List<String>> splittedNames = Stream.of(" versus ", " vs ").filter(string::contains)
                                                     .map(s -> Splitter.on(s).trimResults().omitEmptyStrings()
                                                                       .splitToList(string)).findAny();

        if (splittedNames.isPresent())
            artistsInvolved = ImmutableSet.copyOf(splittedNames.get());
        else {
            String cleanedArtist = string.replaceAll("(?i)(feat)(\\.|\\s)", ",").replaceAll("(?i)(ft)(\\.|\\s)", ",");
            artistsInvolved = ImmutableSet.copyOf(Splitter.on(CharMatcher.anyOf(",&")).trimResults().omitEmptyStrings()
                                                          .splitToList(cleanedArtist));
        }
        return artistsInvolved;
    }

    /**
     * Spits a given string matching a regular expression that specifies a song name
     * in which other artists' are involved. For example:
     *
     * <h3>Example</h3>
     *
     * <p>The given track name field: <pre>   {@code
     *   "Song name (Adam Beyer & Pete Tong Remix)"
     *   }</pre>
     * ... produces the following (without order): <pre>   {@code
     *      [Adam Beyer, Pete Tong]
     *   }</pre>
     *
     * @param string The {@code String} where to find artist names
     *
     * @return An {@link ImmutableSet} with the artists found
     *
     * @since 0.10-b
     */
    private static Set<String> getArtistNamesInTrackName(String string) {
        Pattern endsWithRemix = Pattern.compile("[(|\\[](\\s*(&?\\s*(\\w+)\\s+)+(?i)(remix))[)|\\]]");
        Pattern startsWithRemixBy = Pattern.compile("[(|\\[](?i)(remix) (?i)(by)(.+)[)|\\]]");
        Pattern hasFt = Pattern.compile("[(|\\[|\\s](?i)(ft) (.+)");
        Pattern hasFeat = Pattern.compile("[(|\\[|\\s](?i)(feat) (.+)");
        Pattern hasFeaturing = Pattern.compile("[(|\\[|\\s](?i)(featuring) (.+)");
        Pattern startsWithWith = Pattern.compile("[(|\\[](?i)(with) (.+)[)|\\]]");

        Map<Pattern, Pattern> regexMaps = ImmutableMap.<Pattern, Pattern> builder()
                .put(Pattern.compile(" (?i)(remix)"), endsWithRemix)
                .put(Pattern.compile("(?i)(remix) (?i)(by) "), startsWithRemixBy)
                .put(Pattern.compile("(?i)(ft) "), hasFt).put(Pattern.compile("(?i)(feat) "), hasFeat)
                .put(Pattern.compile("(?i)(featuring) "), hasFeaturing)
                .put(Pattern.compile("(?i)(with) "), startsWithWith).build();

        Set<String> artistsInsideParenthesis = new HashSet<>();
        for (Entry<Pattern, Pattern> regex : regexMaps.entrySet()) {
            Pattern keyPattern = regex.getKey();
            Matcher matcher = regex.getValue().matcher(string);
            if (matcher.find()) {
                String insideParenthesisString = string.substring(matcher.start()).replaceAll("[(|\\[|)|\\]]", "")
                                                       .replaceAll(keyPattern.pattern(), "");

                artistsInsideParenthesis.addAll(Splitter.on(CharMatcher.anyOf("&,")).trimResults().omitEmptyStrings()
                                                        .splitToList(insideParenthesisString
                                                                             .replaceAll("\\s(?i)(vs)\\s", "&")));
                break;
            }
        }
        return artistsInsideParenthesis;
    }

    /**
     * Retrieves a {@link List} with at most {@code maxFiles} files that are in a folder or
     * any of the subfolders in that folder satisfying a condition.
     * If {@code maxFilesRequired} is 0 all the files will be retrieved.
     *
     * @param rootFolder       The folder from within to find the files
     * @param filter           The {@link FileFilter} condition
     * @param maxFilesRequired Maximum number of files in the List. 0 indicates no maximum
     *
     * @return The list containing all the files
     *
     * @throws IllegalArgumentException Thrown if {@code maxFilesRequired} argument is less than zero
     */
    public static List<File> getAllFilesInFolder(File rootFolder, FileFilter filter, int maxFilesRequired) {
        List<File> finalFiles = new ArrayList<>();
        if (! Thread.currentThread().isInterrupted()) {
            if (maxFilesRequired < 0)
                throw new IllegalArgumentException("maxFilesRequired argument less than zero");
            if (rootFolder == null || filter == null)
                throw new IllegalArgumentException("folder or filter null");
            if (! rootFolder.exists() || ! rootFolder.isDirectory())
                throw new IllegalArgumentException("rootFolder argument is not a directory");

            int remainingFiles = addFilesDependingMax(finalFiles, rootFolder.listFiles(filter), maxFilesRequired);

            if (maxFilesRequired == 0 || remainingFiles > 0) {
                File[] rootSubFolders = rootFolder.listFiles(File::isDirectory);
                addFilesFromFolders(finalFiles, rootSubFolders, maxFilesRequired, remainingFiles, filter);
            }
        }
        return finalFiles;
    }

    /**
     * Add files to a {@link List} depending a {@code maxFilesRequired} parameter.
     * <ul>
     * <li>
     * If it's 0, all files are added.
     * </li>
     * <li>
     * If it's greater than the actual number of files, all files are added too.
     * </li>
     * <li>
     * If it's less than the actual number of files, the required number
     * of files are added
     * </li>
     * </ul>
     *
     * @param files            The collection of final files
     * @param subFiles         An Array of files to add to the collection
     * @param maxFilesRequired The maximum number of files to add to the collection
     *
     * @return The remaining number of files to be added
     */
    private static int addFilesDependingMax(List<File> files, File[] subFiles, int maxFilesRequired) {
        int remainingFiles = maxFilesRequired;
        if (maxFilesRequired == 0)                            // No max = add all files
            files.addAll(Arrays.asList(subFiles));
        else if (maxFilesRequired < subFiles.length) {        // There are more valid files than the required
            files.addAll(Arrays.asList(Arrays.copyOfRange(subFiles, 0, maxFilesRequired)));
            remainingFiles -= files.size();                    // Zero files remaining in the folder
        }
        else if (subFiles.length > 0) {
            files.addAll(Arrays.asList(subFiles));        // Add all valid files
            remainingFiles -= files.size();
        }
        return remainingFiles;
    }

    /**
     * Adds files to a {@link List} from several folders depending of a maximum required files,
     * the remaining files to be added, using a {@link FileFilter}.
     *
     * @param files            The collection of final files
     * @param folders          The folders where the files are
     * @param maxFilesRequired The maximum number of files to add to the collection
     * @param remainingFiles   The remaining number of files to add
     * @param filter           The {@link FileFilter} to use to filter the files in the folders
     */
    private static void addFilesFromFolders(List<File> files, File[] folders, int maxFilesRequired, int remainingFiles,
            FileFilter filter) {
        int subFoldersCount = 0;
        int remaining = remainingFiles;
        while ((subFoldersCount < folders.length) && ! Thread.currentThread().isInterrupted()) {
            File subFolder = folders[subFoldersCount++];
            List<File> subFolderFiles = getAllFilesInFolder(subFolder, filter, remaining);
            files.addAll(subFolderFiles);
            if (remaining > 0)
                remaining = maxFilesRequired - files.size();
            if (maxFilesRequired > 0 && remaining == 0)
                break;
        }
    }

    /**
     * Returns a {@link String} representing the given {@code bytes}, with a textual representation
     * depending if the given amount can be represented as KB, MB, GB or TB, limiting the number
     * of decimals, if there are any
     *
     * @param bytes       The {@code bytes} to be represented
     * @param numDecimals The maximum number of decimals to be shown after the comma
     *
     * @return The {@code String} that represents the given bytes
     *
     * @throws IllegalArgumentException Thrown if {@code bytes} or {@code numDecimals} are negative
     */
    public static String byteSizeString(long bytes, int numDecimals) {
        if (numDecimals < 0)
            throw new IllegalArgumentException("Given number of decimals can't be less than zero");

        String byteSizeString = byteSizeString(bytes);
        StringBuilder decimalSharps = new StringBuilder();
        for (int n = 0; n < numDecimals; n++)
            decimalSharps.append("#");
        DecimalFormat decimalFormat = new DecimalFormat("#." + decimalSharps.toString());
        decimalFormat.setRoundingMode(RoundingMode.CEILING);

        int unitPos = byteSizeString.lastIndexOf(' ');
        String stringValue = byteSizeString.substring(0, unitPos);
        stringValue = stringValue.replace(',', '.');
        float floatValue = Float.parseFloat(stringValue);
        byteSizeString = decimalFormat.format(floatValue) + byteSizeString.substring(unitPos);
        return byteSizeString;
    }

    /**
     * Returns a {@link String} representing the given {@code bytes}, with a textual representation
     * depending if the given amount can be represented as KB, MB, GB or TB
     *
     * @param bytes The {@code bytes} to be represented
     *
     * @return The {@code String} that represents the given bytes
     *
     * @throws IllegalArgumentException Thrown if {@code bytes} is negative
     */
    public static String byteSizeString(long bytes) {
        if (bytes < 0)
            throw new IllegalArgumentException("Given bytes can't be less than zero");

        String sizeText;
        String[] bytesUnits = {"B", "KB", "MB", "GB", "TB"};
        long bytesAmount = bytes;
        short binRemainder;
        float decRemainder = 0;
        int u;
        for (u = 0; bytesAmount > 1024 && u < bytesUnits.length; u++) {
            bytesAmount /= 1024;
            binRemainder = (short) (bytesAmount % 1024);
            decRemainder += Float.valueOf((float) binRemainder / 1024);
        }
        String remainderStr = String.format("%f", decRemainder).substring(2);
        sizeText = bytesAmount + ("0".equals(remainderStr) ? "" : "," + remainderStr) + " " + bytesUnits[u];
        return sizeText;
    }

    /**
     * Returns an {@link Image} from an image {@link File}.
     *
     * @param imageFile The image.
     *
     * @return An {@link Optional} with the {@code image} or not.
     */
    public static Optional<Image> getImageFromFile(File imageFile) {
        ErrorDialogController errorDialog = injector.getInstance(ErrorDialogController.class);
        Optional<Image> optionalImage = Optional.empty();
        try {
            byte[] coverBytes = Files.readAllBytes(Paths.get(imageFile.getPath()));
            optionalImage = Optional.of(new Image(new ByteArrayInputStream(coverBytes)));
        }
        catch (IOException exception) {
            errorDialog.show("Error getting Image from image file", "", exception);
        }
        return optionalImage;
    }

    /**
     * Updates an {@link ImageView}, if possible, with the cover
     * image of a {@link Track}.
     *
     * @param track     The given {@code Track}
     * @param imageView The given {@code ImageView}
     *
     * @return {@code True} if the {@code ImageView} was updated with the
     *          cover of the {@code Track}, {@code False} otherwise.
     */
    public static boolean updateCoverImage(Track track, ImageView imageView) {
        boolean updatedWithCustomImage = false;
        if (track.getCoverImage().isPresent()) {
            byte[] coverBytes = track.getCoverImage().get();
            Image image = new Image(new ByteArrayInputStream(coverBytes));
            imageView.setImage(image);
            updatedWithCustomImage = true;
        }
        else
            imageView.setImage(DEFAULT_COVER_IMAGE);
        return updatedWithCustomImage;
    }

    /**
     * Creates an {@link Alert} given a title, a header text, the content to be shown
     * in the description, the {@link AlertType} of the requested {@code Alert},
     * and the {@link Stage} that owns the alert.
     *
     * @param title   The title of the {@code Alert} stage
     * @param header  The header text of the {@code Alert}
     * @param content The content text of the {@code Alert}
     * @param type    The type of the {@code Alert}
     * @param stage   The stage that owns the {@code Alert}
     *
     * @return The {@code Alert} object
     */
    public static Alert createAlert(String title, String header, String content, AlertType type, Stage stage) {
        Alert alert = new Alert(type);
        alert.getDialogPane().getStylesheets().add(Utils.class.getResource(DIALOG_STYLE.toString()).toExternalForm());
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(stage);
        return alert;
    }

    /**
     * Initializes a {@link Logger} that stores the log entries on a file
     *
     * @see <a href=http://www.slf4j.org/>slf4j</href>
     */
    public static void initializeLogger() throws IOException {
        Handler baseFileHandler;
        LogManager logManager = LogManager.getLogManager();
        java.util.logging.Logger logger = logManager.getLogger("");
        Handler rootHandler = logger.getHandlers()[0];

        logManager.readConfiguration(new FileInputStream(LOGGING_PROPERTIES));
        baseFileHandler = new FileHandler(LOG_FILE);
        baseFileHandler.setFormatter(new SimpleFormatter() {

            @Override
            public String format(LogRecord record) {
                return logTextString(record);
            }
        });
        logManager.getLogger("").removeHandler(rootHandler);
        logManager.getLogger("").addHandler(baseFileHandler);
    }

    /**
     * Constructs a log message given a {@link LogRecord}
     *
     * @param record The {@code LogRecord} instance
     *
     * @return The formatted string of a log entries
     */
    private static String logTextString(LogRecord record) {
        StringBuilder stringBuilder = new StringBuilder();
        String dateTimePattern = "dd/MM/yy HH:mm:ss :nnnnnnnnn";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateTimePattern);

        String logDate = LocalDateTime.now().format(dateFormatter);
        String loggerName = record.getLoggerName();
        String sourceMethod = record.getSourceMethodName();
        String firstLine = loggerName + " " + sourceMethod + " " + logDate + "\n";

        String sequenceNumber = String.valueOf(record.getSequenceNumber());
        String loggerLevel = record.getLevel().toString();
        String message = record.getMessage();
        String secondLine = sequenceNumber + "\t" + loggerLevel + ":" + message + "\n\n";

        stringBuilder.append(firstLine);
        stringBuilder.append(secondLine);

        if (record.getThrown() != null) {
            stringBuilder.append(record.getThrown()).append("\n");
            for (StackTraceElement stackTraceElement : record.getThrown().getStackTrace())
                stringBuilder.append(stackTraceElement).append("\n");
        }
        return stringBuilder.toString();
    }
}
