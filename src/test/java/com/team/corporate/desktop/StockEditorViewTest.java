package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

@Disabled("Enable after StockEditorView.fxml is added")
public class StockEditorViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/modals/StockEditorView.fxml";
    }

    @Test
    @TestFx
    void shouldRequireWarehouseProductAndQuantity() {
        FxAssert.verifyThat("#warehouseComboBox", node -> node.isVisible());
        FxAssert.verifyThat("#productComboBox", node -> node.isVisible());
        FxAssert.verifyThat("#quantityField", node -> node.isVisible());
        Assertions.assertTrue(lookup("#saveButton").queryAs(Button.class).isDisabled());
    }

    @Test
    @TestFx
    void shouldExposeSaveAndCancelActionsForStock() {
        FxAssert.verifyThat("#saveButton", node -> node.isVisible());
        FxAssert.verifyThat("#cancelButton", node -> node.isVisible());
    }
}
