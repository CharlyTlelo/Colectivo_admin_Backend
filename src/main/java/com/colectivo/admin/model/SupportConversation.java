package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "support_conversations")
public class SupportConversation {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String status;

    private String topic;
    private Instant createdAt;
    private Instant lastMessageAt;
    private Instant openedByAdminAt;
    private Instant closedAt;
    private String closedByAdmin;
    private String lastMessagePreview;
    private boolean hasUnreadAdmin;
    private boolean hasUnreadUser;
    private Instant closeRequestedAt;
    private String closeRequestedBy;
}
