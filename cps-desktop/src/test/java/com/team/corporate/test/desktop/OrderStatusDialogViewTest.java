package com.team.corporate.test.desktop;

import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.TestFx;

public class OrderStatusDialogViewTest extends BaseUITest {
    @Override
    protected String viewResource() { return "/fxml/orders/OrderStatusDialog.fxml"; }

    @Test
    @TestFx
    void shouldDisplayCurrentStatusAndDisableUnchangedSubmit() {
        Assertions.assertFalse(lookup("#currentStatusLabel").query().isVisible());
        Assertions.assertTrue(lookup("#changeBtn").queryAs(Button.class).isDisabled());
    }

    @Test
    @TestFx
    void shouldEnableSubmitAfterSelectingNewStatus() {
        clickOn("#newStatusCombo");
        type(javafx.scene.input.KeyCode.DOWN);
        type(javafx.scene.input.KeyCode.ENTER);
        Assertions.assertFalse(lookup("#changeBtn").queryAs(Button.class).isDisabled());
        clickOn("#cancelBtn");
        assertDialogClosed("Изменение статуса");
    }
}
