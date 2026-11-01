package com.team.corporate.desktop.controllers;

import com.team.corporate.core.config.ApplicationServices;
import com.team.corporate.desktop.controllers.dialog.DialogService;
import com.team.corporate.desktop.controllers.modals.*;
import com.team.corporate.desktop.controllers.pages.*;
import javafx.util.Callback;

import java.util.function.Supplier;

public final class ControllerFactory implements Callback<Class<?>, Object> {
    private final Supplier<ApplicationServices> services;
    private final DialogService dialogs = new DialogService(this);

    public ControllerFactory(Supplier<ApplicationServices> services) {
        this.services = services;
    }

    @Override
    public Object call(Class<?> type) {
        if (type == MainController.class) return new MainController();
        if (type == LoginController.class) return new LoginController();
        if (type == ServicesController.class) return new ServicesController();
        if (type == ErrorController.class) return new ErrorController();
        if (type == SuccessController.class) return new SuccessController();
        if (type == ConfirmDeleteController.class) return new ConfirmDeleteController();

        ApplicationServices app = services.get();
        if (type == MyOrdersController.class) return new MyOrdersController(app.orders(), app.items());
        if (type == CatalogController.class) return new CatalogController(app.products(), app.categories(), dialogs);
        if (type == CategoriesController.class)
            return new CategoriesController(app.categories(), app.products(), dialogs);
        if (type == WarehousesController.class)
            return new WarehousesController(app.warehouses(), app.inventories(), app.products(), dialogs);
        if (type == UsersController.class) return new UsersController(app.users(), dialogs);
        if (type == StatisticsController.class) return new StatisticsController(app.statistics());
        if (type == OrdersController.class)
            return new OrdersController(app.orders(), app.items(), app.users(), dialogs);
        if (type == ExportController.class) return new ExportController(app.csvExport());
        if (type == ProductEditorController.class) return new ProductEditorController(app.products(), app.categories());
        if (type == CategoryEditorController.class) return new CategoryEditorController(app.categories());
        if (type == WarehouseEditorController.class) return new WarehouseEditorController(app.warehouses());
        if (type == StockEditorController.class)
            return new StockEditorController(app.inventories(), app.warehouses(), app.products());
        if (type == UserEditorController.class) return new UserEditorController(app.users());
        if (type == OrderEditorController.class)
            return new OrderEditorController(app.orders(), app.products(), app.items(), app.stock());
        if (type == ChangeOrderStatusController.class) return new ChangeOrderStatusController(app.orders());
        throw new IllegalStateException("Фабрика контроллеров не настроена для: " + type.getName());
    }
}
