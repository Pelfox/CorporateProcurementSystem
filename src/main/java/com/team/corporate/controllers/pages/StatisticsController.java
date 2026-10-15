package com.team.corporate.controllers.pages;

import com.team.corporate.controllers.utils.MoneyFormat;
import com.team.corporate.controllers.utils.OrderStatusDisplay;
import com.team.corporate.entities.OrderStatus;
import com.team.corporate.services.StatisticsService;
import com.team.corporate.services.SystemStatistics;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

public class StatisticsController {

    @FXML private TilePane statCardsPane;
    @FXML private PieChart statusPieChart;
    @FXML private VBox statusLegendBox;
    @FXML private BarChart<String, Number> warehouseStockChart;
    @FXML private CategoryAxis warehouseAxis;
    @FXML private NumberAxis stockAxis;

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @FXML
    public void initialize() {
        statusPieChart.setLegendVisible(false);
        statusPieChart.setLabelsVisible(false);
        warehouseStockChart.setLegendVisible(false);
        warehouseAxis.setLabel("Склад");
        stockAxis.setLabel("Остаток, шт.");
        loadData();
    }

    private void loadData() {
        SystemStatistics stats = statisticsService.getStatistics();
        buildCards(stats);
        buildStatusChart(stats);
        buildWarehouseChart(statisticsService.getWarehouseStocks());
    }

    private void buildCards(SystemStatistics stats) {
        statCardsPane.getChildren().setAll(
                statCard(String.valueOf(stats.totalUsers()), "Пользователей"),
                statCard(String.valueOf(stats.totalProducts()), "Товаров"),
                statCard(String.valueOf(stats.totalCategories()), "Категорий"),
                statCard(String.valueOf(stats.totalWarehouses()), "Складов"),
                statCard(String.valueOf(stats.totalOrders()), "Заказов"),
                statCard(String.valueOf(stats.activeOrders()), "Активных заказов"),   // метод рекорда
                statCard(MoneyFormat.format(stats.totalOrderValue()) + " ₽", "Общая стоимость"));
    }

    private VBox statCard(String value, String caption) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-card-value");
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("stat-card-caption");
        VBox card = new VBox(valueLabel, captionLabel);
        card.getStyleClass().add("stat-card");
        card.setPrefSize(170, 92);
        return card;
    }

    private void buildStatusChart(SystemStatistics stats) {
        List<StatusSlice> slices = List.of(
                new StatusSlice(OrderStatus.CREATED, stats.createdOrders()),
                new StatusSlice(OrderStatus.APPROVED, stats.approvedOrders()),
                new StatusSlice(OrderStatus.DELIVERED, stats.deliveredOrders()),
                new StatusSlice(OrderStatus.CANCELLED, stats.cancelledOrders()));

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        for (StatusSlice slice : slices) {
            PieChart.Data pie = new PieChart.Data(OrderStatusDisplay.name(slice.status()), slice.count());
            String styleClass = "pie-" + slice.key();
            String tooltipText = OrderStatusDisplay.name(slice.status()) + ": " + slice.count();
            pie.nodeProperty().addListener((obs, oldNode, node) -> {
                if (node != null) {
                    node.getStyleClass().add(styleClass);
                    Tooltip.install(node, new Tooltip(tooltipText));
                }
            });
            data.add(pie);
        }
        statusPieChart.setData(data);

        statusLegendBox.getChildren().setAll(slices.stream().map(this::legendRow).toList());
    }

    private HBox legendRow(StatusSlice slice) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll("legend-swatch", "pie-" + slice.key());
        Label label = new Label(OrderStatusDisplay.name(slice.status()) + " · " + slice.count());
        label.getStyleClass().add("legend-label");
        HBox row = new HBox(swatch, label);
        row.getStyleClass().add("status-legend-row");
        return row;
    }

    private void buildWarehouseChart(List<StatisticsService.WarehouseStock> stocks) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Остаток");
        for (StatisticsService.WarehouseStock stock : stocks) {
            series.getData().add(new XYChart.Data<>(stock.warehouseName(), stock.quantity()));
        }
        warehouseStockChart.getData().setAll(series);
    }

    private record StatusSlice(OrderStatus status, long count) {
        String key() { return status().name().toLowerCase(Locale.ROOT); }
    }
}
