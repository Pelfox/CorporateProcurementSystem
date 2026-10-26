package com.team.corporate.desktop.controllers.pages;

import com.team.corporate.desktop.controllers.dialog.DataReceiver;
import com.team.corporate.desktop.controllers.navigation.Page;
import com.team.corporate.desktop.controllers.session.UserSession;
import javafx.fxml.FXML;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ServicesController implements DataReceiver<UserSession> {

    @FXML
    private Label pageTitleLabel;
    @FXML
    private GridPane servicesGrid;

    private Consumer<Page> onNavigate;
    private UserSession session;

    // Каталог товаров
    @FXML
    private StackPane catalogCard;
    @FXML
    private Label catalogTitleLabel;
    @FXML
    private Label catalogSubtitleLabel;

    // Заказы
    @FXML
    private StackPane ordersCard;
    @FXML
    private Label ordersTitleLabel;
    @FXML
    private Label ordersSubtitleLabel;

    // Категории
    @FXML
    private StackPane categoriesCard;
    @FXML
    private Label categoriesTitleLabel;
    @FXML
    private Label categoriesSubtitleLabel;

    // Склады и остатки
    @FXML
    private StackPane warehousesCard;
    @FXML
    private Label warehousesTitleLabel;
    @FXML
    private Label warehousesSubtitleLabel;

    // Статистика
    @FXML
    private StackPane statisticsCard;
    @FXML
    private Label statisticsTitleLabel;
    @FXML
    private Label statisticsSubtitleLabel;

    // Экспорт данных
    @FXML
    private StackPane exportCard;
    @FXML
    private Label exportTitleLabel;
    @FXML
    private Label exportSubtitleLabel;

    // Пользователи
    @FXML
    private StackPane usersCard;
    @FXML
    private Label usersTitleLabel;
    @FXML
    private Label usersSubtitleLabel;

    @FXML
    public void initialize() {
        setManagerCardsVisible(false);
        for (StackPane card : List.of(catalogCard, ordersCard, categoriesCard,
                warehousesCard, statisticsCard, exportCard, usersCard)) {
            applyRoundedClip(card);
        }
        servicesGrid.widthProperty().addListener((observable, oldWidth, newWidth) -> updateCardLayout());
    }

    @Override
    public void receiveData(UserSession session) {
        this.session = Objects.requireNonNull(session);
        setManagerCardsVisible(Page.USERS.isAccessibleTo(session.role()));
    }

    private void setManagerCardsVisible(boolean visible) {
        for (StackPane card : List.of(exportCard, usersCard)) {
            card.setVisible(visible);
            card.setManaged(visible);
        }
        updateCardLayout();
    }

    private void updateCardLayout() {
        int columns = Math.max(1, Math.min(3,
                (int) ((servicesGrid.getWidth() + servicesGrid.getHgap()) / (260 + servicesGrid.getHgap()))));
        if (servicesGrid.getColumnConstraints().size() != columns) {
            servicesGrid.getColumnConstraints().clear();
            for (int column = 0; column < columns; column++) {
                ColumnConstraints constraint = new ColumnConstraints();
                constraint.setPercentWidth(100.0 / columns);
                constraint.setHgrow(Priority.ALWAYS);
                servicesGrid.getColumnConstraints().add(constraint);
            }
        }
        int index = 0;
        for (StackPane card : List.of(catalogCard, ordersCard, categoriesCard,
                warehousesCard, statisticsCard, exportCard, usersCard)) {
            if (!card.isManaged()) continue;
            GridPane.setColumnIndex(card, index % columns);
            GridPane.setRowIndex(card, index / columns);
            index++;
        }
    }

    public void setOnNavigate(Consumer<Page> onNavigate) {
        this.onNavigate = Objects.requireNonNull(onNavigate);
        Map.of(catalogCard, Page.PRODUCTS, ordersCard, Page.ORDERS,
                categoriesCard, Page.CATEGORIES, warehousesCard, Page.WAREHOUSES,
                statisticsCard, Page.STATISTICS, exportCard, Page.EXPORT,
                usersCard, Page.USERS).forEach((card, page) -> {
            card.setFocusTraversable(true);
            card.setAccessibleRole(AccessibleRole.BUTTON);
            card.setUserData(page);
        });
    }

    @FXML
    private void onCatalogClick(MouseEvent event) {
        navigate(event, Page.PRODUCTS);
    }

    @FXML
    private void onOrdersClick(MouseEvent event) {
        navigate(event, Page.ORDERS);
    }

    @FXML
    private void onCategoriesClick(MouseEvent event) {
        navigate(event, Page.CATEGORIES);
    }

    @FXML
    private void onWarehousesClick(MouseEvent event) {
        navigate(event, Page.WAREHOUSES);
    }

    @FXML
    private void onStatisticsClick(MouseEvent event) {
        navigate(event, Page.STATISTICS);
    }

    @FXML
    private void onExportClick(MouseEvent event) {
        navigate(event, Page.EXPORT);
    }

    @FXML
    private void onUsersClick(MouseEvent event) {
        navigate(event, Page.USERS);
    }

    private void navigate(MouseEvent event, Page page) {
        if (event.getButton() == MouseButton.PRIMARY) navigate(page);
    }

    private void navigate(Page page) {
        if (page != null && session != null && onNavigate != null
                && page.isAccessibleTo(session.role())) onNavigate.accept(page);
    }

    @FXML
    private void onCardKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            navigate((Page) ((StackPane) event.getSource()).getUserData());
            event.consume();
        }
    }

    // Обрезает содержимое карточки по скруглённому прямоугольнику — как overflow:hidden в HTML
    private void applyRoundedClip(StackPane card) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);   // = 2 * background-radius (14)
        clip.setArcHeight(28);
        clip.widthProperty().bind(card.widthProperty());
        clip.heightProperty().bind(card.heightProperty());
        card.setClip(clip);
    }
}
