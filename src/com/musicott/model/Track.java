package com.musicott.model;

import java.time.LocalDate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Track {
	
	private int trackID;
	private String fileFolder;
	private String fileName;
	
	private StringProperty name;
	private StringProperty artist;
	private StringProperty album;
	private StringProperty genre;
	private StringProperty comments;
	private StringProperty albumArtist;
	private StringProperty label;

	private IntegerProperty size;
	private IntegerProperty totalTime;
	private IntegerProperty trackNumber;
	private IntegerProperty year;
	private IntegerProperty bitRate;
	private IntegerProperty playCount;
	private IntegerProperty discNumber;
	private IntegerProperty BPM;
	
	private BooleanProperty hasM4aVersion;
	private BooleanProperty hasFlacVersion;
	private BooleanProperty hasWavVersion;
	private BooleanProperty hasCover;
	private BooleanProperty isInDisk;
	
	private ObjectProperty<LocalDate> dateModified;
	private ObjectProperty<LocalDate> dateAdded;
	
    public Track() {
    	trackID = -1;
    	name = new SimpleStringProperty();
    	name.set("");
    	artist = new SimpleStringProperty();
    	artist.set("");
    	album = new SimpleStringProperty();
    	album.set("");
    	genre = new SimpleStringProperty();
    	genre.set("");
    	comments = new SimpleStringProperty();
    	comments.set("");
    	albumArtist = new SimpleStringProperty();
    	albumArtist.set("");
    	label = new SimpleStringProperty();
    	label.set("");
    	size = new SimpleIntegerProperty();
    	size.set(-1);
    	totalTime = new SimpleIntegerProperty();
    	totalTime.set(-1);
    	trackNumber = new SimpleIntegerProperty();
    	trackNumber.set(-1);
    	year = new SimpleIntegerProperty();
    	year.set(-1);
    	bitRate = new SimpleIntegerProperty();
    	bitRate.set(-1);
    	playCount = new SimpleIntegerProperty();
    	playCount.set(0);
    	discNumber = new SimpleIntegerProperty();
    	discNumber.set(-1);
    	BPM = new SimpleIntegerProperty();
    	BPM.set(-1);
    	hasM4aVersion = new SimpleBooleanProperty();
    	hasFlacVersion = new SimpleBooleanProperty();
    	hasWavVersion = new SimpleBooleanProperty();
    	hasCover = new SimpleBooleanProperty();
    	isInDisk = new SimpleBooleanProperty();
    	dateModified = new SimpleObjectProperty<LocalDate>(LocalDate.now());
    	dateAdded = new SimpleObjectProperty<LocalDate>(LocalDate.now());
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

	public StringProperty getName() {
		return name;
	}

	public void setName(StringProperty name) {
		this.name = name;
	}

	public StringProperty getArtist() {
		return artist;
	}

	public void setArtist(StringProperty artist) {
		this.artist = artist;
	}

	public StringProperty getAlbum() {
		return album;
	}

	public void setAlbum(StringProperty album) {
		this.album = album;
	}

	public StringProperty getGenre() {
		return genre;
	}

	public void setGenre(StringProperty genre) {
		this.genre = genre;
	}

	public StringProperty getComments() {
		return comments;
	}

	public void setComments(StringProperty comments) {
		this.comments = comments;
	}

	public StringProperty getAlbumArtist() {
		return albumArtist;
	}

	public void setAlbumArtist(StringProperty albumArtist) {
		this.albumArtist = albumArtist;
	}

	public StringProperty getLabel() {
		return label;
	}

	public void setLabel(StringProperty label) {
		this.label = label;
	}

	public IntegerProperty getSize() {
		return size;
	}

	public void setSize(IntegerProperty size) {
		this.size = size;
	}

	public IntegerProperty getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(IntegerProperty totalTime) {
		this.totalTime = totalTime;
	}

	public IntegerProperty getTrackNumber() {
		return trackNumber;
	}

	public void setTrackNumber(IntegerProperty trackNumber) {
		this.trackNumber = trackNumber;
	}

	public IntegerProperty getYear() {
		return year;
	}

	public void setYear(IntegerProperty year) {
		this.year = year;
	}

	public IntegerProperty getBitRate() {
		return bitRate;
	}

	public void setBitRate(IntegerProperty bitRate) {
		this.bitRate = bitRate;
	}

	public IntegerProperty getPlayCount() {
		return playCount;
	}

	public void setPlayCount(IntegerProperty playCount) {
		this.playCount = playCount;
	}

	public IntegerProperty getDiscNumber() {
		return discNumber;
	}

	public void setDiscNumber(IntegerProperty discNumber) {
		this.discNumber = discNumber;
	}

	public IntegerProperty getBPM() {
		return BPM;
	}

	public void setBPM(IntegerProperty bPM) {
		BPM = bPM;
	}

	public BooleanProperty getHasM4aVersion() {
		return hasM4aVersion;
	}

	public void setHasM4aVersion(BooleanProperty hasM4aVersion) {
		this.hasM4aVersion = hasM4aVersion;
	}

	public BooleanProperty getHasFlacVersion() {
		return hasFlacVersion;
	}

	public void setHasFlacVersion(BooleanProperty hasFlacVersion) {
		this.hasFlacVersion = hasFlacVersion;
	}

	public BooleanProperty getHasWavVersion() {
		return hasWavVersion;
	}

	public void setHasWavVersion(BooleanProperty hasWavVersion) {
		this.hasWavVersion = hasWavVersion;
	}

	public BooleanProperty getHasCover() {
		return hasCover;
	}

	public void setHasCover(BooleanProperty hasCover) {
		this.hasCover = hasCover;
	}

	public BooleanProperty getIsInDisk() {
		return isInDisk;
	}

	public void setIsInDisk(BooleanProperty isInDisk) {
		this.isInDisk = isInDisk;
	}

	public ObjectProperty<LocalDate> getDateModified() {
		return dateModified;
	}

	public void setDateModified(ObjectProperty<LocalDate> dateModified) {
		this.dateModified = dateModified;
	}

	public ObjectProperty<LocalDate> getDateAdded() {
		return dateAdded;
	}

	public void setDateAdded(ObjectProperty<LocalDate> dateAdded) {
		this.dateAdded = dateAdded;
	}

	@Override
	public int hashCode() {
		int hash=71;
		hash = 73*hash+fileName.hashCode();
		hash = 73*hash+fileFolder.hashCode();
		hash = 73*hash+size.get();
		hash = 73*hash+name.get().hashCode();
		hash = 73*hash+artist.get().hashCode();
		hash = 73*hash+album.get().hashCode();
		hash = 73*hash+comments.get().hashCode();
		hash = 73*hash+genre.get().hashCode();
		hash = 73*hash+trackNumber.get();
		hash = 73*hash+year.get();
		hash = 73*hash+albumArtist.get().hashCode();
		hash = 73*hash+BPM.get();
		hash = 73*hash+label.get().hashCode();
		hash = 73*hash+totalTime.get();
		return hash;
	}
	
	@Override
	public boolean equals(Object o) {
		boolean equals = false;
		if((o instanceof Track && ((Track) o).getFileName().equalsIgnoreCase(this.fileName)) &&
			o instanceof Track && ((Track) o).getFileFolder().equalsIgnoreCase(this.fileFolder) && 
			o instanceof Track && ((Track) o).getSize().get() == this.size.get() &&
			o instanceof Track && ((Track) o).getName().get().equalsIgnoreCase(this.name.get()) &&
			o instanceof Track && ((Track) o).getArtist().get().equalsIgnoreCase(this.artist.get()) &&
			o instanceof Track && ((Track) o).getAlbum().get().equalsIgnoreCase(this.album.get()) &&
			o instanceof Track && ((Track) o).getComments().get().equalsIgnoreCase(this.comments.get()) &&
			o instanceof Track && ((Track) o).getGenre().get().equalsIgnoreCase(this.genre.get()) &&
			o instanceof Track && ((Track) o).getTrackNumber().get() == this.trackNumber.get() &&
			o instanceof Track && ((Track) o).getYear().get() == this.year.get() &&
			o instanceof Track && ((Track) o).getAlbumArtist().get().equalsIgnoreCase(this.albumArtist.get()) &&
			o instanceof Track && ((Track) o).getBPM().get() == this.BPM.get() &&
			o instanceof Track && ((Track) o).getLabel().get().equalsIgnoreCase(this.label.get()) &&
			o instanceof Track && ((Track) o).getTotalTime().get() == this.totalTime.get())
				equals = true;
		return equals;
	}
	
	@Override
    public String toString(){
    	return name.get()+" "+artist.get()+" "+genre.get()+" "+album.get()+"("+year.get()+") "+BPM.get()+" "+label.get()+" "+totalTime.get();
    }
}