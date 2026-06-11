package com.colectivo.admin.chat;

import com.colectivo.admin.dto.SupportMessageDto;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SupportChatStore {

    private static final Duration STREAM_TTL = Duration.ofDays(7);
    private static final long RECENT_LIMIT = 100L;

    private final StringRedisTemplate redis;

    public SupportChatStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static String key(String conversationId) {
        return "chat:support:" + conversationId;
    }

    public String append(String conversationId, String senderId, String alias, String senderRole,
                         String content, Instant sentAt) {
        return append(conversationId, senderId, alias, senderRole, content, sentAt, "text");
    }

    public String append(String conversationId, String senderId, String alias, String senderRole,
                         String content, Instant sentAt, String messageType) {
        String key = key(conversationId);
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("senderId", nullSafe(senderId));
        fields.put("alias", nullSafe(alias));
        fields.put("senderRole", nullSafe(senderRole));
        fields.put("content", nullSafe(content));
        fields.put("messageType", nullSafe(messageType != null ? messageType : "text"));
        fields.put("sentAt", sentAt != null ? sentAt.toString() : Instant.now().toString());

        MapRecord<String, String, String> record = StreamRecords.mapBacked(fields).withStreamKey(key);
        RecordId id = redis.opsForStream().add(record);
        redis.expire(key, STREAM_TTL);
        return id != null ? id.getValue() : null;
    }

    public List<SupportMessageDto> recent(String conversationId) {
        String key = key(conversationId);
        Boolean exists = redis.hasKey(key);
        if (exists == null || Boolean.FALSE.equals(exists)) return List.of();

        StreamOperations<String, Object, Object> ops = redis.opsForStream();
        List<MapRecord<String, Object, Object>> recordsDesc = ops.reverseRange(
                key,
                Range.unbounded(),
                Limit.limit().count((int) RECENT_LIMIT));

        if (recordsDesc == null || recordsDesc.isEmpty()) return List.of();

        java.util.ArrayList<SupportMessageDto> list = new java.util.ArrayList<>(recordsDesc.size());
        for (MapRecord<String, Object, Object> rec : recordsDesc) {
            list.add(toResponse(conversationId, rec));
        }
        java.util.Collections.reverse(list);
        return list;
    }

    public boolean purge(String conversationId) {
        Boolean removed = redis.delete(key(conversationId));
        return Boolean.TRUE.equals(removed);
    }

    private static SupportMessageDto toResponse(String conversationId, MapRecord<String, Object, Object> rec) {
        Map<Object, Object> values = rec.getValue();
        SupportMessageDto dto = new SupportMessageDto();
        dto.setId(rec.getId() != null ? rec.getId().getValue() : null);
        dto.setConversationId(conversationId);
        dto.setSenderId(asString(values.get("senderId")));
        dto.setAlias(asString(values.get("alias")));
        dto.setSenderRole(asString(values.get("senderRole")));
        dto.setContent(asString(values.get("content")));
        String messageType = asString(values.get("messageType"));
        dto.setMessageType(messageType != null && !messageType.isBlank() ? messageType : "text");
        String sentAt = asString(values.get("sentAt"));
        if (sentAt != null && !sentAt.isBlank()) {
            try { dto.setSentAt(Instant.parse(sentAt)); } catch (Exception ignore) {}
        }
        return dto;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
