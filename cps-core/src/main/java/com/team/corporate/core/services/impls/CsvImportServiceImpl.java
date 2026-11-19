package com.team.corporate.core.services.impls;

import com.team.corporate.core.dto.CsvApplyResponse;
import com.team.corporate.core.dto.CsvRowError;
import com.team.corporate.core.dto.CsvValidationResponse;
import com.team.corporate.core.dto.ImportMode;
import com.team.corporate.core.entities.Product;
import com.team.corporate.core.entities.UserRole;
import com.team.corporate.core.exceptions.CsvImportAccessDeniedException;
import com.team.corporate.core.exceptions.CsvImportConflictException;
import com.team.corporate.core.exceptions.CsvImportNotFoundException;
import com.team.corporate.core.repositories.CategoriesRepository;
import com.team.corporate.core.repositories.ProductsRepository;
import com.team.corporate.core.repositories.Transactions;
import com.team.corporate.core.repositories.UsersRepository;
import com.team.corporate.core.services.CsvImportService;
import com.team.corporate.core.utils.EntityValidation;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public final class CsvImportServiceImpl implements CsvImportService {
    public static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    public static final int MAX_ROWS = 10_000;
    public static final int MAX_PENDING_IMPORTS = 100;
    public static final Duration IMPORT_TTL = Duration.ofMinutes(15);

    private static final Set<String> REQUIRED_HEADERS = Set.of("sku", "name", "category_id", "price");
    private static final Set<String> ALLOWED_HEADERS = Set.of("id", "sku", "name", "category_id", "price");
    private static final CSVFormat FORMAT = CSVFormat.RFC4180.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(false)
            .setAllowMissingColumnNames(false)
            .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
            .get();

    private final UsersRepository users;
    private final CategoriesRepository categories;
    private final ProductsRepository products;
    private final Transactions transactions;
    private final Clock clock;
    private final Map<UUID, PendingImport> pending = new HashMap<>();

    public CsvImportServiceImpl(UsersRepository users, CategoriesRepository categories,
                                ProductsRepository products, Transactions transactions) {
        this(users, categories, products, transactions, Clock.systemUTC());
    }

    public CsvImportServiceImpl(UsersRepository users, CategoriesRepository categories,
                                ProductsRepository products, Transactions transactions, Clock clock) {
        this.users = Objects.requireNonNull(users);
        this.categories = Objects.requireNonNull(categories);
        this.products = Objects.requireNonNull(products);
        this.transactions = Objects.requireNonNull(transactions);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @NotNull
    public synchronized CsvValidationResponse validateProducts(UUID managerId, byte[] csv, ImportMode mode) {
        EntityValidation.requireNonNull(managerId, "Идентификатор менеджера");
        EntityValidation.requireNonNull(csv, "CSV-файл");
        EntityValidation.requireNonNull(mode, "Режим импорта");
        removeExpired();

        if (pending.size() >= MAX_PENDING_IMPORTS) {
            throw new CsvImportConflictException("Слишком много загрузок. Дождитесь истечения срока их хранения.");
        }

        if (csv.length == 0 || csv.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Загрузите непустой CSV-файл размером не более 2 МБ.");
        }

        UUID importId = UUID.randomUUID();
        List<CsvRowError> errors = new ArrayList<>();
        List<ProductRow> rows = transactions.execute(() -> {
            requireManager(managerId);
            List<ProductRow> parsed = readRows(csv, errors);
            validateDatabase(parsed, mode, errors);
            return parsed;
        });
        int invalidRows = (int) errors.stream().map(CsvRowError::row).distinct().count();
        var result = new CsvValidationResponse(importId, rows.size(), rows.size() - invalidRows,
                invalidRows, errors.isEmpty(), errors);
        pending.put(importId, new PendingImport(managerId, mode, List.copyOf(rows), result,
                clock.instant().plus(IMPORT_TTL)));
        return result;
    }

    @Override
    @NotNull
    public synchronized CsvApplyResponse applyProducts(UUID managerId, UUID importId) {
        EntityValidation.requireNonNull(managerId, "Идентификатор менеджера");
        EntityValidation.requireNonNull(importId, "Идентификатор импорта");
        removeExpired();

        PendingImport upload = pending.get(importId);
        if (upload == null || !upload.ownerId.equals(managerId)) {
            throw new CsvImportNotFoundException("Загрузка не найдена или срок её хранения истёк.");
        }

        CsvApplyResponse result = transactions.execute(() -> {
            requireManager(managerId);
            if (!upload.validation.canApply()) {
                throw new CsvImportConflictException("CSV содержит ошибки. Исправьте файл и выполните новую проверку.");
            }
            if (upload.applied != null) {
                return upload.applied;
            }

            List<CsvRowError> currentErrors = new ArrayList<>();
            validateDatabase(upload.rows, upload.mode, currentErrors);
            if (!currentErrors.isEmpty()) {
                throw new CsvImportConflictException("Данные в БД изменились. Выполните новую проверку файла.");
            }

            int created = 0;
            int updated = 0;
            for (ProductRow row : upload.rows) {
                var category = row.categoryId() == null ? null : categories.getById(row.categoryId())
                        .orElseThrow(() -> new CsvImportConflictException("Категория удалена. Проверьте файл повторно."));
                var existing = products.getBySKU(row.sku());
                if (existing.isEmpty()) {
                    products.add(new Product(row.sku(), row.name(), category, row.price()));
                    created++;
                } else {
                    if (upload.mode == ImportMode.CREATE_ONLY) {
                        throw new CsvImportConflictException("Артикул уже существует. Проверьте файл повторно.");
                    }
                    Product product = products.getByIdForUpdate(existing.orElseThrow().getId())
                            .orElseThrow(() -> new CsvImportConflictException("Товар удалён. Проверьте файл повторно."));
                    if (!product.getSku().equals(row.sku())) {
                        throw new CsvImportConflictException("Артикул изменился. Проверьте файл повторно.");
                    }
                    product.setName(row.name());
                    product.setCategory(category);
                    product.setPrice(row.price());
                    products.update(product);
                    updated++;
                }
            }
            return new CsvApplyResponse(importId, created, updated);
        });
        upload.applied = result;
        return result;
    }

    private List<ProductRow> readRows(byte[] csv, List<CsvRowError> errors) {
        List<ProductRow> rows = new ArrayList<>();
        try (var parser = FORMAT.parse(new StringReader(decode(csv)))) {
            Set<String> headers = new HashSet<>(parser.getHeaderNames());
            if (!headers.containsAll(REQUIRED_HEADERS) || !ALLOWED_HEADERS.containsAll(headers)) {
                throw new IllegalArgumentException(
                        "Во время чтения были упущены необходимые столбцы (id (опц.),sku,name,category_id,price)."
                );
            }
            for (var record : parser) {
                int rowNumber = rows.size() + 1;
                if (rowNumber > MAX_ROWS) {
                    throw new IllegalArgumentException("В CSV допускается не более " + MAX_ROWS + " записей.");
                }
                if (!record.isConsistent()) {
                    errors.add(new CsvRowError(rowNumber, "$", "Число значений не совпадает с числом столбцов."));
                    rows.add(new ProductRow(rowNumber, null, null, null, null));
                    continue;
                }
                String sku = readValue(rowNumber, "sku", errors,
                        () -> EntityValidation.requireText(record.get("sku"), "Артикул"));
                String name = readValue(rowNumber, "name", errors,
                        () -> EntityValidation.requireText(record.get("name"), "Название товара"));
                UUID categoryId = readValue(rowNumber, "category_id", errors, () -> {
                    String text = record.get("category_id").strip();
                    if (text.isEmpty()) {
                        return null;
                    }
                    UUID id = UUID.fromString(text);
                    if (!id.toString().equalsIgnoreCase(text)) {
                        throw new IllegalArgumentException("Некорректный UUID категории.");
                    }
                    return id;
                });
                BigDecimal price = readValue(rowNumber, "price", errors, () -> {
                    String text = EntityValidation.requireText(record.get("price"), "Цена");
                    return EntityValidation.requireMoney(new BigDecimal(text), "Цена");
                });
                rows.add(new ProductRow(rowNumber, sku, name, categoryId, price));
            }
        } catch (IOException | UncheckedIOException exception) {
            throw new IllegalArgumentException("Некорректная структура CSV. Проверьте кавычки и разделители.", exception);
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV должен содержать хотя бы одну запись после заголовка.");
        }
        return rows;
    }

    private void validateDatabase(List<ProductRow> rows, ImportMode mode, List<CsvRowError> errors) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (ProductRow row : rows) {
            if (row.sku() != null) {
                frequencies.merge(row.sku(), 1, Integer::sum);
            }
        }
        for (ProductRow row : rows) {
            if (row.sku() != null) {
                if (frequencies.get(row.sku()) > 1) {
                    errors.add(new CsvRowError(row.number(), "sku", "Артикул повторяется в CSV."));
                }
                if (mode == ImportMode.CREATE_ONLY && products.getBySKU(row.sku()).isPresent()) {
                    errors.add(new CsvRowError(row.number(), "sku", "Товар с таким артикулом уже существует."));
                }
            }
            if (row.categoryId() != null && categories.getById(row.categoryId()).isEmpty()) {
                errors.add(new CsvRowError(row.number(), "category_id", "Категория не найдена."));
            }
        }
    }

    private static <T> T readValue(int row, String field, List<CsvRowError> errors, Supplier<T> parser) {
        try {
            return parser.get();
        } catch (IllegalArgumentException exception) {
            String message = switch (field) {
                case "category_id" -> "Укажите UUID категории или оставьте значение пустым.";
                case "price" -> "Укажите цену от 0 до 99999999.99, не более двух знаков после точки.";
                default -> exception.getMessage();
            };
            errors.add(new CsvRowError(row, field, message));
            return null;
        }
    }

    private static String decode(byte[] csv) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(csv)).toString();
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("CSV должен иметь кодировку UTF-8.", exception);
        }
    }

    private void requireManager(UUID managerId) {
        var manager = users.getById(managerId)
                .orElseThrow(() -> new CsvImportAccessDeniedException("Пользователь не найден."));
        if (!manager.isEnabled() || manager.getUserRole() != UserRole.MANAGER) {
            throw new CsvImportAccessDeniedException("Импорт доступен только активному менеджеру.");
        }
    }

    private void removeExpired() {
        Instant now = clock.instant();
        pending.entrySet().removeIf(entry -> !entry.getValue().expiresAt.isAfter(now));
    }

    private record ProductRow(int number, String sku, String name, UUID categoryId, BigDecimal price) {
    }

    private static final class PendingImport {
        private final UUID ownerId;
        private final ImportMode mode;
        private final List<ProductRow> rows;
        private final CsvValidationResponse validation;
        private final Instant expiresAt;
        private CsvApplyResponse applied;

        private PendingImport(UUID ownerId, ImportMode mode, List<ProductRow> rows,
                              CsvValidationResponse validation, Instant expiresAt) {
            this.ownerId = ownerId;
            this.mode = mode;
            this.rows = rows;
            this.validation = validation;
            this.expiresAt = expiresAt;
        }
    }
}

