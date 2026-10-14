package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

@Disabled("Enable after CategoryEditorView.fxml is added")
public class CategoryEditorViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/modals/CategoryEditorView.fxml";
    }

    @Test
    @TestFx
    void shouldDisableSaveForBlankCategoryName() {
        FxAssert.verifyThat("#categoryNameField", node -> node.isVisible());
        Assertions.assertTrue(lookup("#saveButton").queryAs(Button.class).isDisabled());
    }

    @Test
    @TestFx
    void shouldEnableSaveWhenCategoryNameIsProvided() {
        clickOn("#categoryNameField").write("Office equipment");
        Assertions.assertFalse(lookup("#saveButton").queryAs(Button.class).isDisabled());
        FxAssert.verifyThat("#cancelButton", node -> node.isVisible());
    }
}
