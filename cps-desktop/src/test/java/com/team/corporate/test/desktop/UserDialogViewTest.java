package com.team.corporate.test.desktop;

import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.TestFx;

public class UserDialogViewTest extends BaseUITest {
    @Override
    protected String viewResource() { return "/fxml/users/UserDialog.fxml"; }

    @Test
    @TestFx
    void shouldRequireUsernameAndRole() {
        Assertions.assertTrue(lookup("#saveBtn").queryAs(Button.class).isDisabled());
        clickOn("#usernameField").write("manager");
        Assertions.assertTrue(lookup("#saveBtn").queryAs(Button.class).isDisabled());
    }

    @Test
    @TestFx
    void shouldEnableSaveAfterSelectingRole() {
        int usersBefore = usersService.getAll().size();
        clickOn("#usernameField").write("manager");
        clickOn("#roleCombo");
        type(javafx.scene.input.KeyCode.DOWN);
        type(javafx.scene.input.KeyCode.ENTER);
        Assertions.assertFalse(lookup("#saveBtn").queryAs(Button.class).isDisabled());
        clickOn("#saveBtn");
        Assertions.assertEquals(usersBefore + 1, usersService.getAll().size());
        Assertions.assertTrue(usersService.getAll().stream()
                .anyMatch(user -> "manager".equals(user.getUsername())));
    }
}
