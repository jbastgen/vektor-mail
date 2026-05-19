package com.vektor.mail.smtp.handler;

import java.util.ArrayList;
import java.util.List;

/** Holds state for one SMTP connection. */
public class SmtpSession {

    public enum State {
        CONNECTED, GREETED, AUTH, MAIL, RCPT, DATA, QUIT
    }

    private State state = State.CONNECTED;
    private String clientHostname;
    private String envelopeFrom;
    private final List<String> envelopeTo = new ArrayList<>();
    private final StringBuilder dataBuffer = new StringBuilder();
    private boolean dataMode = false;

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public String getClientHostname() { return clientHostname; }
    public void setClientHostname(String clientHostname) { this.clientHostname = clientHostname; }

    public String getEnvelopeFrom() { return envelopeFrom; }
    public void setEnvelopeFrom(String envelopeFrom) { this.envelopeFrom = envelopeFrom; }

    public List<String> getEnvelopeTo() { return envelopeTo; }

    public StringBuilder getDataBuffer() { return dataBuffer; }

    public boolean isDataMode() { return dataMode; }
    public void setDataMode(boolean dataMode) { this.dataMode = dataMode; }

    public void reset() {
        envelopeFrom = null;
        envelopeTo.clear();
        dataBuffer.setLength(0);
        dataMode = false;
        state = State.GREETED;
    }
}
