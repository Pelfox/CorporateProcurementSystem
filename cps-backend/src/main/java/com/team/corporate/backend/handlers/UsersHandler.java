package com.team.corporate.backend.handlers;

import com.team.corporate.backend.dto.UserDto;
import com.team.corporate.backend.http.ApiException;
import com.team.corporate.backend.http.ApiExecutor;
import com.team.corporate.core.services.UsersService;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.team.corporate.backend.http.QueryExtractor.*;

@RestController
@RequestMapping("/api/users")
public class UsersHandler {
    private final UsersService users;
    private final ApiExecutor api;

    public UsersHandler(@NotNull UsersService users, @NotNull ApiExecutor api) {
        this.users = users;
        this.api = api;
    }

    @GetMapping
    public List<UserDto> list(@Valid @ModelAttribute UserDto.Query query) {
        String q = normalize(query.q());
        Comparator<UserDto> comparator = switch (query.sort()) {
            case NAME_ASC -> Comparator.comparing(UserDto::username, String.CASE_INSENSITIVE_ORDER);
            case NAME_DESC -> Comparator.comparing(UserDto::username, String.CASE_INSENSITIVE_ORDER).reversed();
            case CREATED_ASC -> Comparator.comparing(UserDto::createdAt);
            case CREATED_DESC -> Comparator.comparing(UserDto::createdAt).reversed();
        };
        return api.execute(() -> users.getAll().stream().map(UserDto::from)
                .filter(user -> contains(user.username(), q) || contains(user.id().toString(), q))
                .filter(user -> query.role() == null || user.role() == query.role())
                .sorted(comparator.thenComparing(UserDto::id)).toList());
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable UUID id) {
        return find(id);
    }

    @GetMapping("/by-username")
    public UserDto byUsername(@Valid @ModelAttribute UserDto.LookupQuery query) {
        return api.execute(() -> UserDto.from(users.login(query.username())
                .orElseThrow(() -> ApiException.notFound("Пользователь"))));
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserDto.Request body) {
        UUID id = api.execute(() -> users.createUser(body.username(), body.role()).getId());
        return ResponseEntity.created(URI.create("/api/users/" + id)).body(find(id));
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable UUID id, @Valid @RequestBody UserDto.Request body) {
        api.run(() -> users.updateUser(id, body.username(), body.role()));
        return find(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam UUID actingUserId) {
        api.run(() -> {
            users.getUser(id).orElseThrow(() -> ApiException.notFound("Пользователь"));
            users.deleteUser(actingUserId, id);
        });
        return ResponseEntity.noContent().build();
    }

    private UserDto find(@NotNull UUID id) {
        return api.execute(() -> UserDto.from(users.getUser(id)
                .orElseThrow(() -> ApiException.notFound("Пользователь"))));
    }
}
