package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.TestFx;

public class ProductDialogViewTest extends BaseUITest {
    @Override
    protected String viewResource() { return "/fxml/products/ProductDialog.fxml"; }

    @Test
    @TestFx
    void shouldRequireAllProductFields() {
        seedData();
        Assertions.assertTrue(lookup("#saveBtn").queryAs(Button.class).isDisabled());
        clickOn("#nameField").write("Laptop");
        clickOn("#skuField").write("LAPTOP-001");
        clickOn("#priceField").write("100");
        Assertions.assertTrue(lookup("#saveBtn").queryAs(Button.class).isDisabled());
        clickOn("#categoryCombo");
        clickOn("#saveBtn");
        Assertions.assertTrue(productsService.getAll().stream()
                .anyMatch(product -> "LAPTOP-001".equals(product.getSku())));
    }

    @Test
    @TestFx
    void shouldAllowCancellingProductEditing() {
        clickOn("#cancelBtn");
        assertDialogClosed("Товар");
    }
}
