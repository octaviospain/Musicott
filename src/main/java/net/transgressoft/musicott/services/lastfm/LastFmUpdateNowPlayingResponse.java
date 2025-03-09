package net.transgressoft.musicott.services.lastfm;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

/**
 * @author Octavio Calleya
 */
@JacksonXmlRootElement (localName = "lfm")
public class LastFmUpdateNowPlayingResponse {

    @JacksonXmlProperty (isAttribute = true)
    private String status;
    @JacksonXmlElementWrapper (localName = "nowplaying")
    private LastFmScrobble nowPlaying;
    @JacksonXmlProperty
    private String error;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LastFmScrobble getNowPlaying() {
        return nowPlaying;
    }

    public void setNowPlaying(LastFmScrobble nowPlaying) {
        this.nowPlaying = nowPlaying;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("status", status)
                .add("scrobbles", nowPlaying)
                .add("error", error)
                .toString();
    }
}
