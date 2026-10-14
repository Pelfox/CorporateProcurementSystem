package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

@Disabled("Enable after WarehouseEditorView.fxml is added")
public class WarehouseEditorViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/modals/WarehouseEditorView.fxml";
    }

    @Test
    @TestFx
    void shouldRequireWarehouseName() {
        FxAssert.verifyThat("#warehouseNameField", node -> node.isVisible());
        Assertions.assertTrue(lookup("#saveButton").queryAs(Button.class).isDisabled());
        clickOn("#warehouseNameField").write("Central warehouse");
        Assertions.assertFalse(lookup("#saveButton").queryAs(Button.class).isDisabled());
    }

    @Test
    @TestFx
    void shouldAllowOptionalAddressAndCancelEditing() {
        clickOn("#warehouseNameField").write("Central warehouse");
        clickOn("#warehouseAddressField").write("Main street, 1");
        FxAssert.verifyThat("#warehouseAddressField", node -> node.isVisible());
        FxAssert.verifyThat("#cancelButton", node -> node.isVisible());
    }
}
