package com.team.corporate.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/** Only fixed action/result codes and IDs: never credentials, request bodies or headers. */
public final class SecurityAudit {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityAudit.class);

    private SecurityAudit() {
    }

    public static void record(String action, UUID actor, UUID target, String result) {
        LOG.info("security action={} actor={} target={} result={}", action, actor, target, result);
    }

    public static UUID currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof SessionPrincipal principal ? principal.userId() : null;
    }

    public static void failureOnRollback(String action, UUID actor, UUID target) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    record(action, actor, target, "REJECTED");
                }
            }
        });
    }

    public static void success(String action, UUID actor, UUID target) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    record(action, actor, target, "SUCCESS");
                }
            });
        } else {
            record(action, actor, target, "SUCCESS");
        }
    }
}
