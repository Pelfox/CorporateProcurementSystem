package com.team.corporate.console;

import org.jetbrains.annotations.NotNull;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;

import java.io.IOException;

public final class ConsoleTerminal {
    private ConsoleTerminal() {
    }

    @NotNull
    public static Terminal open() throws IOException {
        var console = System.console();
        if (console == null || !console.isTerminal()) {
            return new DumbTerminal(System.in, System.out);
        }
        return TerminalBuilder.builder().system(true).dumb(true).build();
    }
}
