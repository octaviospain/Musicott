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

import com.google.inject.*;
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.guice.modules.*;
import org.jaudiotagger.audio.*;
import org.jaudiotagger.audio.wav.*;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.flac.*;
import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.images.*;
import org.jaudiotagger.tag.mp4.*;
import org.jaudiotagger.tag.wav.*;
import org.junit.jupiter.api.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
public class MetadataParserTest {

    static Injector injector;

    String testFolder = "./test-resources/testfiles/";
    File testCover = new File(testFolder + "testcover.jpg");
    File mp3File = new File(testFolder + "testeable.mp3");
    File m4aFile = new File(testFolder + "testeable.m4a");
    File waveFile = new File(testFolder + "testeable.wav");
    File flacFile = new File(testFolder + "testeable.flac");
    File variableBitRateFile = new File(testFolder + "testeablevariableBitRate.mp3");

    String name = "Name";
    String artist = "Artist";
    String album = "Album";
    String comments = "Comments";
    String genre = "Genre";
    int trackNumber = 5;
    int discNumber = 4;
    int year = 2016;
    String albumArtist = "Album Artist";
    int bpm = 128;
    String label = "Label";
    boolean isPartOfCompilation = true;

    @BeforeAll
    public static void beforeAllTests() {
        injector = Guice.createInjector(binder -> {
            binder.install(new TrackFactoryModule());
            binder.bind(ErrorDemon.class).toInstance(mock(ErrorDemon.class));
            binder.bind(MainPreferences.class).toInstance(mock(MainPreferences.class));
            binder.requestStaticInjection(MetadataParser.class);
        });
    }

    @Test
    @DisplayName ("Invalid parse")
    void invalidParseTest() throws Exception {
        TrackParseException exception = assertThrows(
                TrackParseException.class, () -> MetadataParser.createTrack(new File(testFolder)));
        assertTrue(exception.getMessage().startsWith("Error parsing the file "));
    }

    @Test
    @DisplayName ("Variable bit rate test")
    void variableBitRateTest() throws Exception {
        prepareFile(variableBitRateFile);
        Track parsedTrack = MetadataParser.createTrack(variableBitRateFile);
        assertTrack(parsedTrack);
        assertTrue(parsedTrack.isVariableBitRate());
    }

    @Test
    @DisplayName ("Get audio tag nullable")
    void audioTagNullableTest() throws Exception {
        AudioFile audioFile = AudioFileIO.read(mp3File);
        audioFile.setTag(null);
        audioFile.commit();
        assertFalse(MetadataParser.getAudioTag(mp3File).isPresent());
    }

    @Test
    @DisplayName ("Cover bytes nullable")
    void coverBytesNullableTest() throws Exception {
        AudioFile audioFile = AudioFileIO.read(mp3File);
        Tag tag = new ID3v24Tag();
        tag.deleteArtworkField();
        tag.addField(ArtworkFactory.createArtworkFromFile(testCover));
        audioFile.setTag(tag);
        audioFile.commit();
        assertTrue(MetadataParser.getCoverBytes(tag).isPresent());
    }

    @Test
    @DisplayName ("mp3 parse")
    void mp3ParseTest() throws Exception {
        resetMp3File();
        prepareFile(mp3File);
        Track parsedTrack = MetadataParser.createTrack(mp3File);
        assertTrack(parsedTrack);
    }

    @Test
    @DisplayName ("M4a parse")
    void m4aParseTest() throws Exception {
        resetM4aFile();
        prepareFile(m4aFile);
        Track parsedTrack = MetadataParser.createTrack(m4aFile);
        assertTrack(parsedTrack);
    }

    @Test
    @DisplayName ("Wav parse")
    void wavParseTest() throws Exception {
        resetWavFile();
        prepareFile(waveFile);
        Track parsedTrack = MetadataParser.createTrack(waveFile);
        assertTrack(parsedTrack);
    }

    @Test
    @DisplayName ("Flac parse")
    void flacParseTest() throws Exception {
        resetFlacFile();
        prepareFile(flacFile);
        Track parsedTrack = MetadataParser.createTrack(flacFile);
        assertTrack(parsedTrack);
    }

    void assertTrack(Track parsedTrack) {
        assertEquals(name, parsedTrack.getName());
        assertEquals(album, parsedTrack.getAlbum());
        assertEquals(albumArtist, parsedTrack.getAlbumArtist());
        assertEquals(artist, parsedTrack.getArtist());
        assertEquals(genre, parsedTrack.getGenre());
        assertEquals(comments, parsedTrack.getComments());
        assertEquals(label, parsedTrack.getLabel());
        assertEquals(trackNumber, parsedTrack.getTrackNumber());
        assertEquals(discNumber, parsedTrack.getDiscNumber());
        assertEquals(year, parsedTrack.getYear());
        assertEquals(bpm, parsedTrack.getBpm());
        assertEquals(isPartOfCompilation, parsedTrack.isPartOfCompilation());
    }

    void resetMp3File() throws Exception {
        AudioFile audio = AudioFileIO.read(mp3File);
        Tag tag = new ID3v24Tag();
        resetCommonTagFields(tag);
        audio.setTag(tag);
        audio.commit();
    }

    void resetM4aFile() throws Exception {
        AudioFile audio = AudioFileIO.read(m4aFile);
        Tag tag = new Mp4Tag();
        resetCommonTagFields(tag);
        audio.setTag(tag);
        audio.commit();
    }

    void resetWavFile() throws Exception {
        AudioFile audio = AudioFileIO.read(waveFile);
        WavTag wavTag = new WavTag(WavOptions.READ_ID3_ONLY);
        wavTag.setID3Tag(new ID3v24Tag());
        wavTag.setInfoTag(new WavInfoTag());
        resetCommonTagFields(wavTag);
        audio.setTag(wavTag);
        audio.commit();
    }

    void resetFlacFile() throws Exception {
        AudioFile audio = AudioFileIO.read(flacFile);
        Tag tag = new FlacTag();
        resetCommonTagFields(tag);
        audio.setTag(tag);
        audio.commit();
    }

    void resetCommonTagFields(Tag tag) throws FieldDataInvalidException {
        setTagField(FieldKey.TITLE, "", tag);
        setTagField(FieldKey.ALBUM, "", tag);
        setTagField(FieldKey.ALBUM_ARTIST, "", tag);
        setTagField(FieldKey.ARTIST, "", tag);
        setTagField(FieldKey.GENRE, "", tag);
        setTagField(FieldKey.COMMENT, "", tag);
        setTagField(FieldKey.GROUPING, "", tag);
        setTagField(FieldKey.TRACK, Integer.toString(0), tag);
        setTagField(FieldKey.DISC_NO, Integer.toString(0), tag);
        setTagField(FieldKey.YEAR, Integer.toString(0), tag);
        setTagField(FieldKey.BPM, Integer.toString(0), tag);
        setTagField(FieldKey.IS_COMPILATION, Boolean.toString(false), tag);
    }

    void prepareFile(File file) throws Exception {
        AudioFile audio = AudioFileIO.read(file);
        Tag tag = audio.getTag();
        setCommonTagFields(tag);
        audio.commit();
    }

    void setCommonTagFields(Tag tag) throws FieldDataInvalidException {
        setTagField(FieldKey.TITLE, name, tag);
        setTagField(FieldKey.ALBUM, album, tag);
        setTagField(FieldKey.ALBUM_ARTIST, albumArtist, tag);
        setTagField(FieldKey.ARTIST, artist, tag);
        setTagField(FieldKey.GENRE, genre, tag);
        setTagField(FieldKey.COMMENT, comments, tag);
        setTagField(FieldKey.GROUPING, label, tag);
        setTagField(FieldKey.TRACK, Integer.toString(trackNumber), tag);
        setTagField(FieldKey.DISC_NO, Integer.toString(discNumber), tag);
        setTagField(FieldKey.YEAR, Integer.toString(year), tag);
        setTagField(FieldKey.BPM, Integer.toString(bpm), tag);
        setTagField(FieldKey.IS_COMPILATION, Boolean.toString(isPartOfCompilation), tag);
    }

    void setTagField(FieldKey fieldKey, String value, Tag tag) throws FieldDataInvalidException {
        tag.setField(fieldKey, value);
    }
}
