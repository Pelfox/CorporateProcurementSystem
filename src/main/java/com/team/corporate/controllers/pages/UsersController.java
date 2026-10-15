package com.team.corporate.controllers.pages;

import com.team.corporate.controllers.dialog.DialogService;
import com.team.corporate.controllers.dialog.ViewType;
import com.team.corporate.controllers.requests.UserEditorRequest;
import com.team.corporate.controllers.utils.DateTimeFormat;
import com.team.corporate.entities.User;
import com.team.corporate.entities.UserRole;
import com.team.corporate.services.UsersService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.util.*;

public class UsersController {

    @FXML private TextField searchField;
    @FXML private ComboBox<RoleFilter> roleComboBox;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> numberColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, UserRole> roleColumn;
    @FXML private TableColumn<User, LocalDateTime> createdColumn;

    private final UsersService usersService;
    private final DialogService dialogService;

    private final ObservableList<User> users = FXCollections.observableArrayList();

    public UsersController(UsersService usersService, DialogService dialogService) {
        this.usersService = usersService;
        this.dialogService = dialogService;
    }

    @FXML
    public void initialize() {
        configureTable();
        configureFilters();
        configureButtons();
        loadData();

        createdColumn.setSortType(TableColumn.SortType.DESCENDING);
        usersTable.getSortOrder().add(createdColumn);
    }

    private void loadData() {
        users.setAll(usersService.getAll());
    }

    private void configureTable() {
        numberColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(shortId(cell.getValue().getId())));

        usernameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getUsername()));

        roleColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getUserRole()));
        roleColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(UserRole role, boolean empty) {
                super.updateItem(role, empty);
                setText(empty || role == null ? null : role.name());
            }
        });

        createdColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(DateTimeFormat.toLocalDateTime(cell.getValue().getCreatedAt())));
        createdColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : DateTimeFormat.formatDateTime(value));
            }
        });
    }

    private void configureFilters() {
        roleComboBox.setItems(FXCollections.observableArrayList(RoleFilter.all()));
        roleComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(RoleFilter filter) { return filter == null ? "" : filter.label(); }
            @Override public RoleFilter fromString(String string) { return null; }
        });

        FilteredList<User> filtered = new FilteredList<>(users, p -> true);
        Runnable reapply = () -> filtered.setPredicate(this::matchesFilters);
        searchField.textProperty().addListener((obs, old, value) -> reapply.run());
        roleComboBox.valueProperty().addListener((obs, old, value) -> reapply.run());

        SortedList<User> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(usersTable.comparatorProperty());
        usersTable.setItems(sorted);

        roleComboBox.setValue(RoleFilter.ALL);
    }

    private void configureButtons() {
        editButton.disableProperty().bind(
                usersTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(
                usersTable.getSelectionModel().selectedItemProperty().isNull());
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

        RoleFilter filter = roleComboBox.getValue();
        return filter == null || filter.role() == null || user.getUserRole() == filter.role();
    }

    @FXML
    private void onAddClick() {
        Optional<UUID> saved = dialogService.showModal(
                ViewType.CREATE_USER,
                addButton.getScene().getWindow(),
                UserEditorRequest.forCreate());
        if (saved.isPresent()) {
            loadData();
        }
    }

    @FXML
    private void onEditClick() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Optional<UUID> saved = dialogService.showModal(
                ViewType.EDIT_USER,
                editButton.getScene().getWindow(),
                UserEditorRequest.forEdit(selected));
        if (saved.isPresent()) {
            loadData();
        }
    }

    @FXML
    private void onDeleteClick() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        try {
            usersService.deleteUser(selected.getId());
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

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
