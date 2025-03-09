package net.transgressoft.musicott.services.lastfm;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * @author Octavio Calleya
 */
@JacksonXmlRootElement
public class LastFmScrobbleResponse {

    @JacksonXmlProperty (isAttribute = true)
    private String status;
    @JacksonXmlElementWrapper
    @JacksonXmlProperty(localName = "scrobbles")
    private List<LastFmScrobble> scrobbles;
    @JacksonXmlProperty
    private String error;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<LastFmScrobble> getScrobbles() {
        return scrobbles;
    }

    public void setScrobbles(List<LastFmScrobble> scrobbles) {
        this.scrobbles = scrobbles;
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
                .add("scrobbles", scrobbles)
                .add("error", error)
                .toString();
    }
}
