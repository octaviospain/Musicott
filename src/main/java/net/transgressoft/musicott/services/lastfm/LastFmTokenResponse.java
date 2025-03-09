package net.transgressoft.musicott.services.lastfm;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

/**
 * @author Octavio Calleya
 */
@JacksonXmlRootElement (localName = "lfm")
public class LastFmTokenResponse {

    @JacksonXmlProperty (isAttribute = true)
    private String status;
    @JacksonXmlProperty
    private String token;
    @JacksonXmlProperty
    private String error;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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
                .add("token", token)
                .add("error", error)
                .toString();
    }
}
