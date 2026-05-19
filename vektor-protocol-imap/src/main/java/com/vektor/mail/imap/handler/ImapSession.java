package com.vektor.mail.imap.handler;

import com.vektor.mail.core.model.Account;
import com.vektor.mail.core.model.Mailbox;

/** Per-connection IMAP session state. */
public class ImapSession {

    public enum State { NOT_AUTHENTICATED, AUTHENTICATED, SELECTED, LOGOUT }

    private State state = State.NOT_AUTHENTICATED;
    private Account account;
    private Mailbox selectedMailbox;

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public Mailbox getSelectedMailbox() { return selectedMailbox; }
    public void setSelectedMailbox(Mailbox selectedMailbox) { this.selectedMailbox = selectedMailbox; }
}
