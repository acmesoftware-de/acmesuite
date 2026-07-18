package de.acmesoftware.acmesuite.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;

/** One message in a mail thread. Embedded (element collection) — not a standalone entity. */
@Embeddable
public class MailMessage {

    @Column(name = "message_id", length = 48)
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 16, nullable = false)
    private MailDirection direction;

    /** "from"/"to" are SQL keywords → from_addr/to_addr. */
    @Column(name = "from_addr", length = 160)
    private String fromAddr;

    @Column(name = "to_addr", length = 160)
    private String toAddr;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "snippet", length = 240)
    private String snippet;

    @Column(name = "body", length = 4000)
    private String body;

    protected MailMessage() {
    }

    public MailMessage(String messageId, MailDirection direction, String fromAddr, String toAddr, Instant sentAt,
                       String snippet, String body) {
        this.messageId = messageId;
        this.direction = direction;
        this.fromAddr = fromAddr;
        this.toAddr = toAddr;
        this.sentAt = sentAt;
        this.snippet = snippet;
        this.body = body;
    }

    public String getMessageId() {
        return messageId;
    }

    public MailDirection getDirection() {
        return direction;
    }

    public String getFromAddr() {
        return fromAddr;
    }

    public String getToAddr() {
        return toAddr;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getSnippet() {
        return snippet;
    }

    public String getBody() {
        return body;
    }
}
