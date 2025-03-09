package net.transgressoft.musicott.services.lastfm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.base.MoreObjects;

/**
 * @author Octavio Calleya
 */
@JsonIgnoreProperties (value = {"streamable", "album", "mbid", "image"})
public class LastFmListenedTrack {

    @JacksonXmlProperty (isAttribute = true)
    private String nowplaying;
    @JacksonXmlProperty
    private String name;
    @JacksonXmlProperty
    private String artist;
    @JacksonXmlProperty
    private String url;
    @JacksonXmlProperty
    private String date;

    public String getNowPlaying() {
        return nowplaying;
    }

    public void setNowPlaying(String nowPlaying) {
        this.nowplaying = nowPlaying;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("nowplaying", nowplaying)
                .add("name", name)
                .add("artist", artist)
                .add("url", url)
                .add("date", date)
                .toString();
    }
}
