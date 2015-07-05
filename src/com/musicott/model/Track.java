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

	private int size;
	private int totalTime;
	private int trackNumber;
	private int year;
	private int bitRate;
	private int playCount;
	private int discNumber;
	private int BPM;
	
	private boolean hasCover;
	private boolean isInDisk;
	private boolean isCompilation;
	
	private LocalDate dateModified;
	private LocalDate dateAdded;
	
	public Track () {
		trackID = -1;
		fileFolder = "";
		fileName = "";
		name = "";
		artist = "";
		album = "";
		genre = "";
		comments = "";
		albumArtist = "";
		label = "";
		size = -1;
		totalTime = -1;
		trackNumber = 0;
		year = 0;
		bitRate = -1;
		playCount = 0;
		discNumber = 0;
		BPM = -1;
		hasCover = false;
		isInDisk = false;
		isCompilation = false;
		dateModified = LocalDate.now();
		dateAdded = LocalDate.now();
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getAlbumArtist() {
		return albumArtist;
	}

	public void setAlbumArtist(String albumArtist) {
		this.albumArtist = albumArtist;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
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

	public int getTrackNumber() {
		return trackNumber;
	}

	public void setTrackNumber(int trackNumber) {
		this.trackNumber = trackNumber;
	}

	public int getYear() {
		return year;
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

	public int getDiscNumber() {
		return discNumber;
	}

	public void setDiscNumber(int discNumber) {
		this.discNumber = discNumber;
	}

	public int getBPM() {
		return BPM;
	}

	public void setBPM(int bPM) {
		BPM = bPM;
	}

	public boolean isHasCover() {
		return hasCover;
	}

	public void setHasCover(boolean hasCover) {
		this.hasCover = hasCover;
	}

	public boolean isInDisk() {
		return isInDisk;
	}

	public void setInDisk(boolean isInDisk) {
		this.isInDisk = isInDisk;
	}

	public boolean isCompilation() {
		return isCompilation;
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
	
	public static Track trackFromObservableTrack(ObservableTrack obsTrack) {
		Track t = new Track();
		t.trackID = obsTrack.getTrackID();
		t.fileFolder = obsTrack.getFileFolder();
		t.fileName = obsTrack.getFileName();
		t.name = obsTrack.getName().get();
		t.artist = obsTrack.getArtist().get();
		t.album = obsTrack.getAlbum().get();
		t.genre = obsTrack.getGenre().get();
		t.comments = obsTrack.getComments().get();
		t.albumArtist = obsTrack.getAlbumArtist().get();
		t.label = obsTrack.getLabel().get();
		t.size = obsTrack.getSize().get();
		t.totalTime = obsTrack.getTotalTime().get();
		t.trackNumber = obsTrack.getTrackNumber().get();
		t.year = obsTrack.getYear().get();
		t.bitRate = obsTrack.getBitRate().get();
		t.playCount = obsTrack.getPlayCount().get();
		t.discNumber = obsTrack.getDiscNumber().get();
		t.BPM = obsTrack.getBPM().get();
		t.hasCover = obsTrack.getHasCover().get();
		t.isInDisk = obsTrack.getIsInDisk().get();
		t.isCompilation = obsTrack.getIsCompilation().get();
		t.dateModified = obsTrack.getDateModified().get();
		t.dateAdded = obsTrack.getDateAdded().get();
		return t;
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
		hash = 73*hash+BPM;
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
			o instanceof Track && ((Track) o).getBPM() == this.BPM &&
			o instanceof Track && ((Track) o).getLabel().equalsIgnoreCase(this.label) &&
			o instanceof Track && ((Track) o).getTotalTime() == this.totalTime)
				equals = true;
		return equals;
	}
	
	@Override
    public String toString(){
    	return name+" "+artist+" "+genre+" "+album+"("+year+") "+BPM+" "+label+" "+totalTime;
    }
}