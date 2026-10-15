package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.TableView;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;
import org.testfx.util.WaitForAsyncUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

@Disabled("Enable when MyOrdersView controller is wired")
public class MyOrdersViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/orders/MyOrdersView.fxml";
    }

    @Test
    @TestFx
    void shouldDisplayUserOrdersAndWelcomeBanner() {
        seedData();
        FxAssert.verifyThat("#welcomeBanner", node -> node.isVisible());
        FxAssert.verifyThat("#ordersTable", node -> node.isVisible());
        Assertions.assertFalse(lookup("#ordersTable").queryAs(TableView.class)
                .getItems().isEmpty());
    }

    @Test
    @TestFx
    void shouldExposeOrderFilters() {
        FxAssert.verifyThat("#statusFilter", node -> node.isVisible());
        FxAssert.verifyThat("#dateFromPicker", node -> node.isVisible());
        FxAssert.verifyThat("#dateToPicker", node -> node.isVisible());
        FxAssert.verifyThat("#sortCombo", node -> node.isVisible());
    }

    @Test
    @TestFx
    void shouldFilterOrdersByStatus() {
        seedData();
        TableView<?> ordersTable = lookup("#ordersTable").queryAs(TableView.class);
        int allOrders = ordersTable.getItems().size();
        ComboBox<?> statusFilter = lookup("#statusFilter").queryAs(ComboBox.class);
        Assertions.assertTrue(statusFilter.getItems().size() > 1);
        interact(() -> statusFilter.getSelectionModel().select(1));
        WaitForAsyncUtils.waitForFxEvents();
        int filteredOrders = ordersTable.getItems().size();
        Assertions.assertTrue(filteredOrders >= 0 && filteredOrders < allOrders);
    }

    @Test
    @TestFx
    void shouldFilterOrdersByDateRange() {
        seedData();
        DatePicker from = lookup("#dateFromPicker").queryAs(DatePicker.class);
        DatePicker to = lookup("#dateToPicker").queryAs(DatePicker.class);
        TableView<?> ordersTable = lookup("#ordersTable").queryAs(TableView.class);
        interact(() -> {
            from.setValue(LocalDate.of(2000, 1, 1));
            to.setValue(LocalDate.of(2000, 1, 2));
        });
        WaitForAsyncUtils.waitForFxEvents();
        Assertions.assertTrue(ordersTable.getItems().isEmpty(),
                "Диапазон без заказов должен скрыть все строки");
    }
}
