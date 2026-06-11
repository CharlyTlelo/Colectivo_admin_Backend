package com.colectivo.admin.repository;

import com.colectivo.admin.model.SupportConversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SupportConversationRepository extends MongoRepository<SupportConversation, String> {
    List<SupportConversation> findByStatusOrderByLastMessageAtDesc(String status);
}
