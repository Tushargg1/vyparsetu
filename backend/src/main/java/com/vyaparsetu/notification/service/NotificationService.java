package com.vyaparsetu.notification.service;

import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.notification.entity.Notification;
import com.vyaparsetu.notification.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Enums.NotificationChannel, NotificationChannelSender> senders;

    public NotificationService(NotificationRepository repository,
                               SimpMessagingTemplate messagingTemplate,
                               List<NotificationChannelSender> senderList) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
        this.senders = senderList.stream()
                .collect(Collectors.toMap(NotificationChannelSender::channel, Function.identity()));
    }

    @Transactional
    public Notification notify(Long userId, Enums.NotificationType type, String title, String body) {
        return notify(userId, type, Enums.NotificationChannel.IN_APP, title, body, null);
    }

    @Transactional
    public Notification notify(Long userId, Enums.NotificationType type, Enums.NotificationChannel channel,
                               String title, String body, String dataJson) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setChannel(channel);
        n.setTitle(title);
        n.setBody(body);
        n.setDataJson(dataJson);
        n = repository.save(n);

        // always push in-app via websocket
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, n);

        // dispatch to external channel if requested
        NotificationChannelSender sender = senders.get(channel);
        if (sender != null && channel != Enums.NotificationChannel.IN_APP) {
            sender.send(String.valueOf(userId), title, body);
        }
        return n;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Notification> list(Long userId,
                                                                   org.springframework.data.domain.Pageable pageable) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        repository.findById(id)
                .filter(n -> n.getUserId().equals(userId)) // SECURITY: only the owner may mark read
                .ifPresent(n -> {
                    n.setReadAt(java.time.Instant.now());
                    repository.save(n);
                });
    }
}
