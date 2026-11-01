package com.team.corporate.desktop.controllers.pages;

import javafx.fxml.FXML;

import java.util.Objects;

public abstract class PageController {
    private Runnable onBack;

    public final void setOnBack(Runnable onBack) {
        this.onBack = Objects.requireNonNull(onBack);
    }

    @FXML
    private void onBackClick() {
        if (onBack != null) onBack.run();
    }
}
