package net.transgressoft.musicott.services.lastfm;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.base.MoreObjects;

/**
 * @author Octavio Calleya
 */
public class LastFmScrobble {

    @JacksonXmlProperty
    private String track;
    @JacksonXmlProperty
    private String artist;
    @JacksonXmlProperty
    private String album;
    @JacksonXmlProperty
    private String albumArtist;
    @JacksonXmlProperty
    private String ignoredMessage;
    @JacksonXmlProperty
    private String timestamp;

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
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

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public String getIgnoredMessage() {
        return ignoredMessage;
    }

    public void setIgnoredMessage(String ignoredMessage) {
        this.ignoredMessage = ignoredMessage;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("track", track)
                .add("artist", artist)
                .add("album", album)
                .add("albumArtist", albumArtist)
                .add("timestamp", timestamp)
                .add("ignoredMessage", ignoredMessage)
                .toString();
    }
}
