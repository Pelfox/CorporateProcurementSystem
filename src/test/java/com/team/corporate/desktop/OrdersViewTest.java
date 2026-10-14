package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Assertions;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;
import org.testfx.matcher.control.LabeledMatchers;

public class OrdersViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/orders/OrdersView.fxml";
    }

    @TestFx
    void shouldDisplayOrderFiltersActionsAndTable() {
        FxAssert.verifyThat("#searchField", node -> node.isVisible());
        FxAssert.verifyThat("#createBtn", LabeledMatchers.hasText("Создать заказ"));
        FxAssert.verifyThat("#statusBtn", LabeledMatchers.hasText("Сменить статус"));
        Assertions.assertEquals(8, lookup("#ordersTable").queryAs(TableView.class).getColumns().size());
    }
}
