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

package com.transgressoft.musicott.tasks.parse;

import com.google.common.collect.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.util.*;
import com.worldsworstsoftware.itunes.*;
import javafx.util.*;
import org.jaudiotagger.audio.*;
import org.jaudiotagger.tag.*;
import org.slf4j.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import static com.transgressoft.musicott.tasks.parse.ItunesParseTask.*;

/**
 * This class extends from {@link RecursiveTask} so it can be used inside a
 * {@link ForkJoinPool} in order to perform the conversion from a collection of
 * {@link ItunesTrack} instances to the system's {@link Track}.
 *
 * If it receives more than a certain amount of items to parse, the task is forked
 * with partitions of the items collection and their results joined after their completion.
 *
 * @author Octavio Calleya
 * @version 0.9.2-b
 * @since 0.9.2-b
 */
public class ItunesTracksParseAction extends RecursiveTask<ItunesParseResult> {

    private static final int MAX_ITEMS_TO_PARSE_PER_ACTION = 250;
    private static final int NUMBER_OF_ITEMS_PARTITIONS = 6;
    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
    private final int metadataPolicy;

    private List<ItunesTrack> itunesTracks;
    private Map<Integer, Track> parsedTracks;
    private Collection<String> parseErrors;
    private Map<Integer, Integer> itunesIdToMusicottIdMap;
    private List<String> notFoundFiles;
    private boolean holdPlayCount;
    private BaseParseTask parentTask;

    /**
     * Constructor of {@link ItunesTracksParseAction}
     *
     * @param itunesTracks   The {@link List} of {@link ItunesTrack} instances to parse
     * @param metadataPolicy The policy to follow importing the metadata
     * @param holdPlayCount  Flag to hold or not the play count from the iTunes Library data
     * @param parentTask     The reference to the parent {@link BaseParseTask} that called this action
     */
    public ItunesTracksParseAction(List<ItunesTrack> itunesTracks, int metadataPolicy, boolean holdPlayCount,
            BaseParseTask parentTask) {
        this.itunesTracks = itunesTracks;
        this.metadataPolicy = metadataPolicy;
        this.holdPlayCount = holdPlayCount;
        this.parentTask = parentTask;
        parsedTracks = new HashMap<>();
        itunesIdToMusicottIdMap = new HashMap<>();
        notFoundFiles = new ArrayList<>();
        parseErrors = new ArrayList<>();
    }

    @Override
    protected ItunesParseResult compute() {
        if (itunesTracks.size() > MAX_ITEMS_TO_PARSE_PER_ACTION) {
            int subListsSize = itunesTracks.size() / NUMBER_OF_ITEMS_PARTITIONS;
            ImmutableList<ItunesTracksParseAction> subActions = Lists.partition(itunesTracks, subListsSize)
                                                         .stream()
                                                         .map(subList -> new ItunesTracksParseAction(
                                                                 subList, metadataPolicy, holdPlayCount, parentTask))
                                                         .collect(ImmutableList.toImmutableList());

            subActions.forEach(ItunesTracksParseAction::fork);
            subActions.forEach(action -> joinPartialResults(action.join()));

            LOG.debug("Forking parse of files into {} sub actions", subActions.size());
        }
        else
            itunesTracks.forEach(this::parseItunesTrack);

        return new ItunesParseResult(parsedTracks, parseErrors, itunesIdToMusicottIdMap, notFoundFiles);
    }

    private void joinPartialResults(ItunesParseResult partialResult) {
        parsedTracks.putAll(partialResult.getParsedItems());
        parseErrors.addAll(partialResult.getParseErrors());
        itunesIdToMusicottIdMap.putAll(partialResult.getItunesIdToMusicottIdMap());
        notFoundFiles.addAll(partialResult.getNotFoundFiles());
    }

    private void parseItunesTrack(ItunesTrack itunesTrack) {
        Optional<Track> currentTrack = Optional.empty();
        if (isValidItunesTrack(itunesTrack)) {
            if (metadataPolicy == METADATA_POLICY)
                currentTrack = createTrackFromFileMetadata(itunesTrack);
            else if (metadataPolicy == ITUNES_DATA_POLICY)
                currentTrack = createTrackFromItunesData(itunesTrack);

            currentTrack.ifPresent(track -> {
                itunesIdToMusicottIdMap.put(itunesTrack.getTrackID(), track.getTrackId());
                parsedTracks.put(track.getTrackId(), track);
            });
        }
        parentTask.updateProgressTask();
    }

    private boolean isValidItunesTrack(ItunesTrack itunesTrack) {
        boolean valid = true;
        if ("URL".equals(itunesTrack.getTrackType()) || "Remote".equals(itunesTrack.getTrackType()))
            valid = false;
        else {
            File itunesFile = Paths.get(URI.create(itunesTrack.getLocation())).toFile();
            int index = itunesFile.toString().lastIndexOf('.');
            String fileExtension = itunesFile.toString().substring(index + 1);
            if (! ("mp3".equals(fileExtension) || "m4a".equals(fileExtension) || "wav".equals(fileExtension)))
                valid = false;
        }
        return valid;
    }

    /**
     * Creates a {@link Track} instance from the audio file metadata
     *
     * @param itunesTrack The {@link ItunesTrack} object
     *
     * @return The {@code Track} instance if the parse was successful
     */
    private Optional<Track> createTrackFromFileMetadata(ItunesTrack itunesTrack) {
        File itunesFile = Paths.get(URI.create(itunesTrack.getLocation())).toFile();
        Optional<Track> parsedTrack = Optional.empty();
        if (itunesFile.exists()) {
            try {
                parsedTrack = Optional.of(MetadataParser.createTrack(itunesFile));
            }
            catch (TrackParseException exception) {
                LOG.error("Error parsing {}", itunesFile, exception);
                parseErrors.add(itunesFile + ":" + exception.getMessage());
            }
            if (parsedTrack.isPresent() && holdPlayCount)
                parsedTrack.get().setPlayCount(itunesTrack.getPlayCount() < 1 ? 0 : itunesTrack.getPlayCount());
        }
        else
            notFoundFiles.add(itunesFile.toString());
        return parsedTrack;
    }

    /**
     * Creates a {@link Track} instance from the data stored on the {@code iTunes} library
     *
     * @param itunesTrack The {@link ItunesTrack} object
     *
     * @return The {@code Track} instance if the parse was successful
     */
    private Optional<Track> createTrackFromItunesData(ItunesTrack itunesTrack) {
        Path itunesPath = Paths.get(URI.create(itunesTrack.getLocation()));
        File itunesFile = itunesPath.toFile();
        Optional<Track> newTrack = Optional.empty();
        if (itunesFile.exists()) {
            Track track = parseItunesFieldsToTrackFields(itunesTrack, itunesPath);
            newTrack = Optional.of(track);
        }
        else
            notFoundFiles.add(itunesFile.toString());
        return newTrack;
    }

    private Track parseItunesFieldsToTrackFields(ItunesTrack itunesTrack, Path itunesPath) {
        String fileFolder = itunesPath.getParent().toString();
        String fileName = itunesPath.getName(itunesPath.getNameCount() - 1).toString();
        Track newTrack = new Track(fileFolder, fileName);
        newTrack.setInDisk(true);
        newTrack.setSize(itunesTrack.getSize());
        newTrack.setTotalTime(Duration.millis(itunesTrack.getTotalTime()));
        newTrack.setName(itunesTrack.getName() == null ? "" : itunesTrack.getName());
        newTrack.setAlbum(itunesTrack.getAlbum() == null ? "" : itunesTrack.getAlbum());
        newTrack.setArtist(itunesTrack.getArtist() == null ? "" : itunesTrack.getArtist());
        newTrack.setAlbumArtist(itunesTrack.getAlbumArtist() == null ? "" : itunesTrack.getAlbumArtist());
        newTrack.setGenre(itunesTrack.getGenre() == null ? "" : itunesTrack.getGenre());
        newTrack.setLabel(itunesTrack.getGrouping() == null ? "" : itunesTrack.getGrouping());
        newTrack.setIsPartOfCompilation(false);
        newTrack.setBpm(itunesTrack.getBPM() < 1 ? 0 : itunesTrack.getBPM());
        newTrack.setDiscNumber(itunesTrack.getDiscNumber() < 1 ? 0 : itunesTrack.getDiscNumber());
        newTrack.setTrackNumber(itunesTrack.getTrackNumber() < 1 ? 0 : itunesTrack.getTrackNumber());
        newTrack.setYear(itunesTrack.getYear() < 1 ? 0 : itunesTrack.getYear());

        if (holdPlayCount)
            newTrack.setPlayCount(itunesTrack.getPlayCount() < 1 ? 0 : itunesTrack.getPlayCount());
        setEncoderAndBitRateToTrack(itunesPath.toFile(), newTrack);
        return newTrack;
    }

    private void setEncoderAndBitRateToTrack(File trackFile, Track track) {
        try {
            AudioFile audioFile = AudioFileIO.read(trackFile);
            track.setEncoding(audioFile.getAudioHeader().getEncodingType());
            track.setEncoder(audioFile.getTag().getFirst(FieldKey.ENCODER));
            String bitRate = audioFile.getAudioHeader().getBitRate();
            if ("~".equals(bitRate.substring(0, 1))) {
                track.setIsVariableBitRate(true);
                bitRate = bitRate.substring(1);
            }
            track.setBitRate(Integer.parseInt(bitRate));
        }
        catch (Exception exception) {
            LOG.warn("Error getting encoder or bitrate from track {}:", track.getTrackId(), exception);
        }
    }
}