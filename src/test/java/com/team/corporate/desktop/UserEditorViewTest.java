package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

@Disabled("Enable after UserEditorView.fxml is added")
public class UserEditorViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/modals/UserEditorView.fxml";
    }

    @Test
    @TestFx
    void shouldDisableSaveUntilUsernameAndRoleAreSelected() {
        FxAssert.verifyThat("#userNameField", node -> node.isVisible());
        FxAssert.verifyThat("#userRoleComboBox", node -> node.isVisible());
        Assertions.assertTrue(lookup("#saveButton").queryAs(Button.class).isDisabled());
    }

    @Test
    @TestFx
    void shouldEnableSaveForValidUserData() {
        clickOn("#userNameField").write("warehouse-manager");
        clickOn("#userRoleComboBox");
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);
        Assertions.assertFalse(lookup("#saveButton").queryAs(Button.class).isDisabled());
        FxAssert.verifyThat("#cancelButton", node -> node.isVisible());
    }
}
