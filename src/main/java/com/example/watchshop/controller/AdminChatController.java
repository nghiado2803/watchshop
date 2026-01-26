package com.example.watchshop.controller;

import com.example.watchshop.entity.ChatSession;
import com.example.watchshop.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/chat")
@RequiredArgsConstructor
public class AdminChatController {

    private final ChatSessionRepository sessionRepo;

    @GetMapping
    public String chatPage(Model model) {
        // Lấy danh sách các cuộc hội thoại, sắp xếp mới nhất lên đầu
        List<ChatSession> sessions = sessionRepo.findAllOrderByUpdatedAtDesc();
        model.addAttribute("sessions", sessions);
        return "admin/chat"; // Trả về file HTML chat của bạn
    }
}