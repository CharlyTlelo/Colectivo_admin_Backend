package com.colectivo.admin.service;

import com.colectivo.admin.chat.SupportChatStore;
import com.colectivo.admin.dto.SupportConversationDto;
import com.colectivo.admin.dto.SupportMessageDto;
import com.colectivo.admin.model.SupportConversation;
import com.colectivo.admin.model.User;
import com.colectivo.admin.repository.SupportConversationRepository;
import com.colectivo.admin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportConversationService {

    private static final String STATUS_OPEN = "open";
    private static final String STATUS_CLOSED = "closed";
    private static final String ADMIN_ALIAS = "Soporte Colectivo";

    private final SupportConversationRepository supportConversationRepository;
    private final UserRepository userRepository;
    private final SupportChatStore supportChatStore;
    private final UserNotificationService userNotificationService;
    private final Clock clock;

    public List<SupportConversationDto> listOpen() {
        List<SupportConversation> conversations = supportConversationRepository.findByStatusOrderByLastMessageAtDesc(STATUS_OPEN);
        Map<String, User> usersById = userRepository.findAllById(
                conversations.stream().map(SupportConversation::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return conversations.stream()
                .map(conversation -> toDto(conversation, usersById.get(conversation.getUserId())))
                .toList();
    }

    public SupportConversationDto getById(String id) {
        SupportConversation conversation = requireConversation(id);
        if (STATUS_OPEN.equals(conversation.getStatus())) {
            conversation.setOpenedByAdminAt(Instant.now(clock));
            conversation.setHasUnreadAdmin(false);
            conversation = supportConversationRepository.save(conversation);
        }
        return toDto(conversation, findUser(conversation.getUserId()));
    }

    public List<SupportMessageDto> getMessages(String id) {
        SupportConversation conversation = requireConversation(id);
        requireOpenConversation(conversation);
        return supportChatStore.recent(id);
    }

    public SupportMessageDto sendMessage(String id, String content, Authentication authentication) {
        SupportConversation conversation = requireConversation(id);
        requireOpenConversation(conversation);

        Instant now = Instant.now(clock);
        String adminId = authentication != null ? authentication.getName() : "admin";
        String trimmed = content.trim();
        String messageId = supportChatStore.append(id, adminId, ADMIN_ALIAS, "admin", trimmed, now);

        conversation.setOpenedByAdminAt(now);
        conversation.setLastMessageAt(now);
        conversation.setLastMessagePreview(truncate(trimmed, 120));
        conversation.setHasUnreadAdmin(false);
        conversation.setHasUnreadUser(true);
        supportConversationRepository.save(conversation);

        userNotificationService.notifySupportReply(conversation.getUserId(), conversation.getId());

        SupportMessageDto dto = new SupportMessageDto();
        dto.setId(messageId);
        dto.setConversationId(id);
        dto.setSenderId(adminId);
        dto.setAlias(ADMIN_ALIAS);
        dto.setSenderRole("admin");
        dto.setContent(trimmed);
        dto.setSentAt(now);
        return dto;
    }

    public SupportConversationDto closeConversation(String id, Authentication authentication) {
        SupportConversation conversation = requireConversation(id);
        requireOpenConversation(conversation);

        Instant now = Instant.now(clock);
        conversation.setStatus(STATUS_CLOSED);
        conversation.setClosedAt(now);
        conversation.setClosedByAdmin(authentication != null ? authentication.getName() : "admin");
        conversation.setHasUnreadAdmin(false);
        conversation.setHasUnreadUser(false);
        supportConversationRepository.save(conversation);
        supportChatStore.purge(id);
        userNotificationService.notifySupportClosed(conversation.getUserId(), conversation.getId());
        return toDto(conversation, findUser(conversation.getUserId()));
    }

    private SupportConversation requireConversation(String id) {
        return supportConversationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversacion no encontrada."));
    }

    private void requireOpenConversation(SupportConversation conversation) {
        if (!STATUS_OPEN.equals(conversation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.GONE, "La conversacion ya fue cerrada.");
        }
    }

    private User findUser(String userId) {
        return userRepository.findById(userId).orElse(null);
    }

    private SupportConversationDto toDto(SupportConversation conversation, User user) {
        SupportConversationDto dto = new SupportConversationDto();
        dto.setId(conversation.getId());
        dto.setUserId(conversation.getUserId());
        dto.setUserName(user != null ? user.getName() : "Usuario");
        dto.setUserPhone(user != null ? user.getPhone() : "");
        dto.setTopic(conversation.getTopic());
        dto.setStatus(conversation.getStatus());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setOpenedByAdminAt(conversation.getOpenedByAdminAt());
        dto.setClosedAt(conversation.getClosedAt());
        dto.setLastMessagePreview(conversation.getLastMessagePreview());
        dto.setHasUnreadAdmin(conversation.isHasUnreadAdmin());
        dto.setHasUnreadUser(conversation.isHasUnreadUser());
        return dto;
    }

    private String truncate(String value, int max) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max - 1) + "...";
    }
}
