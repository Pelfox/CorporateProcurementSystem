package com.team.corporate;

import com.team.corporate.entities.User;
import com.team.corporate.entities.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UsersServiceTest extends BaseServiceTest {

    @Test
    @DisplayName("Создание пользователя: должен сохраниться и получить ID")
    void createUser_shouldSaveUserWithId() {
        // when
        User user = usersService.createUser("test_user", UserRole.USER);

        // then
        assertNotNull(user.getId(), "ID пользователя не должен быть null");
        assertEquals("test_user", user.getUsername());
        assertEquals(UserRole.USER, user.getUserRole());
    }

    @Test
    @DisplayName("Поиск по ID: должен вернуть пользователя, если он существует")
    void getUser_shouldReturnUser_whenExists() {
        // given
        User created = usersService.createUser("find_me", UserRole.MANAGER);

        // when
        Optional<User> found = usersService.getUser(created.getId());

        // then
        assertTrue(found.isPresent());
        assertEquals("find_me", found.get().getUsername());
    }

    @Test
    @DisplayName("Поиск по ID: должен вернуть пустой Optional, если пользователя нет")
    void getUser_shouldReturnEmpty_whenNotFound() {
        // when
        Optional<User> found = usersService.getUser(UUID.randomUUID());

        // then
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Фильтрация по роли: должен вернуть только пользователей с нужной ролью")
    void getAllByRole_shouldFilterUsers() {
        // given
        usersService.createUser("user1", UserRole.USER);
        usersService.createUser("manager1", UserRole.MANAGER);
        usersService.createUser("user2", UserRole.USER);

        // when
        List<User> users = usersService.getAllByRole(UserRole.USER);
        List<User> managers = usersService.getAllByRole(UserRole.MANAGER);

        // then
        assertEquals(2, users.size(), "Должно быть 2 обычных пользователя");
        assertEquals(1, managers.size(), "Должен быть 1 менеджер");
    }

    @Test
    @DisplayName("Обновление пользователя: должен изменить имя и роль")
    void updateUser_shouldModifyFields() {
        // given
        User user = usersService.createUser("old_name", UserRole.USER);

        // when
        usersService.updateUser(user.getId(), "new_name", UserRole.MANAGER);

        // then
        User updated = usersService.getUser(user.getId()).orElseThrow();
        assertEquals("new_name", updated.getUsername());
        assertEquals(UserRole.MANAGER, updated.getUserRole());
    }

    @Test
    @DisplayName("Удаление пользователя: должен исчезнуть из базы")
    void deleteUser_shouldRemoveUser() {
        // given
        User user = usersService.createUser("delete_me", UserRole.USER);

        // when
        usersService.deleteUser(user.getId());

        // then
        assertTrue(usersService.getUser(user.getId()).isEmpty(), "Пользователь должен быть удален");
    }

    @Test
    @DisplayName("Логин: должен найти пользователя по имени")
    void login_shouldReturnUser_whenUsernameMatches() {
        // given
        usersService.createUser("login_user", UserRole.USER);

        // when
        Optional<User> loggedIn = usersService.login("login_user");

        // then
        assertTrue(loggedIn.isPresent());
        assertEquals("login_user", loggedIn.get().getUsername());
    }

    @Test
    @DisplayName("Логин: должен вернуть пустой Optional для неверного имени")
    void login_shouldReturnEmpty_whenUsernameDoesNotMatch() {
        // when
        Optional<User> loggedIn = usersService.login("non_existent_user");

        // then
        assertTrue(loggedIn.isEmpty());
    }
}