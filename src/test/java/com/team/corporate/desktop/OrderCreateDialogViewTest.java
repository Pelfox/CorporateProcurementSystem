package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.TestFx;
import org.testfx.util.WaitForAsyncUtils;

public class OrderCreateDialogViewTest extends BaseUITest {
    @Override
    protected String viewResource() { return "/fxml/orders/OrderCreateDialog.fxml"; }

    @Test
    @TestFx
    void shouldDisableSaveUntilOrderItemsAreValid() {
        Assertions.assertTrue(lookup("#createBtn").queryAs(Button.class).isDisabled());
        clickOn("#productCombo");
        clickOn("#quantityField").write("1");
        Assertions.assertTrue(lookup("#createBtn").queryAs(Button.class).isDisabled());
    }

    @Test
    @TestFx
    void shouldPersistOrderAfterAddingAnItem() {
        seedData();
        int ordersBefore = ordersService.getAll().size();
        clickOn("#productCombo");
        type(javafx.scene.input.KeyCode.DOWN);
        type(javafx.scene.input.KeyCode.ENTER);
        clickOn("#quantityField").write("1");
        clickOn("#addItemBtn");
        WaitForAsyncUtils.waitForFxEvents();
        clickOn("#createBtn");
        WaitForAsyncUtils.waitForFxEvents();
        Assertions.assertEquals(ordersBefore + 1, ordersService.getAll().size());
    }

    @Test
    @TestFx
    void shouldAllowCancellingOrderCreation() {
        clickOn("#cancelBtn");
        assertDialogClosed("Заказ");
    }
}
