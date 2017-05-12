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

package com.transgressoft.musicott.model;

import com.google.inject.*;
import com.google.inject.assistedinject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.util.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.util.Duration;
import org.apache.commons.lang3.text.*;

import java.io.*;
import java.time.*;
import java.util.*;

/**
 * Represents an audio file and its metadata information.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
public class Track {

    private int trackId;
    private String fileFolder = "";
    private String fileName = "";
    private String fileFormat = "";
    private String name = "";
    private String artist = "";
    private String album = "";
    private String genre = "";
    private String comments = "";
    private String albumArtist = "";
    private String label = "";
    private String encoding = "";
    private String encoder = "";

    private int size = 0;
    private int bitRate = 0;
    private int playCount = 0;
    private int trackNumber = 0;
    private int discNumber = 0;
    private int year = 0;
    private int bpm = 0;
    private Duration totalTime = Duration.UNKNOWN;

    private boolean isInDisk = false;
    private boolean isPartOfCompilation = false;
    private boolean isVariableBitRate = false;

    private LocalDateTime lastDateModified = LocalDateTime.now();
    private LocalDateTime dateAdded = LocalDateTime.now();

    private StringProperty nameProperty;
    private StringProperty artistProperty;
    private StringProperty albumProperty;
    private StringProperty genreProperty;
    private StringProperty commentsProperty;
    private StringProperty albumArtistProperty;
    private StringProperty labelProperty;

    private IntegerProperty trackNumberProperty;
    private IntegerProperty yearProperty;
    private IntegerProperty discNumberProperty;
    private IntegerProperty bpmProperty;
    private IntegerProperty playCountProperty;

    private SetProperty<String> artistsInvolvedProperty;
    private ObjectProperty<LocalDateTime> dateModifiedProperty;
    private BooleanProperty hasCoverProperty;
    private BooleanProperty isPlayableProperty;

    /**
     * Map of the properties that are editable in the application
     */
    private Map<TrackField, Property> propertyMap;
    private ObservableSet<String> artistsInvolved;

    private Optional<File> coverFileToUpdate = Optional.empty();
    private MetadataUpdater updater = new MetadataUpdater(this);
    @Inject
    private ErrorDemon errorDemon;

    public Track() {
        nameProperty = new SimpleStringProperty(this, "name", name);
        nameProperty.addListener((observable, oldString, newString) -> setName(newString));
        artistProperty = new SimpleStringProperty(this, "artist", artist);
        artistProperty.addListener((observable, oldString, newString) -> setArtist(newString));
        albumProperty = new SimpleStringProperty(this, "album", album);
        albumProperty.addListener((observable, oldString, newString) -> setAlbum(newString));
        genreProperty = new SimpleStringProperty(this, "genre", genre);
        genreProperty.addListener((observable, oldString, newString) -> setGenre(newString));
        commentsProperty = new SimpleStringProperty(this, "comments", comments);
        commentsProperty.addListener((observable, oldString, newString) -> setComments(newString));
        albumArtistProperty = new SimpleStringProperty(this, "albumArtist", albumArtist);
        albumArtistProperty.addListener((observable, oldString, newString) -> setAlbumArtist(newString));
        labelProperty = new SimpleStringProperty(this, "label", label);
        labelProperty.addListener((observable, oldString, newString) -> setLabel(newString));
        trackNumberProperty = new SimpleIntegerProperty(this, "track number", trackNumber);
        trackNumberProperty.addListener((observable, oldNumber, newNumber) -> setTrackNumber(newNumber.intValue()));
        yearProperty = new SimpleIntegerProperty(this, "year", year);
        yearProperty.addListener((observable, oldNumber, newNumber) -> setYear(newNumber.intValue()));
        discNumberProperty = new SimpleIntegerProperty(this, "disc number", discNumber);
        discNumberProperty.addListener((observable, oldNumber, newNumber) -> setDiscNumber(newNumber.intValue()));
        bpmProperty = new SimpleIntegerProperty(this, "bpm", bpm);
        bpmProperty.addListener((observable, oldNumber, newNumber) -> setBpm(newNumber.intValue()));
        dateModifiedProperty = new SimpleObjectProperty<>(this, "date modified", lastDateModified);
        dateModifiedProperty.addListener((observable, oldDate, newDate) -> setLastDateModified(newDate));
        playCountProperty = new SimpleIntegerProperty(this, "play count", playCount);
        isPlayableProperty = new SimpleBooleanProperty(this, "is playable", true);
        hasCoverProperty = new SimpleBooleanProperty(this, "cover flag", false);
        artistsInvolvedProperty = new SimpleSetProperty<>(this, "artists involved", artistsInvolved);
        artistsInvolvedProperty.addListener((obs, oldArtists, newArtists) -> setArtistsInvolved(newArtists));

        propertyMap = new EnumMap<>(TrackField.class);
        propertyMap.put(TrackField.NAME, nameProperty);
        propertyMap.put(TrackField.ALBUM, albumProperty);
        propertyMap.put(TrackField.ALBUM_ARTIST, albumArtistProperty);
        propertyMap.put(TrackField.ARTIST, artistProperty);
        propertyMap.put(TrackField.GENRE, genreProperty);
        propertyMap.put(TrackField.COMMENTS, commentsProperty);
        propertyMap.put(TrackField.LABEL, labelProperty);
        propertyMap.put(TrackField.TRACK_NUMBER, trackNumberProperty);
        propertyMap.put(TrackField.DISC_NUMBER, discNumberProperty);
        propertyMap.put(TrackField.YEAR, yearProperty);
        propertyMap.put(TrackField.BPM, bpmProperty);
    }

    @Inject
    public Track(MainPreferences mainPreferences, @Assisted ("fileFolder") String fileFolder,
            @Assisted ("fileName") String fileName) {
        this();
        trackId = mainPreferences.getTrackSequence();
        this.fileFolder = fileFolder;
        setFileName(fileName);
    }

    public void setName(String name) {
        this.name = name.trim().replaceAll("\\s+", " ")
                        .replaceAll(" (?i)(remix)"," Remix")
                        .replaceAll("(?i)(remix) (?i)(by) ","Remix by ")
                        .replaceAll("(?i)(ft)(\\.|\\s) ","ft ")
                        .replaceAll("(?i)(feat)(\\.|\\s) ","feat ")
                        .replaceAll("(?i)(featuring) ", "featuring ")
                        .replaceAll("(?i)(with) ", "with ");
        nameProperty.setValue(this.name);
    }

    public String getName() {
        return name;
    }

    public void setArtist(String artist) {
        this.artist = WordUtils.capitalize(artist).trim()
                               .replaceAll("\\s+", " ")
                               .replaceAll(" (?i)(vs)(\\.|\\s)", " vs ")
                               .replaceAll(" (?i)(versus) ", " versus ");
        artistProperty.setValue(this.artist);
    }

    public String getArtist() {
        return artist;
    }

    public void setAlbum(String album) {
        this.album = album.trim().replaceAll("\\s+", " ");
        albumProperty.setValue(this.album);
    }

    public String getAlbum() {
        return album;
    }

    public void setGenre(String genre) {
        this.genre = genre.trim().replaceAll("\\s+", " ");
        genreProperty.setValue(this.genre);
    }

    public String getGenre() {
        return genre;
    }

    public void setComments(String comments) {
        this.comments = comments.trim().replaceAll("\\s+", " ");
        commentsProperty.setValue(this.comments);
    }

    public String getComments() {
        return comments;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist =  WordUtils.capitalize(albumArtist.trim().replaceAll("\\s+", " "));
        albumArtistProperty.setValue(this.albumArtist);
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setLabel(String label) {
        this.label = label.trim().replaceAll("\\s+", " ");
        labelProperty.setValue(this.label);
    }

    public String getLabel() {
        return label;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
        trackNumberProperty.setValue(this.trackNumber);
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getYear() {
        return year;
    }

    public void setDiscNumber(int discNumber) {
        this.discNumber = discNumber;
        discNumberProperty.setValue(this.discNumber);
    }

    public int getDiscNumber() {
        return discNumber;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
        bpmProperty.setValue(this.bpm);
    }

    public int getBpm() {
        return bpm;
    }

    private void setFileName(String fileName) {
        this.fileName = fileName;
        int pos = fileName.lastIndexOf('.');
        fileFormat = fileName.substring(pos + 1);
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileFolder() {
        return fileFolder;
    }

    public void setIsInDisk(boolean isInDisk) {
        this.isInDisk = isInDisk;
    }

    public boolean isInDisk() {
        return isInDisk;
    }

    public void setCoverImage(File cover) {
        coverFileToUpdate = Optional.ofNullable(cover);
    }

    public Optional<byte[]> getCoverImage() {
        File trackFile = new File(fileFolder, fileName);
        Optional<byte[]>[] coverBytes = new Optional[]{Optional.empty()};
        MetadataParser.getAudioTag(trackFile).ifPresent(tag -> coverBytes[0] = MetadataParser.getCoverBytes(tag));
        return coverBytes[0];
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public int getTrackId() {
        return trackId;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoder(String encoder) {
        this.encoder = encoder;
    }

    public String getEncoder() {
        return encoder;
    }

    public void setTotalTime(Duration totalTime) {
        this.totalTime = totalTime;
    }

    public Duration getTotalTime() {
        return totalTime;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
        playCountProperty.setValue(playCount);
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public void setIsPartOfCompilation(boolean isPartOfCompilation) {
        this.isPartOfCompilation = isPartOfCompilation;
    }

    public boolean isPartOfCompilation() {
        return isPartOfCompilation;
    }

    public void setIsVariableBitRate(boolean isVariableBitRate) {
        this.isVariableBitRate = isVariableBitRate;
    }

    public boolean isVariableBitRate() {
        return isVariableBitRate;
    }

    public void setArtistsInvolved(ObservableSet<String> artistsInvolved) {
        this.artistsInvolved = artistsInvolved;
        if (artistsInvolved.isEmpty())
            artistsInvolved.add(" ");
        artistsInvolvedProperty.set(artistsInvolved);
    }

    public ObservableSet<String> getArtistsInvolved() {
        return artistsInvolved;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setLastDateModified(LocalDateTime lastDateModified) {
        this.lastDateModified = lastDateModified;
        dateModifiedProperty.set(lastDateModified);
    }

    public LocalDateTime getLastDateModified() {
        return lastDateModified;
    }

    public boolean isPlayable() {
        boolean playable = true;
        if (isInDisk()) {
            File file = new File(fileFolder, fileName);
            if (! file.exists()) {
                errorDemon.showErrorDialog(file.getAbsolutePath() + " not found");
                setIsInDisk(false);
                playable = false;
            }
            else if ("flac".equals(fileFormat) || encoding.startsWith("Apple") || encoder.startsWith("iTunes"))
                playable = false;
        }
        else
            playable = false;
        return playable;
    }

    public Map<TrackField, Property> getPropertyMap() {
        return propertyMap;
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public StringProperty artistProperty() {
        return artistProperty;
    }

    public StringProperty albumProperty() {
        return albumProperty;
    }

    public StringProperty genreProperty() {
        return genreProperty;
    }

    public StringProperty commentsProperty() {
        return commentsProperty;
    }

    public StringProperty albumArtistProperty() {
        return albumArtistProperty;
    }

    public StringProperty labelProperty() {
        return labelProperty;
    }

    public IntegerProperty trackNumberProperty() {
        return trackNumberProperty;
    }

    public IntegerProperty yearProperty() {
        return yearProperty;
    }

    public IntegerProperty playCountProperty() {
        return this.playCountProperty;
    }

    public IntegerProperty discNumberProperty() {
        return discNumberProperty;
    }

    public IntegerProperty bpmProperty() {
        return bpmProperty;
    }

    public ObjectProperty<LocalDateTime> lastDateModifiedProperty() {
        return dateModifiedProperty;
    }

    public BooleanProperty hasCoverProperty() {
        return hasCoverProperty;
    }

    public BooleanProperty isPlayableProperty() {
        return isPlayableProperty;
    }

    public SetProperty<String> artistsInvolvedProperty() {
        return artistsInvolvedProperty;
    }

    /**
     * Updates the cover image and the metadata of the audio file that is represented by
     * this {@link Track} instance with the information that has been entered by the application to it
     *
     * @throws TrackUpdateException If something went bad updating the metadata
     */
    public void writeMetadata() throws TrackUpdateException {
        updater.writeAudioMetadata();
        if (coverFileToUpdate.isPresent()) {
            updater.updateCover(coverFileToUpdate.get());
            coverFileToUpdate = Optional.empty();
        }
    }

    public void incrementPlayCount() {
        playCount++;
        playCountProperty.setValue(playCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, fileFolder);
    }

    @Override
    public boolean equals(Object o) {
        boolean equals = false;

        if (o instanceof Track) {
            Track oTrack = (Track) o;
            if (oTrack.getFileName().equalsIgnoreCase(fileName) && oTrack.getFileFolder().equalsIgnoreCase(fileFolder))
                equals = true;
        }

        return equals;
    }

    @Override
    public String toString() {
        return name + "|" + artist + "|" + genre + "|" + album + "(" + year + ")|" + bpm + "|" + label;
    }
}
