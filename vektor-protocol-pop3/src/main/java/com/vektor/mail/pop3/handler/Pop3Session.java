package com.vektor.mail.pop3.handler;

import com.vektor.mail.core.model.Account;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class Pop3Session {
    private String pendingUser;
    private Account account;
    private final Set<Integer> deletedIndexes = new HashSet<>();
}
