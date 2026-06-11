package com.colectivo.admin.controller;

import com.colectivo.admin.dto.SupportConversationDto;
import com.colectivo.admin.dto.SupportMessageDto;
import com.colectivo.admin.dto.SupportSendMessageDto;
import com.colectivo.admin.service.SupportConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/support/conversations")
@RequiredArgsConstructor
public class SupportConversationController {

    private final SupportConversationService supportConversationService;

    @GetMapping
    public ResponseEntity<List<SupportConversationDto>> listOpen() {
        return ResponseEntity.ok(supportConversationService.listOpen());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupportConversationDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(supportConversationService.getById(id));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<SupportMessageDto>> getMessages(@PathVariable String id) {
        return ResponseEntity.ok(supportConversationService.getMessages(id));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<SupportMessageDto> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody SupportSendMessageDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(supportConversationService.sendMessage(id, dto.getContent(), authentication));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<SupportConversationDto> closeConversation(
            @PathVariable String id,
            Authentication authentication) {
        return ResponseEntity.ok(supportConversationService.closeConversation(id, authentication));
    }
}
