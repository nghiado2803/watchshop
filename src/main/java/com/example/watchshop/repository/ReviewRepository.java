package com.example.watchshop.repository;

import com.example.watchshop.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<Review> findByProductId(Long productId);

    boolean existsByOrderIdAndProductId(Long orderId, Long productId);
}