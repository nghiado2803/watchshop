package com.example.watchshop.repository;

import com.example.watchshop.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Lấy tất cả đánh giá của 1 sản phẩm, sắp xếp mới nhất lên đầu
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<Review> findByProductId(Long productId);

    // Kiểm tra xem trong Đơn hàng X, sản phẩm Y đã được đánh giá chưa?
    boolean existsByOrderIdAndProductId(Long orderId, Long productId);
}