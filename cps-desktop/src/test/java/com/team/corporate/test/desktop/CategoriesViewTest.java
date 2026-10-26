package com.team.corporate.test.desktop;

import com.team.corporate.core.entities.Category;
import javafx.scene.control.Labeled;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.Assertions;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.Future;

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

    void createCategory(String categoryName){
        // open form
        Future<Void> dialogAction = clickAndWaitForDialog("#addBtn", "Категория");
        // fill form
        clickOn("#nameField").write(categoryName); //write category name
        clickOn("#saveBtn"); //save button
        WaitForAsyncUtils.waitForFxEvents();
        awaitDialogAction(dialogAction);
    }

    void editCategory(String categoryName){
        // open form
        Future<Void> dialogAction = clickAndWaitForDialog("#editBtn", "Категория");
        // fill form
        clickOn("#nameField").eraseText(30).write(categoryName); //write category name
        clickOn("#saveBtn"); //save button
        WaitForAsyncUtils.waitForFxEvents(); // wait form to close
        awaitDialogAction(dialogAction);
    }
    @TestFx
    void createdCategoryDisplaysInTable(){
        createCategory("NewCategory");
        Assertions.assertTrue(categoriesService.getAll().stream()
                .map(Category::getName)
                .anyMatch("NewCategory"::equals));
        FxAssert.verifyThat("#categoriesGrid", node -> node.lookupAll(".category-card").stream()
                .flatMap(card -> card.lookupAll(".category-card-title").stream())
                .anyMatch(node_match -> node_match instanceof Labeled labeled
                        && "NewCategory".equals(labeled.getText())));

    }
    @TestFx
    void editedCategoryDisplaysInTable(){
        createCategory("NewCategory");
        editCategory("NewCategoryEdited");
        Assertions.assertTrue(categoriesService.getAll().stream()
                .map(Category::getName)
                .anyMatch("NewCategoryEdited"::equals));
        FxAssert.verifyThat("#categoriesGrid", node -> node.lookupAll(".category-card").stream()
                .flatMap(card -> card.lookupAll(".category-card-title").stream())
                .anyMatch(node_match -> node_match instanceof Labeled labeled
                        && "NewCategoryEdited".equals(labeled.getText())));
    }

    @TestFx
    void deletedCategoryDoesNotDisplayInTable(){
        createCategory("NewCategory");
        Future<Void> dialogAction = clickAndWaitForDialog("#deleteBtn", "Удаление");
        assertDialogContains("#confirmBtn");
        clickOn("#confirmBtn");
        awaitDialogAction(dialogAction);
        Assertions.assertTrue(categoriesService.getAll().stream()
                .noneMatch(category -> "NewCategory".equals(category.getName())));
        boolean categoryNameNotFound = lookup("#categoriesGrid").queryAllAs(Labeled.class).stream().
                noneMatch( node -> "NewCategory".equals(node.getText()));
        Assertions.assertTrue(categoryNameNotFound);

    }
    @TestFx
    void shouldDisplayFilteredCategories(){
        seedData();
        clickOn("#searchField").write("Мебель");
        WaitForAsyncUtils.waitForFxEvents();
        boolean categoryFiltered = lookup("#categoriesGrid").queryAllAs(Labeled.class).stream()
                .anyMatch(node -> "Мебель".equals(node.getText()));
        Assertions.assertTrue(categoryFiltered);
        Assertions.assertFalse(lookup("#categoriesGrid").queryAllAs(Labeled.class).stream()
                .anyMatch(node -> "Электроника".equals(node.getText())));
        Assertions.assertFalse(categoriesService.getAll().isEmpty());
    }
}
