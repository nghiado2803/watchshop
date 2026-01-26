package com.example.watchshop.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Reviews")
@Data
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int rating; // 1 đến 5 sao

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // 1. Liên kết với User (Ai đánh giá)
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // 2. Liên kết với Product (Đánh giá sản phẩm nào)
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // 3. Liên kết với Order (Thuộc đơn hàng nào - QUAN TRỌNG để chặn spam)
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}