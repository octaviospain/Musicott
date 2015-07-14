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
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.musicott.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * @author Octavio Calleya
 *
 */
public class Track {
	
	private int trackID;
	private String fileFolder;
	private String fileName;
	private String coverFileName;
	private String name;
	private String artist;
	private String album;
	private String genre;
	private String comments;
	private String albumArtist;
	private String label;

	private int size;
	private int totalTime;
	private int bitRate;
	private int playCount;
	private int trackNumber;
	private int discNumber;
	private int year;
	private int bpm;

	private boolean hasCover;
	private boolean isInDisk;
	private boolean isCompilation;

	private LocalDate dateModified;
	private LocalDate dateAdded;
	
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
	
	
	private Map<TrackField,Property<?>> propertyMap;
	
    public Track() {
    	trackID = -1;
    	fileFolder = "";
    	fileName = "";
    	coverFileName = "";
    	name = "";
    	artist = "";
    	album = "";
    	genre = "";
    	comments = "";
    	albumArtist = "";
    	label = "";
    	trackNumber = 0;
    	discNumber = 0;
    	year = 0;
    	bpm = -1;
    	size = -1;
    	totalTime = -1;
    	bitRate = -1;
    	playCount = 0;
    	hasCover = false;
    	isInDisk = false;
    	isCompilation = false;
    	dateModified = LocalDate.now();
    	dateAdded = LocalDate.now();
    	
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
    	
    	propertyMap = new HashMap<TrackField,Property<?>>();
    	propertyMap.put(TrackField.NAME, nameProperty);
    	propertyMap.put(TrackField.ARTIST, artistProperty);
    	propertyMap.put(TrackField.ALBUM, albumProperty);
    	propertyMap.put(TrackField.GENRE, genreProperty);
    	propertyMap.put(TrackField.COMMENTS, commentsProperty);
    	propertyMap.put(TrackField.ALBUM_ARTIST, albumArtistProperty);
    	propertyMap.put(TrackField.LABEL, labelProperty);
    	propertyMap.put(TrackField.TRACK_NUMBER, trackNumberProperty);
    	propertyMap.put(TrackField.DISC_NUMBER, discNumberProperty);
    	propertyMap.put(TrackField.YEAR, yearProperty);
    	propertyMap.put(TrackField.BPM, bpmProperty);
    }
    
    public Map<TrackField,Property<?>> getPropertiesMap() {
    	return propertyMap;
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
	}

	public StringProperty getNameProperty() {
		return nameProperty;
	}

	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
		nameProperty.setValue(this.name);
	}

	public StringProperty getArtistProperty() {
		return artistProperty;
	}
	
	public String getArtist() {
		return this.artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
		artistProperty.setValue(this.artist);
	}

	public StringProperty getAlbumProperty() {
		return albumProperty;
	}

	public String getAlbum() {
		return this.album;
	}
	
	public void setAlbum(String album) {
		this.album = album;
		albumProperty.setValue(this.album);
	}

	public StringProperty getGenreProperty() {
		return genreProperty;
	}
	
	public String getGenre() {
		return this.genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
		genreProperty.setValue(this.genre);
	}

	public StringProperty getCommentsProperty() {
		return commentsProperty;
	}

	public String getComments() {
		return this.comments;
	}
	
	public void setComments(String comments) {
		this.comments = comments;
		commentsProperty.setValue(this.comments);
	}

	public StringProperty getAlbumArtistProperty() {
		return albumArtistProperty;
	}

	public String getAlbumArtist() {
		return this.albumArtist;
	}
	
	public void setAlbumArtist(String albumArtist) {
		this.albumArtist = albumArtist;
		albumArtistProperty.setValue(this.albumArtist);
	}

	public StringProperty getLabelProperty() {
		return labelProperty;
	}
	
	public String getLabel() {
		return this.label;
	}

	public void setLabel(String label) {
		this.label = label;
		labelProperty.setValue(this.label);
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(int totalTime) {
		this.totalTime = totalTime;
	}

	public IntegerProperty getTrackNumberProperty() {
		return trackNumberProperty;
	}
	
	public int getTrackNumber() {
		return this.trackNumber;
	}

	public void setTrackNumber(int trackNumber) {
		this.trackNumber = trackNumber;
		trackNumberProperty.setValue(this.trackNumber);
	}

	public IntegerProperty getYearProperty() {
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
	}

	public IntegerProperty getDiscNumberProperty() {
		return discNumberProperty;
	}
	
	public int getDiscNumber() {
		return this.discNumber;
	}

	public void setDiscNumber(int discNumber) {
		this.discNumber = discNumber;
		discNumberProperty.setValue(this.discNumber);
	}

	public IntegerProperty getBpmProperty() {
		return bpmProperty;
	}
	
	public int getBpm() {
		return this.bpm;
	}

	public void setBpm(int bpm) {
		this.bpm = bpm;
		bpmProperty.setValue(this.bpm);
	}

	public boolean getHasCover() {
		return hasCover;
	}

	public void setHasCover(boolean hasCover) {
		this.hasCover = hasCover;
	}

	public boolean getIsInDisk() {
		return isInDisk;
	}

	public void setInDisk(boolean isInDisk) {
		this.isInDisk = isInDisk;
	}
	
	public boolean getIsCompilation() {
		return this.isCompilation;
	}
	
	public void setCompilation(boolean isCompilation) {
		this.isCompilation = isCompilation;
	}
	
	public LocalDate getDateModified() {
		return dateModified;
	}

	public void setDateModified(LocalDate dateModified) {
		this.dateModified = dateModified;
	}

	public LocalDate getDateAdded() {
		return dateAdded;
	}

	public void setDateAdded(LocalDate dateAdded) {
		this.dateAdded = dateAdded;
	}
	
	public String getCoverFileName() {
		return coverFileName;
	}

	public void setCoverFileName(String coverFileName) {
		this.coverFileName = coverFileName;
	}
	
	@Override
	public int hashCode() {
		int hash=71;
		hash = 73*hash+fileName.hashCode();
		hash = 73*hash+fileFolder.hashCode();
		hash = 73*hash+size;
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
		hash = 73*hash+totalTime;
		return hash;
	}
	
	@Override
	public boolean equals(Object o) {
		boolean equals = false;
		if((o instanceof Track && ((Track) o).getFileName().equalsIgnoreCase(this.fileName)) &&
			o instanceof Track && ((Track) o).getFileFolder().equalsIgnoreCase(this.fileFolder) && 
			o instanceof Track && ((Track) o).getSize() == this.size &&
			o instanceof Track && ((Track) o).getName().equalsIgnoreCase(this.name) &&
			o instanceof Track && ((Track) o).getArtist().equalsIgnoreCase(this.artist) &&
			o instanceof Track && ((Track) o).getAlbum().equalsIgnoreCase(this.album) &&
			o instanceof Track && ((Track) o).getComments().equalsIgnoreCase(this.comments) &&
			o instanceof Track && ((Track) o).getGenre().equalsIgnoreCase(this.genre) &&
			o instanceof Track && ((Track) o).getTrackNumber() == this.trackNumber &&
			o instanceof Track && ((Track) o).getYear() == this.year &&
			o instanceof Track && ((Track) o).getAlbumArtist().equalsIgnoreCase(this.albumArtist) &&
			o instanceof Track && ((Track) o).getBpm() == this.bpm &&
			o instanceof Track && ((Track) o).getLabel().equalsIgnoreCase(this.label) &&
			o instanceof Track && ((Track) o).getTotalTime() == this.totalTime)
				equals = true;
		return equals;
	}
	
	@Override
    public String toString(){
    	return name+" "+artist+" "+genre+" "+album+"("+year+") "+bpm+" "+label+" "+totalTime;
    }
}