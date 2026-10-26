package com.team.corporate.test.desktop;

import com.team.corporate.desktop.controllers.navigation.Page;
import com.team.corporate.core.entities.UserRole;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageAccessTest {
    @ParameterizedTest
    @EnumSource(UserRole.class)
    void onlyManagersCanOpenUserManagementAndExport(UserRole role) {
        assertEquals(role == UserRole.MANAGER, Page.USERS.isAccessibleTo(role));
        assertEquals(role == UserRole.MANAGER, Page.EXPORT.isAccessibleTo(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void sharedPagesRemainAvailableToBothRoles(UserRole role) {
        for (Page page : Page.values()) {
            if (page != Page.USERS && page != Page.EXPORT) assertTrue(page.isAccessibleTo(role), page.name());
        }
    }
}
