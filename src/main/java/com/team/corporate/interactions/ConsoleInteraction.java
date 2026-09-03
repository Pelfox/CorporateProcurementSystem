package com.team.corporate.interactions;

import org.jetbrains.annotations.Nullable;

import java.util.Scanner;

public class ConsoleInteraction implements InteractionInterface {
    public static final int HEADER_WIDTH = 10;

    private final Scanner scanner;

    public ConsoleInteraction() {
        this.scanner = new Scanner(System.in);
    }

    private void printHeader() {
        var headerSeparator = "=".repeat(HEADER_WIDTH);
        System.out.printf("%s МЕНЮ %s\n", headerSeparator, headerSeparator);
    }

    @Override
    public void printHelpMenu() {
        this.printHeader();

        var actions = InteractionAction.values();
        for (int i = 0; i < actions.length; i++) {
            var currentAction = actions[i];
            System.out.printf("%d. %s\n", i + 1, currentAction.getVisibleName());
        }

        System.out.println("0. Выход из программы");
        this.printHeader();
    }

    @Nullable
    private InteractionAction actionFromCode(int code) {
        try {
            return InteractionAction.values()[code - 1];
        } catch (IndexOutOfBoundsException _) {
            return null;
        }
    }

    @Override
    public @Nullable InteractionAction getNextAction() {
        while (true) {
            System.out.print("Выберите действие: ");
            System.out.flush();

            String input = scanner.nextLine();
            try {
                int code = Integer.parseInt(input);
                if (code == 0) {
                    return null;
                }

                var action = actionFromCode(code);
                if (action != null) {
                    return action;
                }
            } catch (NumberFormatException _) {
            }

            System.out.printf("Некорректный выбор: %s%n", input);
        }
    }
}
