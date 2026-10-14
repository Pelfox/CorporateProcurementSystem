package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Assertions;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;
import org.testfx.matcher.control.LabeledMatchers;

public class MyOrdersViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/orders/MyOrdersView.fxml";
    }

    @TestFx
    void shouldDisplayWelcomeBannerFiltersAndOrdersTable() {
        FxAssert.verifyThat("#welcomeLabel", LabeledMatchers.hasText("Здравствуйте!"));
        FxAssert.verifyThat("#welcomeBanner", node -> node.isVisible());
        Assertions.assertEquals(5, lookup("#ordersTable").queryAs(TableView.class).getColumns().size());
        FxAssert.verifyThat("#statusFilter", node -> node.isVisible());
    }
}
