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

package com.musicott.model;

import com.musicott.*;
import com.musicott.util.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.util.Duration;
import org.jaudiotagger.audio.*;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.tag.*;

import java.io.*;
import java.time.*;
import java.util.*;

/**
 * @author Octavio Calleya
 *
 */
public class Track {
	
	private int trackID;
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
	private boolean isCompilation;
	private boolean isVariableBitRate;
	private boolean hasCover;

	private LocalDateTime dateModified;
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
	
	private BooleanProperty hasCoverProperty;

	private ObjectProperty<LocalDateTime> dateModifiedProperty;
	private Map<TrackField,Property<?>> propertyMap;
	private String fileFormat;
	private MetadataUpdater updater;
	
    public Track() {
    	trackID = MainPreferences.getInstance().getTrackSequence();
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
    	isCompilation = false;
    	hasCover = false;
    	isVariableBitRate = false;
    	dateModified = LocalDateTime.now();
    	dateAdded = LocalDateTime.now();
    	updater = new MetadataUpdater(this);
    	
    	nameProperty = new SimpleStringProperty(name);
    	nameProperty.addListener((observable, oldString, newString) -> setName(newString));
    	artistProperty = new SimpleStringProperty(artist);
    	artistProperty.addListener((observable, oldString, newString) -> setArtist(newString));
    	albumProperty = new SimpleStringProperty(album);
    	albumProperty.addListener((observable, oldString, newString) -> setAlbum(newString));
    	genreProperty = new SimpleStringProperty(genre);
    	genreProperty.addListener((observable, oldString, newString) -> setGenre(newString));
    	commentsProperty = new SimpleStringProperty(comments);
    	commentsProperty.addListener((observable, oldString, newString) -> setComments(newString));
    	albumArtistProperty = new SimpleStringProperty(albumArtist);
    	albumArtistProperty.addListener((observable, oldString, newString) -> setAlbumArtist(newString));
    	labelProperty = new SimpleStringProperty(label);
    	labelProperty.addListener((observable, oldString, newString) -> setLabel(newString));
    	trackNumberProperty = new SimpleIntegerProperty(trackNumber);
    	trackNumberProperty.addListener((observable, oldNumber, newNumber) -> setTrackNumber(newNumber.intValue()));
    	yearProperty = new SimpleIntegerProperty(year);
    	yearProperty.addListener((observable, oldNumber, newNumber) -> setYear(newNumber.intValue()));
    	discNumberProperty = new SimpleIntegerProperty(discNumber);
    	discNumberProperty.addListener((observable, oldNumber, newNumber) -> setDiscNumber(newNumber.intValue()));
    	bpmProperty = new SimpleIntegerProperty(bpm);
    	bpmProperty.addListener((observable, oldNumber, newNumber) -> setBpm(newNumber.intValue()));
    	dateModifiedProperty = new SimpleObjectProperty<>(dateModified);
    	dateModifiedProperty.addListener((observable, oldDate, newDate) -> setDateModified(newDate));
    	hasCoverProperty = new SimpleBooleanProperty(hasCover);
    	playCountProperty = new SimpleIntegerProperty(playCount);
    	
    	propertyMap = new HashMap<>();
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
    
    public boolean updateMetadata() throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
    	return updater.writeAudioMetadata();
    }
    
    public boolean updateCover(File coverFile) {
    	return updater.updateCover(coverFile);
    }
    
	public byte[] getCoverBytes() {
		byte[] coverBytes = null;
		try {
			AudioFile audioFile = AudioFileIO.read(new File(fileFolder+"/"+fileName));
			if(!audioFile.getTag().getArtworkList().isEmpty())
				coverBytes = audioFile.getTag().getFirstArtwork().getBinaryData();
		} catch (CannotReadException | IOException | TagException
				|ReadOnlyFileException | InvalidAudioFrameException e) {
		}
		return coverBytes;
	}
    
	public Map<TrackField,Property<?>> getPropertiesMap() {
    	return propertyMap;
    }
	
	public boolean isPlayable() {
		boolean playable = true;
		if(getInDisk()) {
			File file = new File(fileFolder+"/"+fileName);
			if(!file.exists()) {
				ErrorDemon.getInstance().showErrorDialog(fileFolder+"/"+fileName+" not found");
				setInDisk(false);
				playable = false;
			}
			else if(fileFormat.equals("flac") || encoding.startsWith("Apple") || encoder.startsWith("iTunes")) {
				Platform.runLater(() -> {
					if(fileFormat.equals("flac"))
						StageDemon.getInstance().getNavigationController().setStatusMessage("Can't play .flac files yet");
					else
						StageDemon.getInstance().getNavigationController().setStatusMessage("Can't play Apple's .m4a encoded files");
				});
				playable = false;
			}
		}
		else
			playable = false;
		return playable;
	}
    
    public int getTrackID() {
		return trackID;
	}

    public void setTrackID(int trackID) {
		this.trackID = trackID;
	}

	public String getFileFolder() {
		return fileFolder;
	}

	public void setFileFolder(String fileFolder) {
		this.fileFolder = fileFolder;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
		int pos = fileName.lastIndexOf(".");
		fileFormat = fileName.substring(pos + 1);
	}
	
	public String getFileFormat() {
		return fileFormat;
	}

	public StringProperty nameProperty() {
		return nameProperty;
	}

	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
		nameProperty.setValue(this.name);
	}

	public StringProperty artistProperty() {
		return artistProperty;
	}
	
	public String getArtist() {
		return this.artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
		artistProperty.setValue(this.artist);
	}

	public StringProperty albumProperty() {
		return albumProperty;
	}

	public String getAlbum() {
		return this.album;
	}
	
	public void setAlbum(String album) {
		this.album = album;
		albumProperty.setValue(this.album);
	}

	public StringProperty genreProperty() {
		return genreProperty;
	}
	
	public String getGenre() {
		return this.genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
		genreProperty.setValue(this.genre);
	}

	public StringProperty commentsProperty() {
		return commentsProperty;
	}

	public String getComments() {
		return this.comments;
	}
	
	public void setComments(String comments) {
		this.comments = comments;
		commentsProperty.setValue(this.comments);
	}

	public StringProperty albumArtistProperty() {
		return albumArtistProperty;
	}

	public String getAlbumArtist() {
		return this.albumArtist;
	}
	
	public void setAlbumArtist(String albumArtist) {
		this.albumArtist = albumArtist;
		albumArtistProperty.setValue(this.albumArtist);
	}

	public StringProperty labelProperty() {
		return labelProperty;
	}
	
	public String getLabel() {
		return this.label;
	}

	public void setLabel(String label) {
		this.label = label;
		labelProperty.setValue(this.label);
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public void setEncoder(String encoder) {
		this.encoder = encoder;
	}
	
	public String getEncoder() {
		return this.encoder;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public Duration getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(Duration totalTime) {
		this.totalTime = totalTime;
	}

	public IntegerProperty trackNumberProperty() {
		return trackNumberProperty;
	}
	
	public int getTrackNumber() {
		return this.trackNumber;
	}

	public void setTrackNumber(int trackNumber) {
		this.trackNumber = trackNumber;
		trackNumberProperty.setValue(this.trackNumber);
	}

	public IntegerProperty yearProperty() {
		return yearProperty;
	}
	
	public int getYear() {
		return this.year;
	}

	public void setYear(int year) {
		this.year = year;
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
		this.playCountProperty.setValue(playCount);
	}
	
	public IntegerProperty playCountProperty() {
		return this.playCountProperty;
	}

	public void incrementPlayCount() {
		this.playCount++;
		this.playCountProperty.setValue(playCount);
	}

	public IntegerProperty discNumberProperty() {
		return discNumberProperty;
	}
	
	public int getDiscNumber() {
		return this.discNumber;
	}

	public void setDiscNumber(int discNumber) {
		this.discNumber = discNumber;
		discNumberProperty.setValue(this.discNumber);
	}

	public IntegerProperty bpmProperty() {
		return bpmProperty;
	}
	
	public int getBpm() {
		return this.bpm;
	}

	public void setBpm(int bpm) {
		this.bpm = bpm;
		bpmProperty.setValue(this.bpm);
	}
	
	public boolean getInDisk() {
		return inDisk;
	}

	public void setInDisk(boolean inDisk) {
		this.inDisk = inDisk;
	}
	
	public boolean getIsCompilation() {
		return this.isCompilation;
	}
	
	public void setCompilation(boolean isCompilation) {
		this.isCompilation = isCompilation;
	}
	
	public ObjectProperty<LocalDateTime> dateModifiedProperty() {
		return dateModifiedProperty;
	}
	
	public LocalDateTime getDateModified() {
		return dateModified;
	}

	public void setDateModified(LocalDateTime dateModified) {
		this.dateModified = dateModified;
		dateModifiedProperty.set(dateModified);
	}

	public LocalDateTime getDateAdded() {
		return dateAdded;
	}

	public void setDateAdded(LocalDateTime dateAdded) {
		this.dateAdded = dateAdded;
	}

	public BooleanProperty hasCoverProperty() {
		return hasCoverProperty;
	}
	
	public boolean hasCover() {
		return this.hasCover;
	}
	
	public void setHasCover(boolean hasCover) {
		this.hasCover = hasCover;
		hasCoverProperty.set(hasCover);
	}
	
	public boolean isVariableBitRate() {
		return this.isVariableBitRate;
	}
	
	public void setIsVariableBitRate(boolean isVariableBitRate) {
		this.isVariableBitRate = isVariableBitRate;
	}
	
	public MetadataUpdater getUpdater() {
		return updater;
	}

	public void setUpdater(MetadataUpdater updater) {
		this.updater = updater;
	}

	@Override
	public int hashCode() {
		int hash=71;
		hash = 73*hash+fileName.hashCode();
		hash = 73*hash+fileFolder.hashCode();
		hash = 73*hash+name.hashCode();
		hash = 73*hash+artist.hashCode();
		hash = 73*hash+album.hashCode();
		hash = 73*hash+comments.hashCode();
		hash = 73*hash+genre.hashCode();
		hash = 73*hash+trackNumber;
		hash = 73*hash+year;
		hash = 73*hash+albumArtist.hashCode();
		hash = 73*hash+bpm;
		hash = 73*hash+label.hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(Object o) {
		boolean equals = false;
		if((o instanceof Track && ((Track) o).getFileName().equalsIgnoreCase(this.fileName)) &&
			o instanceof Track && ((Track) o).getFileFolder().equalsIgnoreCase(this.fileFolder) && 
			o instanceof Track && ((Track) o).getName().equalsIgnoreCase(this.name) &&
			o instanceof Track && ((Track) o).getArtist().equalsIgnoreCase(this.artist) &&
			o instanceof Track && ((Track) o).getAlbum().equalsIgnoreCase(this.album) &&
			o instanceof Track && ((Track) o).getComments().equalsIgnoreCase(this.comments) &&
			o instanceof Track && ((Track) o).getGenre().equalsIgnoreCase(this.genre) &&
			o instanceof Track && ((Track) o).getTrackNumber() == this.trackNumber &&
			o instanceof Track && ((Track) o).getYear() == this.year &&
			o instanceof Track && ((Track) o).getAlbumArtist().equalsIgnoreCase(this.albumArtist) &&
			o instanceof Track && ((Track) o).getBpm() == this.bpm &&
			o instanceof Track && ((Track) o).getLabel().equalsIgnoreCase(this.label))
				equals = true;
		return equals;
	}
	
	@Override
    public String toString(){
    	return "["+name+"|"+artist+"|"+genre+"|"+album+"("+year+")|"+bpm+"|"+label+"]";
    }
}
