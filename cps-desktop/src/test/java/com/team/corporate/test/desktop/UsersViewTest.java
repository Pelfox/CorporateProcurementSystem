package com.team.corporate.test.desktop;

import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

public class UsersViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/users/UsersView.fxml";
    }

    @Test
    @TestFx
    void shouldDisplayPersistedUserOnPage() {
        User user = usersService.createUser("page-user", UserRole.USER);
        FxAssert.verifyThat("#usersTable", node -> node.isVisible());
        Assertions.assertTrue(lookup("#usersTable .table-cell")
                .queryAllAs(javafx.scene.control.Labeled.class).stream()
                .anyMatch(cell -> user.getUsername().equals(cell.getText())));
    }

    @Test
    @TestFx
    void shouldFilterUsersByUsername() {
        usersService.createUser("visible-user", UserRole.USER);
        usersService.createUser("hidden-manager", UserRole.MANAGER);
        clickOn("#searchField").write("visible-user");
        FxAssert.verifyThat("#usersTable", node -> node.isVisible());
        Assertions.assertFalse(lookup("#usersTable").queryAs(TableView.class).getItems().isEmpty());
        assertTableContainsText("#usersTable", "visible-user");
        assertTableDoesNotContainText("#usersTable", "hidden-manager");
    }
}
