package com.colectivo.admin.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class SupportMessageDto {
    private String id;
    private String conversationId;
    private String senderId;
    private String alias;
    private String senderRole;
    private String content;
    private Instant sentAt;
}
