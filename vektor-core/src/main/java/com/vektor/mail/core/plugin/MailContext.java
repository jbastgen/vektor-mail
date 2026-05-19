package com.vektor.mail.core.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Mutable context object passed through the mail processing pipeline.
 * Camel exchanges carry this as the message body.
 */
public class MailContext {

    private String senderIp;
    private String senderDomain;
    private String envelopeFrom;
    private java.util.List<String> envelopeTo = new java.util.ArrayList<>();
    private byte[] rawMessage;
    private final Map<String, Object> attributes = new HashMap<>();

    /** Set by FilterPlugins to signal the final decision. */
    private FilterDecision decision = FilterDecision.CONTINUE;

    /** Rejection reason (used when decision is REJECT or DEFER). */
    private String rejectReason;

    public String getSenderIp() { return senderIp; }
    public void setSenderIp(String senderIp) { this.senderIp = senderIp; }

    public String getSenderDomain() { return senderDomain; }
    public void setSenderDomain(String senderDomain) { this.senderDomain = senderDomain; }

    public String getEnvelopeFrom() { return envelopeFrom; }
    public void setEnvelopeFrom(String envelopeFrom) { this.envelopeFrom = envelopeFrom; }

    public java.util.List<String> getEnvelopeTo() { return envelopeTo; }
    public void setEnvelopeTo(java.util.List<String> envelopeTo) { this.envelopeTo = envelopeTo; }

    public byte[] getRawMessage() { return rawMessage; }
    public void setRawMessage(byte[] rawMessage) { this.rawMessage = rawMessage; }

    public FilterDecision getDecision() { return decision; }
    public void setDecision(FilterDecision decision) { this.decision = decision; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public Map<String, Object> getAttributes() { return attributes; }

    public void setAttribute(String key, Object value) { attributes.put(key, value); }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attributes.get(key); }
}
