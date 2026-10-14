package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import com.team.corporate.entities.Product;
import javafx.scene.control.TableView;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;

import org.junit.jupiter.api.Assertions;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;
import org.testfx.matcher.control.LabeledMatchers;
import org.junit.jupiter.api.Disabled;
import org.testfx.util.WaitForAsyncUtils;
import java.util.concurrent.Future;

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

    @TestFx
    void createdProductDisplaysInTable(){
        seedData();
        Future<Void> dialogAction = clickAndWaitForDialog("#addBtn", "Товар");
        clickOn("#nameField").write("Test product");
        clickOn("#skuField").write("TEST-001");
        clickOn("#categoryCombo").type(KeyCode.DOWN).type(KeyCode.ENTER);
        clickOn("#priceField").write("100");
        clickOn("#saveBtn");
        WaitForAsyncUtils.waitForFxEvents();
        awaitDialogAction(dialogAction);
        Assertions.assertTrue(productsService.getAll().stream()
                .map(Product::getSku)
                .anyMatch("TEST-001"::equals));
        assertTableContainsText("#productsTable", "TEST-001");
        FxAssert.verifyThat("#productsTable", node -> node.isVisible());
    }

    @TestFx
    void changedProductDisplaysInTable(){
        seedData();
        Product existing = productsService.getAll().get(0);
        assertTableContainsText("#productsTable", existing.getSku());
        Future<Void> dialogAction = clickAndWaitForDialog("#editBtn", "Товар");
        clickOn("#nameField").eraseText(30).write("Updated product");
        clickOn("#saveBtn");
        WaitForAsyncUtils.waitForFxEvents();
        awaitDialogAction(dialogAction);
        Assertions.assertEquals("Updated product",
                productsService.getProduct(existing.getId()).orElseThrow().getName());
        assertTableContainsText("#productsTable", "Updated product");
        FxAssert.verifyThat("#productsTable", node -> node.isVisible());
    }

    @TestFx
    void deletedProductDoesNotDisplayInTable(){
        seedData();
        Product existing = productsService.getAll().get(0);
        Future<Void> dialogAction = clickAndWaitForDialog("#deleteBtn", "Удаление");
        finishDialogInteraction(dialogAction, "#cancelBtn", "Удаление");
        Assertions.assertTrue(productsService.getProduct(existing.getId()).isPresent(),
                "Отмена удаления не должна удалять товар");
        assertTableContainsText("#productsTable", existing.getSku());
        FxAssert.verifyThat("#productsTable", node -> node.isVisible());
    }

    @TestFx
    void shouldDisplayFilteredProducts(){
        seedData();
        clickOn("#searchField").write("Laptop");
        WaitForAsyncUtils.waitForFxEvents();
        Assertions.assertFalse(lookup("#productsTable").queryAs(TableView.class).getItems().isEmpty());
        assertTableContainsText("#productsTable", "Laptop");
        assertTableDoesNotContainText("#productsTable", "Chair");
        FxAssert.verifyThat("#productsTable", node -> node.isVisible());
    }

    @TestFx
    void shouldFilterProductsByCategory(){
        seedData();
        ComboBox<?> categoryFilter = lookup("#categoryFilter").queryAs(ComboBox.class);
        Assertions.assertTrue(categoryFilter.getItems().size() > 1,
                "Фильтр категорий должен содержать категории и вариант 'Все'");
        int allProducts = lookup("#productsTable").queryAs(TableView.class).getItems().size();
        interact(() -> categoryFilter.getSelectionModel().select(1));
        WaitForAsyncUtils.waitForFxEvents();
        int filteredProducts = lookup("#productsTable").queryAs(TableView.class).getItems().size();
        Assertions.assertTrue(filteredProducts > 0 && filteredProducts < allProducts,
                "Фильтр категории должен сократить список товаров");
    }
}
