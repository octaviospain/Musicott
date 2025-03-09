package net.transgressoft.musicott.services.lastfm;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

/**
 * @author Octavio Calleya
 */
@JacksonXmlRootElement (localName = "lfm")
public class LastFmSessionResponse {

    @JacksonXmlProperty (isAttribute = true)
    private String status;
    @JacksonXmlProperty
    private LastFmSession session;
    @JacksonXmlProperty
    private String error;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LastFmSession getSession() {
        return session;
    }

    public void setSession(LastFmSession session) {
        this.session = session;
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
                .add("session", session)
                .add("error", error)
                .toString();
    }
}
