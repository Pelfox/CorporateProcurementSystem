package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;
import org.testfx.matcher.control.LabeledMatchers;

public class MainViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/MainView.fxml";
    }

    @TestFx
    void shouldDisplayApplicationShellAndDefaultNavigation() {
        FxAssert.verifyThat("#appTitleLabel", LabeledMatchers.hasText("СКЗ АРГУС"));
        Assertions.assertTrue(lookup("#myOrdersNavBtn").queryAs(ToggleButton.class).isSelected());
        Assertions.assertFalse(lookup("#servicesNavBtn").queryAs(ToggleButton.class).isSelected());
        FxAssert.verifyThat("#usernameLabel", LabeledMatchers.hasText("User"));
    }
}
