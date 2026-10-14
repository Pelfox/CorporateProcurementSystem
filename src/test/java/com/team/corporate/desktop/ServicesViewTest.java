package com.team.corporate.desktop;

import com.team.corporate.BaseUITest;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.Assertions;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.TestFx;
import org.testfx.matcher.control.LabeledMatchers;

public class ServicesViewTest extends BaseUITest {
    @Override
    protected String viewResource() {
        return "/fxml/main/ServicesView.fxml";
    }

    @TestFx
    void shouldDisplayAllServiceCards() {
        FxAssert.verifyThat("#pageTitleLabel", LabeledMatchers.hasText("Сервисы"));
        GridPane grid = lookup("#servicesGrid").queryAs(GridPane.class);
        Assertions.assertEquals(7, grid.getChildren().size());
        FxAssert.verifyThat("#catalogTitleLabel", LabeledMatchers.hasText("Каталог товаров"));
        FxAssert.verifyThat("#usersTitleLabel", LabeledMatchers.hasText("Пользователи"));
    }
}
