package com.team.corporate.test.desktop;

import com.team.corporate.core.entities.Warehouse;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

public class WarehouseViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/warehouses/WarehousesView.fxml";
    }

    @Test
    @TestFx
    void shouldDisplayPersistedWarehouseOnPage() {
        Warehouse warehouse = warehousesService.createWarehouse("page-warehouse", "Main street");
        FxAssert.verifyThat("#warehousesTable", node -> node.isVisible());
        assertTableContainsText("#warehousesTable", warehouse.getName());
    }

    @Test
    @TestFx
    void shouldExposeWarehouseActionsForPersistedData() {
        warehousesService.createWarehouse("editable-warehouse", "Old address");
        FxAssert.verifyThat("#addBtn", node -> node.isVisible());
        FxAssert.verifyThat("#editBtn", node -> node.isVisible());
        FxAssert.verifyThat("#deleteBtn", node -> node.isVisible());
        Assertions.assertFalse(lookup("#warehousesTable").queryAs(TableView.class)
                .getItems().isEmpty());
        assertTableContainsText("#warehousesTable", "editable-warehouse");
    }
}
