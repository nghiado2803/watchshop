package com.example.watchshop.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String address;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Column(name = "total_price")
    private Double totalPrice;

    private String status; // "Chờ xác nhận", "Đang giao hàng", "Hoàn thành", "Đã hủy"

    @Column(name = "order_date")
    private LocalDateTime orderDate = LocalDateTime.now();

    // Quan hệ này BẮT BUỘC PHẢI CÓ để lấy danh sách sản phẩm đi trừ kho
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails;

    // Các trường thanh toán
    @Column(name = "payment_method")
    private String paymentMethod; // "Tiền mặt", "COD", "BANKING"...

    @Column(name = "payment_status")
    private String paymentStatus; // "Chưa thanh toán", "Đã thanh toán"
}