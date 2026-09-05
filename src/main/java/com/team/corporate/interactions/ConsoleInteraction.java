package com.team.corporate.interactions;

import com.team.corporate.interactions.commands.CommandInterface;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ConsoleInteraction implements InteractionInterface {
    public static final int HEADER_WIDTH = 10;
    private Map<InteractionAction, CommandInterface> menuCommands = new HashMap<>();

    private final Scanner scanner;

    public ConsoleInteraction() {
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        printHelpMenu();

        while (true) {
            System.out.print("Выберите действие: ");
            System.out.flush();

            String input = scanner.nextLine();
            try {
                int code = Integer.parseInt(input);
                if (code == 0) {
                    break;
                }

                var action = actionFromCode(code);
                if (action != null) {
                    var command = menuCommands.get(action);
                    if (command != null) {
                        command.execute();
                    }
                }
                else {
                    System.out.printf("Некорректный выбор: %s%n", input);
                }
            } catch (NumberFormatException _) {
                System.out.printf("Некорректный ввод: '%s'. Ожидалось число.%n", input);
            }

        }
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

    public void registerCommand(InteractionAction action, CommandInterface command) {
        menuCommands.put(action, command);
    }

    @Nullable
    private InteractionAction actionFromCode(int code) {
        try {
            return InteractionAction.values()[code - 1];
        } catch (IndexOutOfBoundsException _) {
            return null;
        }
    }
}
