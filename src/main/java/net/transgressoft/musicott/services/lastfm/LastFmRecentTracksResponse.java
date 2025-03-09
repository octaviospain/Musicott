package net.transgressoft.musicott.services.lastfm;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * @author Octavio Calleya
 */
@JacksonXmlRootElement (localName = "lfm")
public class LastFmRecentTracksResponse {

    @JacksonXmlProperty (isAttribute = true)
    private String status;
    @JacksonXmlElementWrapper (localName = "recenttracks")
    private List<LastFmListenedTrack> listenedTracks;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<LastFmListenedTrack> getListenedTracks() {
        return listenedTracks;
    }

    public void setListenedTracks(List<LastFmListenedTrack> listenedTracks) {
        this.listenedTracks = listenedTracks;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("status", status)
                .add("listenedTracks", listenedTracks)
                .toString();
    }
}
