package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

@Disabled("Enable after HeaderView.fxml is added")
public class HeaderViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/pages/HeaderView.fxml";
    }

    @Test
    @TestFx
    void shouldExposeAllNavigationTabs() {
        FxAssert.verifyThat("#myOrdersButton", node -> node.isVisible());
        FxAssert.verifyThat("#hubButton", node -> node.isVisible());
        FxAssert.verifyThat("#statisticsButton", node -> node.isVisible());
        FxAssert.verifyThat("#profileButton", node -> node.isVisible());
        FxAssert.verifyThat("#logoutButton", node -> node.isVisible());
    }

    @Test
    @TestFx
    void shouldMarkSelectedPageAsActive() {
        Button myOrdersButton = lookup("#myOrdersButton").queryAs(Button.class);
        Assertions.assertTrue(myOrdersButton.getStyleClass().contains("active-tab"));
        clickOn("#statisticsButton");
        Assertions.assertTrue(lookup("#statisticsButton").queryAs(Button.class)
                .getStyleClass().contains("active-tab"));
    }
}
