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

import java.util.List;
import java.util.Locale;

public class StatisticsController extends PageController {

    private final StatisticsService statisticsService;
    @FXML
    private Label usersCountLabel, productsCountLabel, categoriesCountLabel, warehousesCountLabel,
            ordersCountLabel, activeOrdersLabel, totalSumLabel;
    @FXML
    private PieChart statusPieChart;
    @FXML
    private BarChart<String, Number> stockBarChart;
    @FXML
    private CategoryAxis warehouseAxis;
    @FXML
    private NumberAxis quantityAxis;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @FXML
    public void initialize() {
        statusPieChart.setLegendVisible(true);
        statusPieChart.setLabelsVisible(false);
        stockBarChart.setLegendVisible(false);
        warehouseAxis.setLabel("Склад");
        quantityAxis.setLabel("Остаток, шт.");
        loadData();
    }

    private void loadData() {
        SystemStatistics stats = statisticsService.getStatistics();
        buildCards(stats);
        buildStatusChart(stats);
        buildWarehouseChart(statisticsService.getWarehouseStocks());
    }

    private void buildCards(SystemStatistics stats) {
        usersCountLabel.setText(String.valueOf(stats.totalUsers()));
        productsCountLabel.setText(String.valueOf(stats.totalProducts()));
        categoriesCountLabel.setText(String.valueOf(stats.totalCategories()));
        warehousesCountLabel.setText(String.valueOf(stats.totalWarehouses()));
        ordersCountLabel.setText(String.valueOf(stats.totalOrders()));
        activeOrdersLabel.setText(String.valueOf(stats.activeOrders()));
        totalSumLabel.setText(MoneyFormat.format(stats.totalOrderValue()) + " ₽");
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
    }

    private void buildWarehouseChart(List<StatisticsService.WarehouseStock> stocks) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Остаток");
        for (StatisticsService.WarehouseStock stock : stocks) {
            series.getData().add(new XYChart.Data<>(stock.warehouseName(), stock.quantity()));
        }
        stockBarChart.getData().setAll(List.of(series));
    }

    private record StatusSlice(OrderStatus status, long count) {
        String key() {
            return status().name().toLowerCase(Locale.ROOT);
        }
    }
}
