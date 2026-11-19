package com.team.corporate.backend.repositories;

import com.team.corporate.core.dto.*;
import com.team.corporate.core.entities.OrderStatus;
import com.team.corporate.core.repositories.StatisticsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcStatisticsRepository implements StatisticsRepository {
    private final JdbcTemplate jdbc;

    public JdbcStatisticsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @NotNull
    public ProductsStatisticsResponse getProductStatistics() {
        return Objects.requireNonNull(jdbc.queryForObject("""
                SELECT COUNT(*) AS total,
                       COUNT(CASE WHEN p.deleted_at IS NULL THEN 1 END) AS active,
                       COUNT(CASE WHEN p.deleted_at IS NOT NULL THEN 1 END) AS archived,
                       COUNT(CASE WHEN p.deleted_at IS NULL AND EXISTS (
                           SELECT 1
                           FROM product_tags pt
                           JOIN tags t ON t.id = pt.tag_id AND t.deleted_at IS NULL
                           WHERE pt.product_id = p.id
                       ) THEN 1 END) AS with_tags,
                       AVG(CASE WHEN p.deleted_at IS NULL THEN p.price END) AS average_price
                FROM products p
                """, (rs, rowNum) -> new ProductsStatisticsResponse(
                rs.getLong("total"),
                rs.getLong("active"),
                rs.getLong("archived"),
                rs.getLong("with_tags"),
                rs.getBigDecimal("average_price")
        )));
    }

    @Override
    @NotNull
    public List<CategoryProductCountResponse> getProductsByCategory() {
        return jdbc.query("""
                SELECT p.category_id,
                       COALESCE(c.name, 'Без категории') AS category_name,
                       COUNT(*) AS product_count
                FROM products p
                LEFT JOIN categories c ON c.id = p.category_id
                WHERE p.deleted_at IS NULL
                GROUP BY p.category_id, c.name
                ORDER BY category_name, p.category_id
                """, (rs, rowNum) -> new CategoryProductCountResponse(
                rs.getObject("category_id", UUID.class),
                rs.getString("category_name"),
                rs.getLong("product_count")
        ));
    }

    @Override
    @NotNull
    public List<OrderStatusCountResponse> getOrdersByStatus() {
        return jdbc.query("""
                SELECT order_status, COUNT(*) AS order_count
                FROM orders
                WHERE deleted_at IS NULL
                GROUP BY order_status
                """, (rs, rowNum) -> new OrderStatusCountResponse(
                OrderStatus.valueOf(rs.getString("order_status")),
                rs.getLong("order_count")
        ));
    }

    @Override
    @NotNull
    public List<ProductStockCountResponse> getProductsByStock(int lowStockThreshold) {
        if (lowStockThreshold < 1) {
            throw new IllegalArgumentException("Порог низкого остатка должен быть не меньше 1.");
        }
        return jdbc.query("""
                SELECT classified.stock_level, COUNT(*) AS product_count
                FROM (
                    SELECT CASE
                               WHEN COALESCE(stock.quantity, 0) = 0 THEN 'OUT_OF_STOCK'
                               WHEN COALESCE(stock.quantity, 0) <= ? THEN 'LOW_STOCK'
                               ELSE 'IN_STOCK'
                           END AS stock_level
                    FROM products p
                    LEFT JOIN (
                        SELECT i.product_id, SUM(CAST(i.quantity AS BIGINT)) AS quantity
                        FROM inventory i
                        JOIN warehouses w ON w.id = i.warehouse_id AND w.deleted_at IS NULL
                        WHERE i.deleted_at IS NULL
                        GROUP BY i.product_id
                    ) stock ON stock.product_id = p.id
                    WHERE p.deleted_at IS NULL
                ) classified
                GROUP BY classified.stock_level
                """, (rs, rowNum) -> new ProductStockCountResponse(
                StockLevel.valueOf(rs.getString("stock_level")),
                rs.getLong("product_count")
        ), lowStockThreshold);
    }
}

