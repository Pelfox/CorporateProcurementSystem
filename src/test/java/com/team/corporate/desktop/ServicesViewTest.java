package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

@Disabled("Enable when ServicesView navigation is wired")
public class ServicesViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/main/ServicesView.fxml";
    }

    @Test
    @TestFx
    void shouldDisplayAllServiceCards() {
        FxAssert.verifyThat("#pageTitleLabel", node -> node.isVisible());
        Assertions.assertEquals(7,
                lookup("#servicesGrid").queryAs(GridPane.class).getChildren().size());
        FxAssert.verifyThat("#catalogCard", node -> node.isVisible());
        FxAssert.verifyThat("#ordersCard", node -> node.isVisible());
        FxAssert.verifyThat("#categoriesCard", node -> node.isVisible());
    }

    @Test
    @TestFx
    void shouldExposeNavigationTargetsForEveryService() {
        String[] cards = {
                "#catalogCard", "#ordersCard", "#categoriesCard",
                "#warehousesCard", "#statisticsCard", "#exportCard", "#usersCard"
        };
        for (String card : cards) {
            assertVisible(card);
        }
    }
}
