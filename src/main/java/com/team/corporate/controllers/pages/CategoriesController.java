package com.team.corporate.controllers.pages;

import com.team.corporate.controllers.dialog.DialogService;
import com.team.corporate.controllers.dialog.ViewType;
import com.team.corporate.controllers.requests.CategoryEditorRequest;
import com.team.corporate.entities.Category;
import com.team.corporate.entities.Product;
import com.team.corporate.services.CategoriesService;
import com.team.corporate.services.ProductsService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.*;

public class CategoriesController {

    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private TilePane cardsPane;
    @FXML private Label placeholderLabel;

    private static final int CARD_WIDTH = 260;
    private static final int CARD_HEIGHT = 140;

    private final CategoriesService categoriesService;
    private final ProductsService productsService;
    private final DialogService dialogService;

    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private final Map<UUID, Long> productCounts = new HashMap<>();
    private final ObjectProperty<Category> selectedCategory = new SimpleObjectProperty<>(null);
    private final ToggleGroup cardGroup = new ToggleGroup();

    public CategoriesController(CategoriesService categoriesService,
                                ProductsService productsService,
                                DialogService dialogService) {
        this.categoriesService = categoriesService;
        this.productsService = productsService;
        this.dialogService = dialogService;
    }

    @FXML
    public void initialize() {
        editButton.disableProperty().bind(selectedCategory.isNull());
        deleteButton.disableProperty().bind(selectedCategory.isNull());

        placeholderLabel.managedProperty().bind(placeholderLabel.visibleProperty());
        placeholderLabel.visibleProperty().bind(Bindings.isEmpty(cardsPane.getChildren()));

        cardGroup.selectedToggleProperty().addListener((obs, old, toggle) ->
                selectedCategory.set(toggle == null ? null : (Category) toggle.getUserData()));

        searchField.textProperty().addListener((obs, old, value) -> rebuildCards());

        loadData();
    }

    private void loadData() {
        categories.setAll(categoriesService.getAll());

        productCounts.clear();
        for (Product product : productsService.getAll()) {
            Category category = product.getCategory();
            if (category != null) {
                productCounts.merge(category.getId(), 1L, Long::sum);
            }
        }

        selectedCategory.set(null);
        rebuildCards();
    }

    private void rebuildCards() {
        cardGroup.getToggles().clear();
        cardsPane.getChildren().clear();

        String query = searchField.getText();
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);

        for (Category category : categories) {
            if (!q.isEmpty() && !category.getName().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            cardsPane.getChildren().add(createCard(category));
        }
    }

    private ToggleButton createCard(Category category) {
        Label nameLabel = new Label(category.getName());
        nameLabel.getStyleClass().add("category-card-name");
        nameLabel.setWrapText(true);

        Label countLabel = new Label(productCountText(productCounts.getOrDefault(category.getId(), 0L)));
        countLabel.getStyleClass().add("category-card-count");

        VBox content = new VBox(nameLabel, countLabel);
        content.getStyleClass().add("category-card-content");

        ToggleButton card = new ToggleButton();
        card.getStyleClass().add("category-card");
        card.setToggleGroup(cardGroup);
        card.setUserData(category);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.setGraphic(content);
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);

        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                card.setSelected(true);
                openEditDialog(category);
            }
        });
        return card;
    }

    @FXML
    private void onAddClick() {
        Optional<UUID> saved = dialogService.showModal(
                ViewType.CREATE_CATEGORY,
                addButton.getScene().getWindow(),
                CategoryEditorRequest.forCreate());
        if (saved.isPresent()) {
            loadData();
        }
    }

    @FXML
    private void onEditClick() {
        Category selected = selectedCategory.get();
        if (selected == null) {
            return;
        }
        openEditDialog(selected);
    }

    @FXML
    private void onDeleteClick() {
        Category selected = selectedCategory.get();
        if (selected == null) {
            return;
        }

        // TODO: перед categoriesService.deleteCategory(...) подтвердить удаление.
        try {
            categoriesService.deleteCategory(selected.getId());
            loadData();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось удалить категорию." : e.getMessage());
        }
    }

    private void openEditDialog(Category category) {
        Optional<UUID> saved = dialogService.showModal(
                ViewType.EDIT_CATEGORY,
                editButton.getScene().getWindow(),
                CategoryEditorRequest.forEdit(category));
        if (saved.isPresent()) {
            loadData();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Категории");
        alert.setHeaderText(null);
        alert.initOwner(cardsPane.getScene().getWindow());
        alert.showAndWait();
    }

    private static String productCountText(long count) {
        long mod10 = count % 10;
        long mod100 = count % 100;
        String word;
        if (mod10 == 1 && mod100 != 11) {
            word = "товар";
        } else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) {
            word = "товара";
        } else {
            word = "товаров";
        }
        return count + " " + word;
    }
}
