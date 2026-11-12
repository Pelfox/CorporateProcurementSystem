package com.team.corporate.backend.security;

import com.team.corporate.backend.http.ApiException;
import com.team.corporate.core.entities.Order;
import com.team.corporate.core.entities.SessionType;
import com.team.corporate.core.services.OrdersService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Actor IDs supplied by legacy request DTOs must agree with the verified principal. */
@Component
public class BusinessAccess {
    private final OrdersService orders;

    public BusinessAccess(OrdersService orders) {
        this.orders = orders;
    }

    public SessionPrincipal principal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionPrincipal principal)
                || principal.type() != SessionType.FULL) {
            throw new AccessDeniedException("Требуется полная сессия.");
        }
        return principal;
    }

    public UUID actor(UUID claimedId) {
        var id = principal().userId();
        if (claimedId != null && !id.equals(claimedId)) {
            throw new AccessDeniedException("Нельзя действовать от имени другого пользователя.");
        }
        return id;
    }

    public boolean isManager() {
        return principal().role().canManageProcurement();
    }

    public UUID orderScope(UUID requestedUser) {
        if (isManager()) {
            return requestedUser;
        }
        return actor(requestedUser);
    }

    public Order requireOrder(UUID id) {
        var order = orders.getOrder(id).orElseThrow(() -> ApiException.notFound("Заказ"));
        if (!isManager() && !order.getUser().getId().equals(principal().userId())) {
            throw new AccessDeniedException("Нет доступа к заказу.");
        }
        return order;
    }
}
