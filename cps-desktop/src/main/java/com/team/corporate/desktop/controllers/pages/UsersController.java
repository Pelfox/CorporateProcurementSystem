package com.team.corporate.desktop.controllers.pages;

import com.team.corporate.desktop.controllers.dialog.DataReceiver;
import com.team.corporate.desktop.controllers.dialog.DialogService;
import com.team.corporate.desktop.controllers.dialog.ViewType;
import com.team.corporate.desktop.controllers.requests.UserEditorRequest;
import com.team.corporate.desktop.controllers.session.UserSession;
import com.team.corporate.desktop.controllers.utils.DateTimeFormat;
import com.team.corporate.desktop.controllers.utils.TextLengthInput;
import com.team.corporate.desktop.controllers.utils.UiErrors;
import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;
import com.team.corporate.core.services.UsersService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.util.*;

public class UsersController extends PageController implements DataReceiver<UserSession> {

    private final UsersService usersService;
    private final DialogService dialogService;
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObjectProperty<UserSession> session = new SimpleObjectProperty<>();
    @FXML
    private TextField searchField;
    @FXML
    private ToggleGroup roleTabs;
    @FXML
    private ToggleButton allTab, userTab, managerTab;
    @FXML
    private Button addBtn;
    @FXML
    private Button editBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, String> numCol;
    @FXML
    private TableColumn<User, String> nameCol;
    @FXML
    private TableColumn<User, UserRole> roleCol;
    @FXML
    private TableColumn<User, LocalDateTime> dateCol;

    public UsersController(UsersService usersService, DialogService dialogService) {
        this.usersService = usersService;
        this.dialogService = dialogService;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    @Override
    public void receiveData(UserSession session) {
        this.session.set(Objects.requireNonNull(session));
    }

    @FXML
    public void initialize() {
        TextLengthInput.configure(searchField);
        configureTable();
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        configureFilters();
        configureButtons();
        loadData();

        dateCol.setSortType(TableColumn.SortType.DESCENDING);
        usersTable.getSortOrder().add(dateCol);
    }

    private void loadData() {
        users.setAll(usersService.getAll());
    }

    private void configureTable() {
        numCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(shortId(cell.getValue().getId())));

        nameCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getUsername()));

        roleCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getUserRole()));
        roleCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(UserRole role, boolean empty) {
                super.updateItem(role, empty);
                setText(empty || role == null ? null : role.name());
            }
        });

        dateCol.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(DateTimeFormat.toLocalDateTime(cell.getValue().getCreatedAt())));
        dateCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : DateTimeFormat.formatDateTime(value));
            }
        });
    }

    private void configureFilters() {
        allTab.setUserData(RoleFilter.ALL);
        userTab.setUserData(new RoleFilter("USER", UserRole.USER));
        managerTab.setUserData(new RoleFilter("MANAGER", UserRole.MANAGER));

        FilteredList<User> filtered = new FilteredList<>(users, p -> true);
        Runnable reapply = () -> filtered.setPredicate(this::matchesFilters);
        searchField.textProperty().addListener((obs, old, value) -> reapply.run());
        roleTabs.selectedToggleProperty().addListener((obs, old, value) -> reapply.run());

        SortedList<User> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(usersTable.comparatorProperty());
        usersTable.setItems(sorted);

        roleTabs.selectToggle(allTab);
    }

    private void configureButtons() {
        editBtn.disableProperty().bind(
                usersTable.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.disableProperty().bind(Bindings.createBooleanBinding(this::cannotDeleteSelected,
                usersTable.getSelectionModel().selectedItemProperty(), session));
        Tooltip deleteHint = new Tooltip();
        deleteHint.textProperty().bind(Bindings.createStringBinding(() -> {
            User selected = usersTable.getSelectionModel().getSelectedItem();
            return selected != null && session.get() != null && selected.getId().equals(session.get().userId())
                    ? "Нельзя удалить свою учётную запись." : "Удалить выбранного пользователя.";
        }, usersTable.getSelectionModel().selectedItemProperty(), session));
        deleteBtn.setTooltip(deleteHint);
    }

    private boolean cannotDeleteSelected() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        UserSession current = session.get();
        return selected == null || current == null || current.role() != UserRole.MANAGER
                || selected.getId().equals(current.userId());
    }

    private boolean matchesFilters(User user) {
        String query = searchField.getText();
        if (query != null && !query.isBlank()) {
            String q = query.strip().toLowerCase(Locale.ROOT);
            boolean byName = user.getUsername().toLowerCase(Locale.ROOT).contains(q);
            boolean byNumber = user.getId().toString().toLowerCase(Locale.ROOT).contains(q);
            if (!byName && !byNumber) {
                return false;
            }
        }

        RoleFilter filter = roleTabs.getSelectedToggle() == null ? RoleFilter.ALL
                : (RoleFilter) roleTabs.getSelectedToggle().getUserData();
        return filter == null || filter.role() == null || user.getUserRole() == filter.role();
    }

    @FXML
    private void onAddClick() {
        if (session.get() == null || session.get().role() != UserRole.MANAGER) return;
        try {
            Optional<UUID> saved = dialogService.showModal(
                    ViewType.CREATE_USER,
                    addBtn.getScene().getWindow(),
                    UserEditorRequest.forCreate());
            if (saved.isPresent()) {
                loadData();
            }

        } catch (RuntimeException e) {
            UiErrors.show(usersTable.getScene() == null ? null : usersTable.getScene().getWindow(),
                    "Не удалось выполнить действие", e);
        }
    }

    @FXML
    private void onEditClick() {
        if (session.get() == null || session.get().role() != UserRole.MANAGER) return;
        try {
            User selected = usersTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            Optional<UUID> saved = dialogService.showModal(
                    ViewType.EDIT_USER,
                    editBtn.getScene().getWindow(),
                    UserEditorRequest.forEdit(selected));
            if (saved.isPresent()) {
                loadData();
            }

        } catch (RuntimeException e) {
            UiErrors.show(usersTable.getScene() == null ? null : usersTable.getScene().getWindow(),
                    "Не удалось выполнить действие", e);
        }
    }

    @FXML
    private void onDeleteClick() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (cannotDeleteSelected()) {
            return;
        }

        if (!dialogService.confirmDelete(usersTable.getScene().getWindow(), selected.getUsername())) return;

        try {
            usersService.deleteUser(session.get().userId(), selected.getId());
            loadData();
        } catch (RuntimeException e) {
            showAlert(e.getMessage() == null ? "Не удалось удалить пользователя." : e.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Пользователи");
        alert.setHeaderText(null);
        alert.initOwner(usersTable.getScene().getWindow());
        alert.showAndWait();
    }

    private record RoleFilter(String label, UserRole role) {
        static final RoleFilter ALL = new RoleFilter("Все роли", null);

        static List<RoleFilter> all() {
            List<RoleFilter> filters = new ArrayList<>();
            filters.add(ALL);
            for (UserRole role : UserRole.values()) {
                filters.add(new RoleFilter(role.name(), role));
            }
            return filters;
        }
    }
}
