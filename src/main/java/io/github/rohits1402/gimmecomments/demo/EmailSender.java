package io.github.rohits1402.gimmecomments.demo;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component @Primary
public class EmailSender implements MessageSender {
    @Override
    public void send(String to, String message) {
        System.out.println("EMAIL to " + to + ": " + message
                + "  [instance " + this.hashCode() + "]");
    }
}
