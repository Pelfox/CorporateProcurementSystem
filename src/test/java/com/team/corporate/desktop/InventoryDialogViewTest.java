package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.TestFx;

public class InventoryDialogViewTest extends BaseUITest {
    @Override
    protected String viewResource() { return "/fxml/warehouses/InventoryDialog.fxml"; }

    @Test
    @TestFx
    void shouldRequireWarehouseProductAndQuantity() {
        Assertions.assertTrue(lookup("#saveBtn").queryAs(Button.class).isDisabled());
        clickOn("#quantityField").write("10");
        Assertions.assertTrue(lookup("#saveBtn").queryAs(Button.class).isDisabled());
    }

    @Test
    @TestFx
    void shouldAllowCancellingInventoryEditing() {
        clickOn("#cancelBtn");
        assertDialogClosed("Остаток");
    }
}
