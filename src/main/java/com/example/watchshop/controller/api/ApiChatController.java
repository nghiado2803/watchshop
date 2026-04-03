package com.example.watchshop.controller.api;

import com.example.watchshop.entity.*;
import com.example.watchshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}, allowCredentials = "true")
public class ApiChatController {

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    private String extractEmail(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            if (auth.getPrincipal() instanceof OAuth2User) {
                return ((OAuth2User) auth.getPrincipal()).getAttribute("email");
            }
            return auth.getName();
        }
        return null;
    }

    @GetMapping("/admin/chat/sessions")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getChatSessionsForAdmin() {
        List<ChatSession> allSessions = sessionRepo.findAllOrderByUpdatedAtDesc();
        List<Map<String, Object>> result = allSessions.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("userName", s.getUser() != null ? s.getUser().getFullName() : "Khách");
            if (s.getMessages() != null && !s.getMessages().isEmpty()) {
                m.put("lastMessage", s.getMessages().get(s.getMessages().size() - 1).getContent());
            } else {
                m.put("lastMessage", "Chưa có tin nhắn...");
            }
            m.put("time", s.getUpdatedAt() != null ? s.getUpdatedAt().toLocalTime().toString() : "");
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // 2. GỬI TIN NHẮN
    @PostMapping({"/admin/chat/send", "/chat/send"})
    @Transactional
    public ResponseEntity<?> sendMessage(@RequestParam("sessionId") Long sessionId,
                                         @RequestParam("content") String content,
                                         @RequestParam(value = "productId", required = false) Long productId,
                                         Authentication auth) {
        String email = extractEmail(auth);
        if (email == null) return ResponseEntity.status(401).body("Chưa đăng nhập");

        User sender = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
        ChatSession session = sessionRepo.findById(sessionId).orElseThrow();

        ChatMessage msg = new ChatMessage();
        msg.setChatSession(session);
        msg.setSender(sender);
        msg.setRole(sender.getRole());
        msg.setContent(content);

        if (productId != null) {
            Product p = productRepo.findById(productId).orElse(null);
            msg.setProduct(p);
        }
        msg.setCreatedAt(LocalDateTime.now());
        messageRepo.save(msg);

        session.setUpdatedAt(LocalDateTime.now());
        sessionRepo.save(session);
        return ResponseEntity.ok(convertToDto(msg));
    }

    @PostMapping({"/admin/chat/upload", "/chat/upload"})
    @Transactional
    public ResponseEntity<?> sendImage(@RequestParam("sessionId") Long sessionId,
                                       @RequestParam("file") MultipartFile file,
                                       Authentication auth) throws IOException {
        String email = extractEmail(auth);
        if (email == null) return ResponseEntity.status(401).body("Chưa đăng nhập");

        User sender = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
        ChatSession session = sessionRepo.findById(sessionId).orElseThrow();

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get("uploads");
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        ChatMessage msg = new ChatMessage();
        msg.setChatSession(session);
        msg.setSender(sender);
        msg.setRole(sender.getRole());
        msg.setImageUrl(fileName);
        msg.setCreatedAt(LocalDateTime.now());
        messageRepo.save(msg);

        session.setUpdatedAt(LocalDateTime.now());
        sessionRepo.save(session);
        return ResponseEntity.ok(convertToDto(msg));
    }

    @GetMapping({"/admin/chat/messages/{sessionId}", "/chat/messages/{sessionId}"})
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMessages(@PathVariable Long sessionId) {
        List<Map<String, Object>> msgs = messageRepo.findByChatSessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(this::convertToDto).collect(Collectors.toList());
        return ResponseEntity.ok(msgs);
    }

    @GetMapping("/chat/my-messages")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyMessages(Authentication auth) {
        String email = extractEmail(auth);
        if (email == null) return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));

        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy User trong DB"));

        Optional<ChatSession> sessionOpt = sessionRepo.findByUserId(user.getId());

        if (sessionOpt.isPresent()) {
            ChatSession session = sessionOpt.get();
            List<Map<String, Object>> messages = messageRepo.findByChatSessionIdOrderByCreatedAtAsc(session.getId())
                    .stream().map(this::convertToDto).collect(Collectors.toList());
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("messages", messages);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/chat/start")
    @Transactional
    public ResponseEntity<?> startChat(Authentication auth) {
        String email = extractEmail(auth);
        if (email == null) return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));

        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy User trong DB"));

        Optional<ChatSession> existingSession = sessionRepo.findByUserId(user.getId());
        if(existingSession.isPresent()) return ResponseEntity.ok(Map.of("sessionId", existingSession.get().getId()));

        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        ChatSession savedSession = sessionRepo.save(session);
        return ResponseEntity.ok(Map.of("sessionId", savedSession.getId()));
    }

    private Map<String, Object> convertToDto(ChatMessage msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", msg.getId());
        m.put("content", msg.getContent());
        m.put("imageUrl", msg.getImageUrl());
        m.put("isAdmin", "ROLE_ADMIN".equals(msg.getRole()) || "ADMIN".equals(msg.getRole()));
        m.put("senderName", msg.getSender() != null ? msg.getSender().getFullName() : "Unknown");
        m.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : "");

        if (msg.getProduct() != null) {
            Map<String, Object> pDto = new HashMap<>();
            pDto.put("id", msg.getProduct().getId());
            pDto.put("name", msg.getProduct().getName());
            pDto.put("price", msg.getProduct().getPrice());
            pDto.put("imageUrl", msg.getProduct().getImageUrl());
            m.put("product", pDto);
        }
        return m;
    }
}