package de.vektor.mail.core;

import java.util.List;

public interface MailService {

    void send(MailMessage message);

    List<MailMessage> getInbox(String recipient);
}
