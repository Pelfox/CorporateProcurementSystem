package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.TestFx;

@Disabled("Enable after ProductEditorView.fxml is added")
public class ProductEditorViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/modals/ProductEditorView.fxml";
    }

    @Test
    @TestFx
    void shouldRequireNameSkuCategoryAndPositivePrice() {
        Assertions.assertTrue(lookup("#saveButton").queryAs(Button.class).isDisabled());
        clickOn("#productNameField").write("Laptop");
        clickOn("#productSkuField").write("LAPTOP-001");
        clickOn("#productPriceField").write("100");
        Assertions.assertTrue(lookup("#saveButton").queryAs(Button.class).isDisabled());
        clickOn("#productCategoryComboBox");
        Assertions.assertFalse(lookup("#saveButton").queryAs(Button.class).isDisabled());
    }

    @Test
    @TestFx
    void shouldExposeCancelActionForEditingProduct() {
        org.testfx.api.FxAssert.verifyThat("#productNameField", node -> node.isVisible());
        org.testfx.api.FxAssert.verifyThat("#productSkuField", node -> node.isVisible());
        org.testfx.api.FxAssert.verifyThat("#productCategoryComboBox", node -> node.isVisible());
        org.testfx.api.FxAssert.verifyThat("#productPriceField", node -> node.isVisible());
        org.testfx.api.FxAssert.verifyThat("#cancelButton", node -> node.isVisible());
    }
}
