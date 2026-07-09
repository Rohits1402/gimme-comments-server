package io.github.rohits1402.gimmecomments.demo;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final List<MessageSender> senders;

    public NotificationService(List<MessageSender> senders) {
        this.senders = senders;
    }

    public void notifyUser(String user, String text) {
        senders.forEach(s -> s.send(user, text));
    }
}
