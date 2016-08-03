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
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.transgressoft.musicott.model;

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.util.*;
import javafx.beans.property.*;
import javafx.util.Duration;

import java.io.*;
import java.time.*;
import java.util.*;

/**
 * Represents an audio file and its metadata information.
 *
 * @author Octavio Calleya
 * @version 0.9-b
 */
public class Track {

	private int trackId;
	private String fileFolder;
	private String fileName;
	private String name;
	private String artist;
	private String album;
	private String genre;
	private String comments;
	private String albumArtist;
	private String label;
	private String encoding;
	private String encoder;

	private int size;
	private int bitRate;
	private int playCount;
	private int trackNumber;
	private int discNumber;
	private int year;
	private int bpm;
	private Duration totalTime;

	private boolean inDisk;
	private boolean isPartOfCompilation;
	private boolean isVariableBitRate;

	private LocalDateTime lastDateModified;
	private LocalDateTime dateAdded;

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

	private ObjectProperty<LocalDateTime> dateModifiedProperty;
	private BooleanProperty hasCoverProperty;
	private BooleanProperty isPlayableProperty;

	/**
	 * Map of the properties that are editable in the application
	 */
	private Map<TrackField, Property> propertyMap;

	private String fileFormat;
	private File coverFileToUpdate;
	private MetadataUpdater updater;
	private ErrorDemon errorDemon = ErrorDemon.getInstance();

	public Track() {
		trackId = MainPreferences.getInstance().getTrackSequence();
		fileFolder = "";
		fileName = "";
		fileFormat = "";
		name = "";
		artist = "";
		album = "";
		genre = "";
		comments = "";
		albumArtist = "";
		label = "";
		encoder = "";
		encoding = "";
		trackNumber = 0;
		discNumber = 0;
		year = 0;
		bpm = 0;
		size = 0;
		totalTime = Duration.UNKNOWN;
		bitRate = 0;
		playCount = 0;
		inDisk = false;
		isPartOfCompilation = false;
		isVariableBitRate = false;
		lastDateModified = LocalDateTime.now();
		dateAdded = LocalDateTime.now();
		updater = new MetadataUpdater(this);

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
		isPlayableProperty = new SimpleBooleanProperty(this, "is playable", isPlayable());
		hasCoverProperty = new SimpleBooleanProperty(this, "cover bytes", false);
		getCoverImage().ifPresent(coverBytes -> hasCoverProperty.set(true));

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

	public Optional<byte[]> getCoverImage() {
		File trackFile = new File(fileFolder + "/" + fileName);
		Optional<byte[]>[] coverBytes = new Optional[]{Optional.empty()};
		MetadataParser.getAudioTag(trackFile).ifPresent(tag -> coverBytes[0] = MetadataParser.getCoverBytes(tag));
		return coverBytes[0];
	}

	public void setCoverImage(File cover) {
		coverFileToUpdate = cover;
	}

	public int getTrackId() {
		return trackId;
	}

	public void setTrackId(int trackId) {
		this.trackId = trackId;
	}

	public String getFileFormat() {
		return fileFormat;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getEncoder() {
		return encoder;
	}

	public void setEncoder(String encoder) {
		this.encoder = encoder;
	}

	public Duration getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(Duration totalTime) {
		this.totalTime = totalTime;
	}

	public int getDiscNumber() {
		return discNumber;
	}

	public void setDiscNumber(int discNumber) {
		this.discNumber = discNumber;
		discNumberProperty.setValue(this.discNumber);
	}

	public int getBitRate() {
		return bitRate;
	}

	public void setBitRate(int bitRate) {
		this.bitRate = bitRate;
	}

	public int getPlayCount() {
		return playCount;
	}

	public void setPlayCount(int playCount) {
		this.playCount = playCount;
		playCountProperty.setValue(playCount);
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public boolean isPartOfCompilation() {
		return isPartOfCompilation;
	}

	public boolean isVariableBitRate() {
		return isVariableBitRate;
	}

	public LocalDateTime getLastDateModified() {
		return lastDateModified;
	}

	public void setLastDateModified(LocalDateTime lastDateModified) {
		this.lastDateModified = lastDateModified;
		dateModifiedProperty.set(lastDateModified);
	}

	public LocalDateTime getDateAdded() {
		return dateAdded;
	}

	public void setDateAdded(LocalDateTime dateAdded) {
		this.dateAdded = dateAdded;
	}

	public Map<TrackField, Property> getPropertyMap() {
		return propertyMap;
	}

	public void setIsPartOfCompilation(boolean isPartOfCompilation) {
		this.isPartOfCompilation = isPartOfCompilation;
	}

	public void setIsVariableBitRate(boolean isVariableBitRate) {
		this.isVariableBitRate = isVariableBitRate;
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

	public boolean writeMetadata() {
		boolean updatedCover = coverFileToUpdate == null || updater.updateCover(coverFileToUpdate);
		return updater.writeAudioMetadata() && updatedCover;
	}

	public boolean isPlayable() {
		boolean playable = true;
		if (getInDisk()) {
			File file = new File(fileFolder + "/" + fileName);
			if (! file.exists()) {
				errorDemon.showErrorDialog(fileFolder + "/" + fileName + " not found");
				setInDisk(false);
				playable = false;
			}
			else if ("flac".equals(fileFormat) || encoding.startsWith("Apple") || encoder.startsWith("iTunes"))
				playable = false;
		}
		else
			playable = false;
		return playable;
	}

	public boolean getInDisk() {
		return inDisk;
	}

	public void setInDisk(boolean inDisk) {
		this.inDisk = inDisk;
	}

	public void incrementPlayCount() {
		playCount++;
		playCountProperty.setValue(playCount);
	}

	public String getFileName() {
		return fileName;
	}

	public String getFileFolder() {
		return fileFolder;
	}

	public String getName() {
		return name;
	}

	public String getArtist() {
		return artist;
	}

	public String getAlbum() {
		return album;
	}

	public String getComments() {
		return comments;
	}

	public String getGenre() {
		return genre;
	}

	public int getTrackNumber() {
		return trackNumber;
	}

	public int getYear() {
		return year;
	}

	public String getAlbumArtist() {
		return albumArtist;
	}

	public int getBpm() {
		return bpm;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
		labelProperty.setValue(this.label);
	}

	public void setBpm(int bpm) {
		this.bpm = bpm;
		bpmProperty.setValue(this.bpm);
	}

	public void setAlbumArtist(String albumArtist) {
		this.albumArtist = albumArtist;
		albumArtistProperty.setValue(this.albumArtist);
	}

	public void setYear(int year) {
		this.year = year;
	}

	public void setTrackNumber(int trackNumber) {
		this.trackNumber = trackNumber;
		trackNumberProperty.setValue(this.trackNumber);
	}

	public void setGenre(String genre) {
		this.genre = genre;
		genreProperty.setValue(this.genre);
	}

	public void setComments(String comments) {
		this.comments = comments;
		commentsProperty.setValue(this.comments);
	}

	public void setAlbum(String album) {
		this.album = album;
		albumProperty.setValue(this.album);
	}

	public void setArtist(String artist) {
		this.artist = artist;
		artistProperty.setValue(this.artist);
	}

	public void setName(String name) {
		this.name = name;
		nameProperty.setValue(this.name);
	}

	public void setFileFolder(String fileFolder) {
		this.fileFolder = fileFolder;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
		int pos = fileName.lastIndexOf('.');
		fileFormat = fileName.substring(pos + 1);
	}

	@Override
	public int hashCode() {
		int hash = 71;
		hash = 73 * hash + fileName.hashCode();
		hash = 73 * hash + fileFolder.hashCode();
		hash = 73 * hash + name.hashCode();
		hash = 73 * hash + artist.hashCode();
		hash = 73 * hash + album.hashCode();
		hash = 73 * hash + comments.hashCode();
		hash = 73 * hash + genre.hashCode();
		hash = 73 * hash + trackNumber;
		hash = 73 * hash + year;
		hash = 73 * hash + albumArtist.hashCode();
		hash = 73 * hash + bpm;
		hash = 73 * hash + label.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object o) {
		boolean equals = false;

		if (o instanceof Track) {
			Track oTrack = (Track) o;

			if (oTrack.getFileName().equalsIgnoreCase(fileName) &&
					oTrack.getFileFolder().equalsIgnoreCase(fileFolder) &&
					oTrack.getName().equalsIgnoreCase(name) &&
					oTrack.getArtist().equalsIgnoreCase(artist) &&
					oTrack.getAlbum().equalsIgnoreCase(album) &&
					oTrack.getComments().equalsIgnoreCase(comments) &&
					oTrack.getGenre().equalsIgnoreCase(genre) &&
					oTrack.getTrackNumber() == trackNumber &&
					oTrack.getYear() == year &&
					oTrack.getAlbumArtist().equalsIgnoreCase(albumArtist) &&
					oTrack.getBpm() == bpm &&
					oTrack.getLabel().equalsIgnoreCase(label)) {
				equals = true;
			}
		}
		else {
			equals = false;
		}

		return equals;
	}

	@Override
	public String toString() {
		return name + "|" + artist + "|" + genre + "|" + album + "(" + year + ")|" + bpm + "|" + label;
	}
}
