package com.team.corporate.core.services.impls;

import com.team.corporate.core.entities.OrderStatus;
import com.team.corporate.core.entities.UserRole;
import com.team.corporate.core.exceptions.CsvImportException;
import com.team.corporate.core.services.CsvImportService;
import com.team.corporate.core.utils.CsvWriter;
import com.team.corporate.core.utils.EntityValidation;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.CharacterCodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import static com.team.corporate.core.exceptions.CsvImportException.Kind.CONFLICT;
import static com.team.corporate.core.exceptions.CsvImportException.Kind.INVALID_CSV;
import static com.team.corporate.core.services.impls.CsvImportServiceImpl.Column.*;

public final class CsvImportServiceImpl implements CsvImportService {
    private final SessionFactory factory;

    public CsvImportServiceImpl(SessionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Result importFiles(List<Source> files) throws IOException {
        if (files.isEmpty()) {
            throw invalidCsv("Добавьте хотя бы один CSV-файл.");
        }

        var input = new EnumMap<Table, List<Object[]>>(Table.class);
        for (var file : files) {
            Table table = Table.forFile(file.filename());
            if (input.containsKey(table)) {
                throw invalidCsv("Файл " + table.filename + " передан несколько раз.");
            }
            input.put(table, read(file, table));
        }

        return factory.fromTransaction(session ->
                session.doReturningWork(connection -> importRows(connection, input)));
    }

    private static Result importRows(Connection connection, Map<Table, List<Object[]>> input) throws SQLException {
        var affectedProducts = new TreeSet<UUID>();
        var counts = new LinkedHashMap<String, Integer>();
        int total = 0;
        for (var entry : input.entrySet()) {
            Table table = entry.getKey();
            List<Object[]> rows = entry.getValue();
            if (table == Table.ORDER_ITEMS) {
                lockOrderItemReferences(connection, rows, affectedProducts);
            }
            insert(connection, table, rows);
            counts.put(table.filename, rows.size());
            total += rows.size();
        }
        validateReservations(connection, affectedProducts);
        return new Result(counts, total);
    }

    private static List<Object[]> read(Source file, Table table) throws IOException {
        var rows = new ArrayList<Object[]>();
        try (var reader = new CsvReader(file.content().open())) {
            if (!table.headers.equals(reader.readRecord())) {
                throw invalidCsv(table.filename + ": ожидается заголовок " + String.join(",", table.headers) + ".");
            }

            for (List<String> fields = reader.readRecord(); fields != null; fields = reader.readRecord()) {
                if (fields.size() != table.headers.size()) {
                    throw new IllegalArgumentException("Количество полей должно совпадать с заголовком.");
                }
                var row = new Object[fields.size()];
                for (int i = 0; i < row.length; i++) {
                    String column = table.headers.get(i);
                    try {
                        row[i] = table.columns[i].parse(fields.get(i), column);
                    } catch (IllegalArgumentException | DateTimeParseException e) {
                        throw new IllegalArgumentException("Некорректное значение поля «" + column + "».", e);
                    }
                }
                rows.add(row);
            }
        } catch (CharacterCodingException e) {
            throw invalidCsv(table.filename + ": ожидается текст в кодировке UTF-8.");
        } catch (IllegalArgumentException e) {
            throw invalidCsv(recordLocation(table, rows.size()) + e.getMessage());
        }
        return rows;
    }

    private static void insert(Connection connection, Table table, List<Object[]> rows) throws SQLException {
        try (var statement = connection.prepareStatement(table.insertSql)) {
            for (int i = 0; i < rows.size(); i++) {
                Object[] row = rows.get(i);
                for (int column = 0; column < row.length; column++) {
                    statement.setObject(column + 1, row[column]);
                }
                try {
                    statement.executeUpdate();
                } catch (SQLException e) {
                    String state = e.getSQLState();
                    if (state != null && (state.startsWith("23") || state.startsWith("22"))) {
                        throw new CsvImportException(CONFLICT, recordLocation(table, i)
                                + "Запись конфликтует с существующими данными или ссылается на отсутствующую запись.", e);
                    }
                    throw e;
                }
            }
        }
    }

    private static void lockOrderItemReferences(Connection connection, List<Object[]> rows,
                                                TreeSet<UUID> productIds) throws SQLException {
        var orderIds = new TreeSet<UUID>();
        for (Object[] row : rows) {
            orderIds.add((UUID) row[1]);
            productIds.add((UUID) row[2]);
        }
        // Order mutations lock the order first, then its products in UUID order.
        lockRows(connection, "select id from orders where id = ? for update", orderIds);
        lockRows(connection, "select id from products where id = ? for update", productIds);
    }

    private static void lockRows(Connection connection, String sql, Collection<UUID> ids) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (UUID id : ids) {
                statement.setObject(1, id);
                statement.executeQuery().close();
            }
        }
    }

    private static void validateReservations(Connection connection, Collection<UUID> productIds) throws SQLException {
        try (var statement = connection.prepareStatement("""
                select coalesce((select sum(quantity) from inventory where product_id = ?), 0)
                     - coalesce((select sum(i.quantity) from order_items i
                                 join orders o on o.id = i.order_id
                                 where i.product_id = ? and o.order_status in ('CREATED', 'APPROVED')), 0)
                """)) {
            for (UUID id : productIds) {
                statement.setObject(1, id);
                statement.setObject(2, id);
                try (var result = statement.executeQuery()) {
                    if (result.next() && result.getLong(1) < 0) {
                        throw new CsvImportException(CONFLICT,
                                "Недостаточно товара " + id + " на складах для активных заказов.");
                    }
                }
            }
        }
    }

    private static String recordLocation(Table table, int rowIndex) {
        return table.filename + ", запись " + (rowIndex + 2) + ": ";
    }

    private static CsvImportException invalidCsv(String message) {
        return new CsvImportException(INVALID_CSV, message);
    }

    private enum Table {
        USERS("users", "id,username,user_role,created_at", ID, TEXT, ROLE, TIMESTAMP),
        CATEGORIES("categories", "id,name", ID, TEXT),
        WAREHOUSES("warehouses", "id,name,location", ID, TEXT, OPTIONAL_TEXT),
        PRODUCTS("products", "id,sku,name,category_id,price", ID, TEXT, TEXT, OPTIONAL_ID, MONEY),
        ORDERS("orders", "id,user_id,order_status,notes,created_at,updated_at",
                ID, ID, STATUS, OPTIONAL_TEXT, TIMESTAMP, TIMESTAMP),
        ORDER_ITEMS("order_items", "id,order_id,product_id,quantity,purchase_price",
                ID, ID, ID, ORDER_QUANTITY, MONEY),
        INVENTORIES("inventories.csv", "inventory", "id,product_id,warehouse_id,quantity",
                ID, ID, ID, STOCK_QUANTITY),
        AUDIT_LOGS("audit_logs", "id,order_id,changed_by,old_status,new_status,changed_at",
                ID, ID, ID, STATUS, STATUS, TIMESTAMP);

        private final String filename;
        private final List<String> headers;
        private final Column[] columns;
        private final String insertSql;

        Table(String table, String header, Column... columns) {
            this(table + ".csv", table, header, columns);
        }

        Table(String filename, String table, String header, Column... columns) {
            this.filename = filename;
            this.headers = List.of(header.split(","));
            this.columns = columns;
            String placeholders = String.join(", ", Collections.nCopies(columns.length, "?"));
            this.insertSql = "insert into " + table + " (" + header + ") values (" + placeholders + ")";
        }

        static Table forFile(String filename) {
            for (Table table : values()) {
                if (table.filename.equals(filename)) {
                    return table;
                }
            }
            throw invalidCsv("Имя файла должно совпадать с именем одного из CSV в экспорте.");
        }
    }

    enum Column {
        ID, OPTIONAL_ID, TEXT, OPTIONAL_TEXT, ROLE, STATUS, TIMESTAMP, MONEY, STOCK_QUANTITY, ORDER_QUANTITY;

        Object parse(String value, String column) {
            return switch (this) {
                case ID -> uuid(value);
                case OPTIONAL_ID -> value.isEmpty() ? null : uuid(value);
                case TEXT -> EntityValidation.requireText(CsvWriter.restoreTextWithinLimit(value), column);
                case OPTIONAL_TEXT -> value.isEmpty() ? null
                        : EntityValidation.optionalText(CsvWriter.restoreTextWithinLimit(value), column);
                case ROLE -> UserRole.valueOf(value).name();
                case STATUS -> OrderStatus.valueOf(value).name();
                case TIMESTAMP -> OffsetDateTime.parse(value);
                case MONEY -> EntityValidation.requireMoney(new BigDecimal(value), column);
                case STOCK_QUANTITY -> EntityValidation.requireAtLeast(Integer.parseInt(value), 0, column);
                case ORDER_QUANTITY -> EntityValidation.requireOrderQuantity(Integer.parseInt(value));
            };
        }

        private static UUID uuid(String value) {
            UUID id = UUID.fromString(value);
            if (!id.toString().equalsIgnoreCase(value)) {
                throw new IllegalArgumentException("Некорректный UUID.");
            }
            return id;
        }
    }
}
