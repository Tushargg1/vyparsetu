package com.vyaparsetu.notification.entity;

import com.vyaparsetu.common.enums.Enums;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enums.NotificationChannel channel = Enums.NotificationChannel.IN_APP;

    @Column(nullable = false)
    private String title;

    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_json")
    private String dataJson;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
