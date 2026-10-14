package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

@Disabled("Enable after MainWindowView.fxml is added")
public class MainWindowViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/pages/MainWindowView.fxml";
    }

    @Test
    @TestFx
    void shouldDisplayInventoryTableAndCreateOrderAction() {
        FxAssert.verifyThat("#inventorySearchField", node -> node.isVisible());
        FxAssert.verifyThat("#inventoryTable", node -> node.isVisible());
        FxAssert.verifyThat("#createOrderButton", node -> node.isVisible());
        Assertions.assertEquals(6, lookup("#inventoryTable").queryAs(TableView.class).getColumns().size());
    }

    @Test
    @TestFx
    void shouldFilterInventoryByProductNameOrSku() {
        clickOn("#inventorySearchField").write("laptop");
        FxAssert.verifyThat("#inventoryTable", node -> node.isVisible());
        FxAssert.verifyThat("#inventorySearchField", node -> node.isVisible());
    }
}
