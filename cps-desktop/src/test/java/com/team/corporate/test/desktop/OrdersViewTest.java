package com.team.corporate.test.desktop;

import com.team.corporate.core.entities.Order;
import com.team.corporate.core.entities.OrderStatus;
import javafx.scene.control.TableView;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.Future;

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

    @Test
    @TestFx
    void createOrder(){
        seedData();
        int ordersBefore = ordersService.getAll().size();
        Future<Void> dialogAction = clickAndWaitForDialog("#createBtn", "Заказ");
        assertDialogContains("#productCombo");
        clickOn("#productCombo").type(KeyCode.DOWN).type(KeyCode.ENTER);
        clickOn("#quantityField").write("1");
        clickOn("#addItemBtn");
        clickOn("#notesArea").write("UI order");
        clickOn("#createBtn");
        awaitDialogAction(dialogAction);
        Assertions.assertEquals(ordersBefore + 1, ordersService.getAll().size());
        Assertions.assertEquals("UI order",
                ordersService.getAll().get(ordersService.getAll().size() - 1).getNotes());
    }

    @Test
    @TestFx
    void editOrder(){
        seedData();
        Future<Void> dialogAction = clickAndWaitForDialog("#editBtn", "Заказ");
        finishDialogInteraction(dialogAction, "#cancelBtn", "Заказ");
        Assertions.assertFalse(ordersService.getAll().isEmpty());
    }

    @Test
    @TestFx
    void createdOrderDisplaysInTable(){
        seedData();
        Assertions.assertFalse(ordersService.getAll().isEmpty());
        Assertions.assertFalse(lookup("#ordersTable .table-cell")
                .queryAllAs(javafx.scene.control.Labeled.class).isEmpty());
        FxAssert.verifyThat("#ordersTable", node -> node.isVisible());
    }


    @Test
    @TestFx
    void changedStatusDisplaysInTable(){
        seedData();
        Order order = ordersService.getAll().get(0);
        Future<Void> dialogAction = clickAndWaitForDialog("#statusBtn", "статус");
        clickOn("#newStatusCombo").type(KeyCode.DOWN).type(KeyCode.ENTER);
        clickOn("#changeBtn");
        awaitDialogAction(dialogAction);
        Assertions.assertEquals(OrderStatus.APPROVED,
                ordersService.getOrder(order.getId()).orElseThrow().getStatus());
        FxAssert.verifyThat("#ordersTable", node -> node.isVisible());
    }

    @Test
    @TestFx
    void changedDescriptionDisplaysInTable(){
        seedData();
        Order order = ordersService.getAll().get(0);
        Future<Void> dialogAction = clickAndWaitForDialog("#editBtn", "Заказ");
        clickOn("#notesArea").eraseText(100).write("Updated note");
        clickOn("#saveBtn");
        awaitDialogAction(dialogAction);
        Assertions.assertEquals("Updated note",
                ordersService.getOrder(order.getId()).orElseThrow().getNotes());
        FxAssert.verifyThat("#ordersTable", node -> node.isVisible());
    }

    @Test
    @TestFx
    void deletedOrderDoesNotDisplayInTable(){
        seedData();
        Order order = ordersService.getAll().get(0);
        Future<Void> dialogAction = clickAndWaitForDialog("#deleteBtn", "Удаление");
        clickOn("#confirmBtn");
        awaitDialogAction(dialogAction);
        Assertions.assertTrue(ordersService.getOrder(order.getId()).isEmpty());

    }

    @Test
    @TestFx
    void shouldDisplayFilteredOrders(){
        seedData();
        clickOn("#searchField").write("Laptop");
        WaitForAsyncUtils.waitForFxEvents();
        Assertions.assertFalse(lookup("#ordersTable").queryAs(TableView.class).getItems().isEmpty());
        assertTableContainsText("#ordersTable", "Laptop");
        FxAssert.verifyThat("#ordersTable", node -> node.isVisible());
    }

    @Test
    @TestFx
    void shouldFilterOrdersByStatus(){
        seedData();
        TableView<?> ordersTable = lookup("#ordersTable").queryAs(TableView.class);
        int allOrders = ordersTable.getItems().size();
        ComboBox<?> statusFilter = lookup("#statusFilter").queryAs(ComboBox.class);
        Assertions.assertTrue(statusFilter.getItems().size() > 1,
                "Фильтр статуса должен содержать статусы и вариант 'Все'");
        interact(() -> statusFilter.getSelectionModel().select(1));
        WaitForAsyncUtils.waitForFxEvents();
        int filteredOrders = ordersTable.getItems().size();
        Assertions.assertTrue(filteredOrders > 0 && filteredOrders < allOrders,
                "Фильтр статуса должен сократить список заказов");
    }
}
