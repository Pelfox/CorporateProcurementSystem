package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

@Disabled("Enable after ChangeOrderStatusView.fxml is added")
public class ChangeOrderStatusViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/modals/ChangeOrderStatusView.fxml";
    }

    @Test
    @TestFx
    void shouldDisplayCurrentOrderStatusAndDisableUnchangedStatus() {
        FxAssert.verifyThat("#orderIdLabel", node -> node.isVisible());
        FxAssert.verifyThat("#currentStatusLabel", node -> node.isVisible());
        Assertions.assertTrue(lookup("#changeButton").queryAs(Button.class).isDisabled());
    }

    @Test
    @TestFx
    void shouldEnableChangeAfterSelectingDifferentStatus() {
        clickOn("#newStatusComboBox");
        type(KeyCode.DOWN);
        type(KeyCode.ENTER);
        Assertions.assertFalse(lookup("#changeButton").queryAs(Button.class).isDisabled());
        FxAssert.verifyThat("#cancelButton", node -> node.isVisible());
    }
}
