package com.example.watchshop.controller.api;

import com.example.watchshop.entity.Order;
import com.example.watchshop.entity.Product;
import com.example.watchshop.entity.Review;
import com.example.watchshop.entity.User;
import com.example.watchshop.repository.OrderRepository;
import com.example.watchshop.repository.ProductRepository;
import com.example.watchshop.repository.ReviewRepository;
import com.example.watchshop.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ApiReviewController {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @PostMapping("/add")
    public ResponseEntity<?> addReview(
            @RequestBody ReviewRequest request,
            @RequestParam(required = false) String email,
            Authentication auth) {

        if (request.getOrderId() == null || request.getProductId() == null) {
            return ResponseEntity.status(400).body(Map.of("message", "Mã đơn hàng hoặc mã sản phẩm không được để trống!"));
        }

        // ĐÃ SỬA LỖI: Ưu tiên lấy email truyền từ frontend lên để không bị dính ID của Google
        String targetEmail = (email != null && !email.isEmpty()) ? email : (auth != null ? auth.getName() : null);

        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập"));
        }

        // SỬA LỖI: Chuẩn hóa email bằng .trim().toLowerCase() để tìm chính xác trong DB
        User currentUser = userRepository.findByEmail(targetEmail.trim().toLowerCase()).orElse(null);

        if (currentUser == null) {
            return ResponseEntity.status(404).body(Map.of("message", "User không tồn tại"));
        }

        if (reviewRepository.existsByOrderIdAndProductId(request.getOrderId(), request.getProductId())) {
            return ResponseEntity.status(400).body(Map.of("message", "Bạn đã đánh giá sản phẩm này rồi!"));
        }

        Product product = productRepository.findById(request.getProductId()).orElse(null);
        Order order = orderRepository.findById(request.getOrderId()).orElse(null);

        if (product == null || order == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy SP hoặc Đơn hàng"));
        }

        Review review = new Review();
        review.setProduct(product);
        review.setOrder(order);
        review.setUser(currentUser);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreatedAt(LocalDateTime.now());

        reviewRepository.save(review);
        return ResponseEntity.ok(Map.of("message", "Đánh giá thành công!"));
    }

    @Data
    public static class ReviewRequest {
        private Long orderId;
        private Long productId;
        private int rating;
        private String comment;
    }
}