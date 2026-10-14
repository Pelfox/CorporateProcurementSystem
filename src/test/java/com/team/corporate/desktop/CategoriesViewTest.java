package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.Assertions;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;
import org.testfx.matcher.control.LabeledMatchers;

public class CategoriesViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/categories/CategoriesView.fxml";
    }

    @TestFx
    void shouldDisplayCategorySearchActionsAndGrid() {
        FxAssert.verifyThat("#searchField", node -> node.isVisible());
        FxAssert.verifyThat("#addBtn", LabeledMatchers.hasText("Добавить"));
        FxAssert.verifyThat("#editBtn", LabeledMatchers.hasText("Редактировать"));
        FxAssert.verifyThat("#deleteBtn", LabeledMatchers.hasText("Удалить"));
        Assertions.assertEquals(0, lookup("#categoriesGrid").queryAs(GridPane.class).getChildren().size());
        FxAssert.verifyThat("#emptyPlaceholder", LabeledMatchers.hasText("Категории не найдены"));
    }
}
