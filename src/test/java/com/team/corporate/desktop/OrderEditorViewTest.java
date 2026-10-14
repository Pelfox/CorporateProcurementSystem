package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

@Disabled("Enable after OrderEditorView.fxml is added")
public class OrderEditorViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/modals/OrderEditorView.fxml";
    }

    @Test
    @TestFx
    void shouldExposeOrderItemEditorAndKeepSaveDisabledUntilValid() {
        FxAssert.verifyThat("#orderUserComboBox", node -> node.isVisible());
        FxAssert.verifyThat("#orderProductComboBox", node -> node.isVisible());
        FxAssert.verifyThat("#orderQuantityField", node -> node.isVisible());
        Assertions.assertTrue(lookup("#saveButton").queryAs(Button.class).isDisabled());
    }

    @Test
    @TestFx
    void shouldAllowEnteringOrderNotesAndCancelEditing() {
        clickOn("#orderNotesField").write("Urgent delivery");
        FxAssert.verifyThat("#orderNotesField", node -> node.isVisible());
        FxAssert.verifyThat("#cancelButton", node -> node.isVisible());
    }
}
