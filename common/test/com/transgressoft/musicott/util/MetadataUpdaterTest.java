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

import com.transgressoft.musicott.model.*;
import javafx.beans.property.*;
import org.jaudiotagger.audio.*;
import org.jaudiotagger.audio.wav.*;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.images.*;
import org.jaudiotagger.tag.wav.*;
import org.junit.jupiter.api.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
public class MetadataUpdaterTest {

    static File mp3File;
    static File wavFile;
    static File testCover;

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
    Track testedTrack = prepareTrack();

    @BeforeAll
    public static void beforeAllTests() throws Exception {
        mp3File = new File(MetadataUpdaterTest.class.getResource("/testfiles/testeable.mp3").toURI());
        wavFile = new File(MetadataUpdaterTest.class.getResource("/testfiles/testeable.wav").toURI());
        testCover = new File(MetadataParser.class.getResource("/testfiles/cover.jpg").toURI());
    }

    @Test
    @DisplayName ("Write audio metadata")
    void writeAudioMetadataTest() throws Exception {
        resetMp3File();
        when(testedTrack.getFileName()).thenReturn("testeable.mp3");
        MetadataUpdater updater = new MetadataUpdater();
        updater.setTrack(testedTrack);
        updater.writeAudioMetadata();
        assertFile(mp3File);
    }

    @Test
    @DisplayName ("Write wav audio metadata")
    void writeWavAudioMetadataTest() throws Exception {
        resetWavFile();
        when(testedTrack.getFileName()).thenReturn("testeable.wav");
        MetadataUpdater updater = new MetadataUpdater();
        updater.setTrack(testedTrack);
        updater.writeAudioMetadata();
        assertFile(wavFile);
    }

    @Test
    @DisplayName("Update cover")
    void updateCoverTest() throws Exception {
        resetMp3File();
        BooleanProperty coverProperty = new SimpleBooleanProperty(false);
        when(testedTrack.hasCoverProperty()).thenReturn(coverProperty);
        when(testedTrack.getFileName()).thenReturn("testeable.mp3");
        MetadataUpdater updater = new MetadataUpdater();
        updater.setTrack(testedTrack);
        updater.updateCover(testCover);

        Artwork expectedCover = ArtworkFactory.createArtworkFromFile(testCover);
        AudioFile audioFile = AudioFileIO.read(mp3File);
        Artwork coverOnFile = audioFile.getTag().getFirstArtwork();

        assertEquals(expectedCover.getHeight(), coverOnFile.getHeight());
        assertEquals(expectedCover.getWidth(), coverOnFile.getWidth());
    }

    @Test
    @DisplayName("Set cover found in folder")
    void setCoverFoundInFolderTest() throws Exception {
        resetMp3File();
        BooleanProperty coverProperty = new SimpleBooleanProperty(false);
        when(testedTrack.hasCoverProperty()).thenReturn(coverProperty);
        when(testedTrack.getFileName()).thenReturn("testeable.mp3");
        MetadataUpdater updater = new MetadataUpdater();
        updater.setTrack(testedTrack);
        updater.searchCoverInFolderAndUpdate();

        Artwork expectedCover = ArtworkFactory.createArtworkFromFile(testCover);
        AudioFile audioFile = AudioFileIO.read(mp3File);
        Artwork coverOnFile = audioFile.getTag().getFirstArtwork();

        assertEquals(expectedCover.getHeight(), coverOnFile.getHeight());
        assertEquals(expectedCover.getWidth(), coverOnFile.getWidth());
    }

    private Track prepareTrack() {
        Track track = mock(Track.class);
        when(track.getFileFolder()).thenReturn(mp3File.getParent());
        when(track.getName()).thenReturn(name);
        when(track.getAlbum()).thenReturn(album);
        when(track.getAlbumArtist()).thenReturn(albumArtist);
        when(track.getArtist()).thenReturn(artist);
        when(track.getGenre()).thenReturn(genre);
        when(track.getComments()).thenReturn(comments);
        when(track.getLabel()).thenReturn(label);
        when(track.getTrackNumber()).thenReturn(trackNumber);
        when(track.getDiscNumber()).thenReturn(discNumber);
        when(track.getYear()).thenReturn(year);
        when(track.getBpm()).thenReturn(bpm);
        when(track.isPartOfCompilation()).thenReturn(isPartOfCompilation);
        return track;
    }

    private void assertFile(File file) throws Exception {
        AudioFile audio = AudioFileIO.read(file);
        Tag tag = audio.getTag();
        assertEquals(name, tag.getFirst(FieldKey.TITLE));
        assertEquals(album, tag.getFirst(FieldKey.ALBUM));
        assertEquals(albumArtist, tag.getFirst(FieldKey.ALBUM_ARTIST));
        assertEquals(artist, tag.getFirst(FieldKey.ARTIST));
        assertEquals(genre, tag.getFirst(FieldKey.GENRE));
        assertEquals(comments, tag.getFirst(FieldKey.COMMENT));
        assertEquals(label, tag.getFirst(FieldKey.GROUPING));
        assertEquals(trackNumber, Integer.parseInt(tag.getFirst(FieldKey.TRACK)));
        assertEquals(discNumber, Integer.parseInt(tag.getFirst(FieldKey.DISC_NO)));
        assertEquals(year, Integer.parseInt(tag.getFirst(FieldKey.YEAR)));
        assertEquals(bpm, Integer.parseInt(tag.getFirst(FieldKey.BPM)));
        assertEquals(isPartOfCompilation, Boolean.valueOf(tag.getFirst(FieldKey.IS_COMPILATION)));
    }

    void resetMp3File() throws Exception {
        AudioFile audio = AudioFileIO.read(mp3File);
        Tag tag = new ID3v24Tag();
        tag.getArtworkList().clear();
        resetCommonTagFields(tag);
        audio.setTag(tag);
        audio.commit();
    }

    void resetWavFile() throws Exception {
        AudioFile audio = AudioFileIO.read(wavFile);
        WavTag wavTag = new WavTag(WavOptions.READ_ID3_ONLY);
        wavTag.setID3Tag(new ID3v24Tag());
        wavTag.setInfoTag(new WavInfoTag());
        resetCommonTagFields(wavTag);
        audio.setTag(wavTag);
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

    void setTagField(FieldKey fieldKey, String value, Tag tag) throws FieldDataInvalidException {
        tag.setField(fieldKey, value);
    }
}
