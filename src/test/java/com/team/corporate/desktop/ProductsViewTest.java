package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Assertions;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;
import org.testfx.matcher.control.LabeledMatchers;

public class ProductsViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/products/ProductsView.fxml";
    }

    @TestFx
    void shouldDisplayProductFiltersActionsAndTable() {
        FxAssert.verifyThat("#searchField", node -> node.isVisible());
        FxAssert.verifyThat("#categoryFilter", node -> node.isVisible());
        FxAssert.verifyThat("#addBtn", LabeledMatchers.hasText("Добавить"));
        TableView<?> table = lookup("#productsTable").queryAs(TableView.class);
        Assertions.assertEquals(5, table.getColumns().size());
        FxAssert.verifyThat("#productsTable", node -> node.isVisible());
    }
}
