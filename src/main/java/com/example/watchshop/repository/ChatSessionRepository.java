package com.example.watchshop.repository;

import com.example.watchshop.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findByUserId(Long userId);

    // Lấy danh sách session, sắp xếp theo tin nhắn mới nhất
    @Query("SELECT c FROM ChatSession c ORDER BY c.updatedAt DESC")
    List<ChatSession> findAllOrderByUpdatedAtDesc();
}