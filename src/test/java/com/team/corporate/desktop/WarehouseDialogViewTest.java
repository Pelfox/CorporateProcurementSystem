package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.TestFx;

public class WarehouseDialogViewTest extends BaseUITest {
    @Override
    protected String viewResource() { return "/fxml/warehouses/WarehouseDialog.fxml"; }

    @Test
    @TestFx
    void shouldRequireWarehouseName() {
        Assertions.assertTrue(lookup("#saveBtn").queryAs(Button.class).isDisabled());
        clickOn("#nameField").write("Central warehouse");
        Assertions.assertFalse(lookup("#saveBtn").queryAs(Button.class).isDisabled());
        clickOn("#saveBtn");
        Assertions.assertTrue(warehousesService.getAll().stream()
                .anyMatch(warehouse -> "Central warehouse".equals(warehouse.getName())));
    }

    @Test
    @TestFx
    void shouldAcceptOptionalLocation() {
        clickOn("#nameField").write("Central warehouse");
        clickOn("#locationField").write("Main street, 1");
        clickOn("#cancelBtn");
        assertDialogClosed("Склад");
    }
}
