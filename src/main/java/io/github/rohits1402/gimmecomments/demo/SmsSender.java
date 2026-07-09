package io.github.rohits1402.gimmecomments.demo;

import org.springframework.stereotype.Component;

@Component
public class SmsSender implements MessageSender {
    @Override
    public void send(String to, String message) {
        System.out.println("SMS to " + to + ": " + message
                + "  [instance " + this.hashCode() + "]");
    }
}