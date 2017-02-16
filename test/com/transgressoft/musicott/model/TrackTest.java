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

import com.transgressoft.musicott.*;
import javafx.beans.property.*;
import javafx.util.Duration;
import org.junit.*;
import org.junit.runner.*;
import org.powermock.api.mockito.*;
import org.powermock.core.classloader.annotations.*;
import org.powermock.modules.junit4.*;

import java.io.*;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.*;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author Octavio Calleya
 */
@RunWith (PowerMockRunner.class)
@PrepareForTest (ErrorDemon.class)
public class TrackTest {

    private Track track;
    private File testCover = new File("/test-resources/testfiles/testcover.jpg");

    private int trackId = 55;
    private String fileFormat = "mp3";
    private String fileName = "File Name" + "." + "mp3";
    private String fileFolder = "File Fodler";
    private String name = "Name";
    private String artist = "Artist";
    private String album = "Album";
    private String comments = "Comments";
    private String genre = "Genre";
    private int trackNumber = 5;
    private int discNumber = 4;
    private int year = 2016;
    private String albumArtist = "Album Artist";
    private int bpm = 128;
    private int bitRate = 320;
    private int playCount = 3;
    private int size = 2048;
    private String label = "Label";
    private String encoding = "Encoding";
    private String encoder = "Encoder";
    private Duration totalTime = Duration.seconds(40);

    @Before
    public void beforeEachTest() throws Exception {
        MainPreferences.getInstance().setMusicottUserFolder(".");

        PowerMockito.mockStatic(ErrorDemon.class);
        ErrorDemon errorDemonMock = mock(ErrorDemon.class);
        when(ErrorDemon.getInstance()).thenReturn(errorDemonMock);
        PowerMockito.doNothing().when(errorDemonMock, "showErrorDialog", any());
    }

    @After
    public void afterEachTest() {
        verifyStatic();
    }

    @Test
    public void constructorTest() {
        track = new Track();
        assertEquals(1, track.getTrackId());
        assertEquals("", track.getFileFolder());
        assertEquals("", track.getFileName());
        assertEquals("", track.getFileFormat());
        assertEquals("", track.getName());
        assertEquals("", track.getArtist());
        assertEquals("", track.getAlbum());
        assertEquals("", track.getGenre());
        assertEquals("", track.getComments());
        assertEquals("", track.getAlbumArtist());
        assertEquals("", track.getLabel());
        assertEquals("", track.getEncoder());
        assertEquals("", track.getEncoding());
        assertEquals(0, track.getTrackNumber());
        assertEquals(0, track.getDiscNumber());
        assertEquals(0, track.getYear());
        assertEquals(0, track.getBpm());
        assertEquals(Duration.UNKNOWN, track.getTotalTime());
        assertEquals(0, track.getBitRate());
        assertEquals(0, track.getPlayCount());
        assertEquals(false, track.getInDisk());
        assertEquals(false, track.isPartOfCompilation());
        assertEquals(false, track.isVariableBitRate());
        assertTrue(LocalDateTime.now().isAfter(track.getLastDateModified()));
        assertTrue(LocalDateTime.now().isAfter(track.getDateAdded()));
        assertFalse(track.getCoverImage().isPresent());
    }

    @Test
    public void propertiesTest() {
        track = new Track();
        assertEquals("", track.nameProperty().get());
        assertEquals("", track.artistProperty().get());
        assertEquals("", track.albumProperty().get());
        assertEquals("", track.genreProperty().get());
        assertEquals("", track.commentsProperty().get());
        assertEquals("", track.albumArtistProperty().get());
        assertEquals("", track.labelProperty().get());
        assertEquals(0, track.trackNumberProperty().get());
        assertEquals(0, track.yearProperty().get());
        assertEquals(0, track.discNumberProperty().get());
        assertEquals(0, track.bpmProperty().get());
        assertEquals(0, track.playCountProperty().get());
        assertTrue(track.lastDateModifiedProperty().get().isBefore(LocalDateTime.now()));
        assertEquals(false, track.isPlayableProperty().get());
        assertEquals(false, track.hasCoverProperty().get());

        Map<TrackField, Property> propertyMap = track.getPropertyMap();
        assertEquals("", propertyMap.get(TrackField.NAME).getValue());
        assertEquals("", propertyMap.get(TrackField.ALBUM).getValue());
        assertEquals("", propertyMap.get(TrackField.ALBUM_ARTIST).getValue());
        assertEquals("", propertyMap.get(TrackField.ARTIST).getValue());
        assertEquals("", propertyMap.get(TrackField.GENRE).getValue());
        assertEquals("", propertyMap.get(TrackField.COMMENTS).getValue());
        assertEquals("", propertyMap.get(TrackField.LABEL).getValue());
        assertEquals(0, propertyMap.get(TrackField.TRACK_NUMBER).getValue());;
        assertEquals(0, propertyMap.get(TrackField.YEAR).getValue());;
        assertEquals(0, propertyMap.get(TrackField.BPM).getValue());
    }

    @Test
    public void settersTest() {
        track = new Track();
        track.setTrackId(trackId);
        track.setName(name);
        track.setAlbum(album);
        track.setArtist(artist);
        track.setAlbumArtist(albumArtist);
        track.setLabel(label);
        track.setGenre(genre);
        track.setComments(comments);
        track.setBpm(bpm);
        track.setYear(year);
        track.setEncoding(encoding);
        track.setEncoder(encoder);
        track.setFileFolder(fileFolder);
        track.setFileName(fileName);
        track.setTotalTime(totalTime);
        track.setDiscNumber(discNumber);
        track.setTrackNumber(trackNumber);
        track.setBitRate(bitRate);
        track.setPlayCount(playCount);
        track.setSize(size);
        track.setIsPartOfCompilation(true);
        track.setIsVariableBitRate(true);
        track.setInDisk(true);
        track.setDateAdded(LocalDateTime.of(2006, 1, 1, 23, 59));
        track.setLastDateModified(LocalDateTime.of(2006, 1, 1, 23, 59));

        assertEquals(trackId, track.getTrackId());
        assertEquals(name, track.getName());
        assertEquals(album, track.getAlbum());
        assertEquals(artist, track.getArtist());
        assertEquals(albumArtist, track.getAlbumArtist());
        assertEquals(label, track.getLabel());
        assertEquals(genre, track.getGenre());
        assertEquals(comments, track.getComments());
        assertEquals(bpm, track.getBpm());
        assertEquals(year, track.getYear());
        assertEquals(encoding, track.getEncoding());
        assertEquals(encoder, track.getEncoder());
        assertEquals(fileFolder, track.getFileFolder());
        assertEquals(fileName, track.getFileName());
        assertEquals(fileFormat, track.getFileFormat());
        assertEquals(totalTime, track.getTotalTime());
        assertEquals(discNumber, track.getDiscNumber());
        assertEquals(trackNumber, track.getTrackNumber());
        assertEquals(bitRate, track.getBitRate());
        assertEquals(playCount, track.getPlayCount());
        assertEquals(size, track.getSize());
        assertTrue(track.isPartOfCompilation());
        assertTrue(track.isVariableBitRate());
        assertTrue(track.getInDisk());
        assertEquals(LocalDateTime.of(2006, 1, 1, 23, 59), track.getDateAdded());
        assertEquals(LocalDateTime.of(2006, 1, 1, 23, 59), track.getLastDateModified());
    }

    @Test
    public void writeMetadataTest() {
        
    }

    @Test
    public void hashCodeTest() {
        int hash = Objects.hash(fileName, fileFolder, name, artist, album, comments,
                                genre, trackNumber, year, albumArtist, bpm, label);

        track = new Track();
        track.setFileName(fileName);
        track.setFileFolder(fileFolder);
        track.setName(name);
        track.setArtist(artist);
        track.setAlbum(album);
        track.setComments(comments);
        track.setGenre(genre);
        track.setTrackNumber(trackNumber);
        track.setYear(year);
        track.setAlbumArtist(albumArtist);
        track.setBpm(bpm);
        track.setLabel(label);

        assertEquals(hash, track.hashCode());
    }

    @Test
    public void equalsTest() {
        track = new Track();
        track.setFileName(fileName);
        track.setFileFolder(fileFolder);
        track.setName(name);
        track.setArtist(artist);
        track.setAlbum(album);
        track.setComments(comments);
        track.setGenre(genre);
        track.setTrackNumber(trackNumber);
        track.setYear(year);
        track.setAlbumArtist(albumArtist);
        track.setBpm(bpm);
        track.setLabel(label);

        Track track2 = new Track();
        track2.setFileName(fileName);
        track2.setFileFolder(fileFolder);
        track2.setName(name);
        track2.setArtist(artist);
        track2.setAlbum(album);
        track2.setComments(comments);
        track2.setGenre(genre);
        track2.setTrackNumber(trackNumber);
        track2.setYear(year);
        track2.setAlbumArtist(albumArtist);
        track2.setBpm(bpm);
        track2.setLabel(label);

        assertTrue(track.equals(track2));
    }

    @Test
    public void notEqualsTest() {
        track = new Track();
        track.setFileName(fileName);
        track.setFileFolder(fileFolder);
        track.setName(name);
        track.setArtist(artist);
        track.setAlbum(album);
        track.setComments(comments);
        track.setGenre(genre);
        track.setTrackNumber(trackNumber);
        track.setYear(year);
        track.setAlbumArtist(albumArtist);
        track.setBpm(bpm);
        track.setLabel(label);

        Track track2 = new Track();

        assertFalse(track.equals(track2));
    }

    @Test
    public void toStringTest() {
        track = new Track();
        track.setName(name);
        track.setArtist(artist);
        track.setGenre(genre);
        track.setAlbum(album);
        track.setYear(year);
        track.setBpm(bpm);
        track.setLabel(label);

        String expectedString = name + "|" + artist + "|" + genre + "|" + album + "(" + year + ")|" + bpm + "|" + label;
        assertEquals(expectedString, track.toString());
    }

    @Test
    public void incrementPlayCountTest() {
        track = new Track();
        assertEquals(0, track.getPlayCount());
        track.incrementPlayCount();
        assertEquals(1, track.getPlayCount());
        assertEquals(1, track.playCountProperty().get());
    }

    @Test
    public void notPlayableIfNotExistsTest() {
        track = new Track();
        track.setInDisk(true);
        track.setFileFolder("./test-resources/testfiles/");
        track.setFileName("nonexistentfile.mp3");
        assertFalse(track.isPlayable());
    }

    @Test
    public void notPlayableIfNotInDiskTest() {
        track = new Track();
        track.setInDisk(false);
        assertFalse(track.isPlayable());
    }

    @Test
    public void notPlayableIfFlacTest() {
        track = new Track();
        track.setInDisk(true);
        track.setFileFolder("./test-resources/testfiles/");
        track.setFileName("testeable.flac");
        assertFalse(track.isPlayable());
    }

    @Test
    public void notPlayableIfAppleEncodingTest() {
        track = new Track();
        track.setInDisk(true);
        track.setEncoding("Apple");
        track.setFileFolder("./test-resources/testfiles/");
        track.setFileName("testeable.mp3");
        assertFalse(track.isPlayable());
    }

    @Test
    public void notPlayableIfEncoderItunesTest() {
        track = new Track();
        track.setInDisk(true);
        track.setEncoder("iTunes");
        track.setFileFolder("./test-resources/testfiles/");
        track.setFileName("testeable.mp3");
        assertFalse(track.isPlayable());
    }
}