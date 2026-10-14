package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

public class MainWindowViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/pages/MainWindowView.fxml";
    }

    @Test
    @TestFx
    void shouldDisplayInventoryTableAndCreateOrderAction() {
        seedData();
        FxAssert.verifyThat("#inventorySearchField", node -> node.isVisible());
        FxAssert.verifyThat("#inventoryTable", node -> node.isVisible());
        FxAssert.verifyThat("#createOrderButton", node -> node.isVisible());
        Assertions.assertEquals(6, lookup("#inventoryTable").queryAs(TableView.class).getColumns().size());
        Assertions.assertFalse(lookup("#inventoryTable").queryAs(TableView.class).getItems().isEmpty());
    }

    @Test
    @TestFx
    void shouldFilterInventoryByProductNameOrSku() {
        seedData();
        TableView<?> inventoryTable = lookup("#inventoryTable").queryAs(TableView.class);
        int allItems = inventoryTable.getItems().size();
        clickOn("#inventorySearchField").write("laptop");
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        FxAssert.verifyThat("#inventoryTable", node -> node.isVisible());
        FxAssert.verifyThat("#inventorySearchField", node -> node.isVisible());
        Assertions.assertTrue(inventoryTable.getItems().size() > 0
                && inventoryTable.getItems().size() <= allItems);
    }
}
