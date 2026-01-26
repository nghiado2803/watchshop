package com.example.watchshop.controller;

import com.example.watchshop.entity.Order;
import com.example.watchshop.entity.Product;
import com.example.watchshop.entity.Review;
import com.example.watchshop.entity.User;
import com.example.watchshop.repository.OrderRepository;
import com.example.watchshop.repository.ProductRepository;
import com.example.watchshop.repository.ReviewRepository;
import com.example.watchshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @PostMapping("/add")
    public String addReview(@RequestParam("productId") Long productId,
                            @RequestParam("orderId") Long orderId,
                            @RequestParam("rating") int rating,
                            @RequestParam("comment") String comment,
                            Principal principal) {

        // 1. Kiểm tra đăng nhập
        if (principal == null) {
            return "redirect:/login";
        }
        String email = principal.getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow();

        // 2. Kiểm tra xem đã đánh giá chưa
        if (reviewRepository.existsByOrderIdAndProductId(orderId, productId)) {
            // [ĐÃ SỬA]: Đúng đường dẫn /orders/detail/
            return "redirect:/orders/detail/" + orderId + "?error=already_reviewed";
        }

        // 3. Lấy thông tin
        Product product = productRepository.findById(productId).orElseThrow();
        Order order = orderRepository.findById(orderId).orElseThrow();

        // 4. Tạo Review mới
        Review review = new Review();
        review.setProduct(product);
        review.setOrder(order);
        review.setUser(currentUser);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());

        // 5. Lưu xuống DB
        reviewRepository.save(review);

        // 6. Quay lại trang chi tiết đơn hàng
        // [ĐÃ SỬA]: Đúng đường dẫn /orders/detail/
        return "redirect:/orders/detail/" + orderId + "?success=review_added";
    }
}