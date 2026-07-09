package io.github.rohits1402.gimmecomments.demo;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoRunner implements CommandLineRunner {

    private final NotificationService notificationService;
    private final MessageSender directSender;

    public DemoRunner(NotificationService notificationService, @Qualifier("smsSender") MessageSender directSender) {
        this.notificationService = notificationService;
        this.directSender = directSender;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== DI DEMO ===");
        notificationService.notifyUser("rohit", "Welcome to DI!");
        directSender.send("rohit", "Injected directly into the runner");
    }
}
