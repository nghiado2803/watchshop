package com.example.watchshop.controller;

import com.example.watchshop.entity.*;
import com.example.watchshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ApiChatController {

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    // --- CÁC HÀM CŨ (GIỮ NGUYÊN) ---

    // 1. Gửi tin nhắn (Text & Sản phẩm)
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestParam("sessionId") Long sessionId,
                                         @RequestParam("content") String content,
                                         @RequestParam(value = "productId", required = false) Long productId,
                                         Authentication auth) {
        User sender = userRepo.findByEmail(auth.getName()).orElseThrow();
        ChatSession session = sessionRepo.findById(sessionId).orElseThrow();

        ChatMessage msg = new ChatMessage();
        msg.setChatSession(session);
        msg.setSender(sender);
        msg.setRole(sender.getRole()); // ADMIN hoặc USER
        msg.setContent(content);

        if (productId != null) {
            Product p = productRepo.findById(productId).orElse(null);
            msg.setProduct(p);
        }

        msg.setCreatedAt(LocalDateTime.now());
        messageRepo.save(msg);

        // Update thời gian session để đẩy lên đầu danh sách của Admin
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepo.save(session);

        return ResponseEntity.ok(msg);
    }

    // 2. Gửi ảnh (Upload)
    @PostMapping("/upload")
    public ResponseEntity<?> sendImage(@RequestParam("sessionId") Long sessionId,
                                       @RequestParam("file") MultipartFile file,
                                       Authentication auth) throws IOException {
        User sender = userRepo.findByEmail(auth.getName()).orElseThrow();
        ChatSession session = sessionRepo.findById(sessionId).orElseThrow();

        // Lưu ảnh
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get("uploads");
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        ChatMessage msg = new ChatMessage();
        msg.setChatSession(session);
        msg.setSender(sender);
        msg.setRole(sender.getRole());
        msg.setImageUrl(fileName); // Lưu tên file
        msg.setCreatedAt(LocalDateTime.now());
        messageRepo.save(msg);

        session.setUpdatedAt(LocalDateTime.now());
        sessionRepo.save(session);

        return ResponseEntity.ok(msg);
    }

    // 3. Admin lấy tin nhắn của 1 session cụ thể
    @GetMapping("/messages/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable Long sessionId) {
        return ResponseEntity.ok(messageRepo.findByChatSessionIdOrderByCreatedAtAsc(sessionId));
    }

    // --- CÁC HÀM MỚI (BỔ SUNG CHO CLIENT) ---

    // 4. Client lấy tin nhắn của chính mình
    @GetMapping("/my-messages")
    public ResponseEntity<?> getMyMessages(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();

        // Tìm session chat của user này
        Optional<ChatSession> sessionOpt = sessionRepo.findByUserId(user.getId());

        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();
            // Lấy danh sách tin nhắn
            List<ChatMessage> messages = messageRepo.findByChatSessionIdOrderByCreatedAtAsc(session.getId());

            // Trả về cả ID session và list tin nhắn
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("messages", messages);

            return ResponseEntity.ok(response);
        } else {
            // Nếu chưa có session nào -> Trả về 404 để JS biết đường gọi hàm /start
            return ResponseEntity.notFound().build();
        }
    }

    // 5. Client bắt đầu cuộc trò chuyện mới (Tạo Session)
    @PostMapping("/start")
    public ResponseEntity<?> startChat(Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();

        // Kiểm tra xem đã có session chưa, nếu có rồi thì dùng lại
        Optional<ChatSession> existingSession = sessionRepo.findByUserId(user.getId());
        if(existingSession.isPresent()){
            return ResponseEntity.ok(existingSession.get());
        }

        // Nếu chưa có thì tạo mới
        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepo.save(session);

        return ResponseEntity.ok(session);
    }
}