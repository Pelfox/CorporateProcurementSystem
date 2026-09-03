package com.team.corporate.interactions;

import org.jetbrains.annotations.Nullable;

public interface InteractionInterface {
    void printHelpMenu();

    // This method will return null when user wants to exit the program.
    @Nullable InteractionAction getNextAction();
}
