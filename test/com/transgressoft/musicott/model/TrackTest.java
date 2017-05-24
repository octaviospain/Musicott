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

import com.google.common.collect.*;
import com.google.inject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.util.guice.factories.*;
import com.transgressoft.musicott.util.guice.modules.*;
import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.util.Duration;
import org.apache.commons.lang3.text.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.time.*;
import java.util.*;

import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
public class TrackTest {

    static TrackFactory trackFactory;

    Track track;
    File testCover = new File("/test-resources/testfiles/testcover.jpg");
    int trackId = 55;
    String fileFormat = "mp3";
    String fileName = "File Name" + "." + fileFormat;
    String fileFolder = "./test-resources/testfiles/";
    String name = "Name";
    String artist = "artist name";
    String album = "Album";
    String comments = "Comments";
    String genre = "Genre";
    int trackNumber = 5;
    int discNumber = 4;
    int year = 2016;
    String albumArtist = "album artist";
    int bpm = 128;
    int bitRate = 320;
    int playCount = 3;
    int size = 2048;
    String label = "Label";
    String encoding = "Encoding";
    String encoder = "Encoder";
    Duration totalTime = Duration.seconds(40);

    @BeforeAll
    public static void beforeAll() throws Exception {
        Injector injector = Guice.createInjector(new TestModule());
        trackFactory = injector.getInstance(TrackFactory.class);
    }

    @Test
    @DisplayName ("Constructor")
    void constructorTest() {
        track = trackFactory.create("", "");
        assertEquals(0, track.getTrackId());
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
        assertEquals(false, track.isInDisk());
        assertEquals(false, track.isPartOfCompilation());
        assertEquals(false, track.isVariableBitRate());
        await().untilAsserted(() -> assertTrue(track.lastDateModifiedProperty().get().isBefore(LocalDateTime.now())));
        await().untilAsserted(() -> assertTrue(track.getDateAdded().isBefore(LocalDateTime.now())));
        assertFalse(track.getCoverImage().isPresent());
    }

    @Test
    @DisplayName ("Properties")
    void propertiesTest() throws Exception {
        track = trackFactory.create("", "");
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
        assertEquals(true, track.isPlayableProperty().get());
        assertEquals(false, track.hasCoverProperty().get());

        Map<TrackField, Property> propertyMap = track.getPropertyMap();
        assertEquals("", propertyMap.get(TrackField.NAME).getValue());
        assertEquals("", propertyMap.get(TrackField.ALBUM).getValue());
        assertEquals("", propertyMap.get(TrackField.ALBUM_ARTIST).getValue());
        assertEquals("", propertyMap.get(TrackField.ARTIST).getValue());
        assertEquals("", propertyMap.get(TrackField.GENRE).getValue());
        assertEquals("", propertyMap.get(TrackField.COMMENTS).getValue());
        assertEquals("", propertyMap.get(TrackField.LABEL).getValue());
        assertEquals(0, propertyMap.get(TrackField.TRACK_NUMBER).getValue());
        assertEquals(0, propertyMap.get(TrackField.YEAR).getValue());
        assertEquals(0, propertyMap.get(TrackField.BPM).getValue());
        await().untilAsserted(() -> assertTrue(track.lastDateModifiedProperty().get().isBefore(LocalDateTime.now())));
    }

    @Test
    @DisplayName ("Setters")
    void settersTest() {
        track = trackFactory.create(fileFolder, fileName);
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
        track.setTotalTime(totalTime);
        track.setDiscNumber(discNumber);
        track.setTrackNumber(trackNumber);
        track.setBitRate(bitRate);
        track.setPlayCount(playCount);
        track.setSize(size);
        track.setIsPartOfCompilation(true);
        track.setIsVariableBitRate(true);
        track.setIsInDisk(true);
        track.setDateAdded(LocalDateTime.of(2006, 1, 1, 23, 59));
        track.setLastDateModified(LocalDateTime.of(2006, 1, 1, 23, 59));
        track.setArtistsInvolved(FXCollections.observableSet(Utils.getArtistsInvolvedInTrack(track)));

        assertEquals(trackId, track.getTrackId());
        assertEquals(name, track.getName());
        assertEquals(album, track.getAlbum());
        assertEquals(WordUtils.capitalize(artist), track.getArtist());
        assertEquals(WordUtils.capitalize(albumArtist), track.getAlbumArtist());
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
        assertTrue(track.isInDisk());
        assertEquals(LocalDateTime.of(2006, 1, 1, 23, 59), track.getDateAdded());
        assertEquals(LocalDateTime.of(2006, 1, 1, 23, 59), track.getLastDateModified());
        assertEquals(Sets.newHashSet(WordUtils.capitalize(artist), WordUtils.capitalize(albumArtist)),
                     track.getArtistsInvolved());
    }

    @Test
    @DisplayName ("WriteMetadata")
    void writeMetadataTest() {
        // TODO
    }

    @Test
    @DisplayName ("HashCode")
    void hashCodeTest() {
        int hash = Objects.hash(fileName, fileFolder);

        track = trackFactory.create(fileFolder, fileName);
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
    @DisplayName ("Equals")
    void equalsTest() {
        track = trackFactory.create(fileFolder, fileName);
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

        Track track2 = trackFactory.create(fileFolder, fileName);
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
    @DisplayName ("Not Equals")
    void notEqualsTest() {
        track = trackFactory.create(fileName, fileFolder);
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

        Track track2 = trackFactory.create("", "");

        assertFalse(track.equals(track2));
    }

    @Test
    void toStringTest() {
        track = trackFactory.create("", "");
        track.setName(name);
        track.setArtist(artist);
        track.setGenre(genre);
        track.setAlbum(album);
        track.setYear(year);
        track.setBpm(bpm);
        track.setLabel(label);

        String expectedString = name + "|" + WordUtils
                .capitalize(artist) + "|" + genre + "|" + album + "(" + year + ")|" + bpm + "|" + label;
        assertEquals(expectedString, track.toString());
    }

    @Test
    @DisplayName ("Increment play count")
    void incrementPlayCountTest() {
        track = trackFactory.create("", "");
        assertEquals(0, track.getPlayCount());
        track.incrementPlayCount();
        assertEquals(1, track.getPlayCount());
        assertEquals(1, track.playCountProperty().get());
    }

    @Test
    @DisplayName ("Not Playable not exists")
    void notPlayableIfNotExistsTest() {
        track = trackFactory.create(fileFolder, "nonexistentfile.mp3");
        track.setIsInDisk(true);
        IOException exception = assertThrows(IOException.class, () -> track.isPlayable());
        assertEquals(exception.getMessage(), "File not found: " + fileFolder + "/nonexistentfile.mp3");
    }

    @Test
    @DisplayName ("Not playable not in desk")
    void notPlayableIfNotInDiskTest() throws Exception {
        track = trackFactory.create("", "");
        track.setIsInDisk(false);
        assertFalse(track.isPlayable());
    }

    @Test
    @DisplayName ("Not playable is flac")
    void notPlayableIfFlacTest() throws Exception {
        track = trackFactory.create(fileFolder, "testeable.flac");
        track.setIsInDisk(true);
        assertFalse(track.isPlayable());
    }

    @Test
    @DisplayName ("Not playable Apple encoding")
    void notPlayableIfAppleEncodingTest() throws Exception {
        track = trackFactory.create(fileFolder, "testeable.mp3");
        track.setIsInDisk(true);
        track.setEncoding("Apple");
        assertFalse(track.isPlayable());
    }

    @Test
    @DisplayName ("Not playable Itunes encoder")
    void notPlayableIfEncoderItunesTest() throws Exception {
        track = trackFactory.create(fileFolder, "testeable.mp3");
        track.setIsInDisk(true);
        track.setEncoder("iTunes");
        assertFalse(track.isPlayable());
    }

    private static class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            install(new TrackFactoryModule());
            MainPreferences preferences = mock(MainPreferences.class);
            when(preferences.getTrackSequence()).thenReturn(0);
            when(preferences.getMusicottUserFolder()).thenReturn(".");
            bind(MainPreferences.class).toInstance(preferences);
        }

        @Provides
        ChangeListener<Number> providesPlayCountListener() {
            return (a, b, c) -> {};
        }
    }
}
