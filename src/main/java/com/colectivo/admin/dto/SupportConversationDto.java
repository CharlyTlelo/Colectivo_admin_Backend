package com.colectivo.admin.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class SupportConversationDto {
    private String id;
    private String userId;
    private String userName;
    private String userPhone;
    private String topic;
    private String status;
    private Instant createdAt;
    private Instant lastMessageAt;
    private Instant openedByAdminAt;
    private Instant closedAt;
    private String lastMessagePreview;
    private boolean hasUnreadAdmin;
    private boolean hasUnreadUser;
    private Instant closeRequestedAt;
    private String closeRequestedBy;
}
