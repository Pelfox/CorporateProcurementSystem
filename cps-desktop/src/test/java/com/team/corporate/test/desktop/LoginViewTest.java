package com.team.corporate.test.desktop;

import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;

public class LoginViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/login/LoginView.fxml";
    }

    @Test
    @TestFx
    void shouldDisplayLoginForm() {
        FxAssert.verifyThat("#usernameField", node -> node.isVisible());
        FxAssert.verifyThat("#loginBtn", node -> node.isVisible());
    }

    @Test
    @TestFx
    void shouldDisplayErrorForUnknownUser() {
        clickOn("#usernameField").write("unknown-user");
        clickOn("#loginBtn");
        FxAssert.verifyThat("#loginError", node -> node.isVisible());
    }
}
