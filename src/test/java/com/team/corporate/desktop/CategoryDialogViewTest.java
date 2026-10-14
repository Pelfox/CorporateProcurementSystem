package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.TestFx;

public class CategoryDialogViewTest extends BaseUITest {
    @Override
    protected String viewResource() { return "/fxml/categories/CategoryDialog.fxml"; }

    @Test
    @TestFx
    void shouldKeepSaveDisabledForBlankName() {
        Assertions.assertTrue(lookup("#saveBtn").queryAs(Button.class).isDisabled());
    }

    @Test
    @TestFx
    void shouldEnableSaveAfterEnteringName() {
        clickOn("#nameField").write("Office equipment");
        Assertions.assertFalse(lookup("#saveBtn").queryAs(Button.class).isDisabled());
        int categoriesBefore = categoriesService.getAll().size();
        clickOn("#saveBtn");
        Assertions.assertEquals(categoriesBefore + 1, categoriesService.getAll().size());
        Assertions.assertTrue(categoriesService.getAll().stream()
                .anyMatch(category -> "Office equipment".equals(category.getName())));
    }
}
