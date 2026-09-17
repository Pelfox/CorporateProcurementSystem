package com.team.corporate;

import com.team.corporate.adapters.outbound.persistence.hibernate.HibernateUserRepository;
import com.team.corporate.util.HibernateConfiguration;
import com.team.corporate.interactions.ConsoleInteraction;
import com.team.corporate.interactions.InteractionAction;
import com.team.corporate.interactions.InteractionInterface;

import java.util.HashMap;
import java.util.function.Consumer;

public class Main {
    static void main() {
        HashMap<InteractionAction, Consumer<InteractionInterface>> executors = new HashMap<>();
        executors.put(InteractionAction.CREATE_NEW_ORDER, (interaction) -> {});

        var interaction = new ConsoleInteraction();

        interaction.printHelpMenu();

        InteractionAction nextAction = interaction.getNextAction();
        while (nextAction != null) {
            System.out.println(nextAction);
            nextAction = interaction.getNextAction();
        }

        HibernateUserRepository hur = new HibernateUserRepository(HibernateConfiguration.getSessionFactory());
//        User u = new User();
//        u.setUsername("test");
//        u.setUserRole(UserRole.USER);
//        hur.add(u);
        hur.getAll();
    }
}
