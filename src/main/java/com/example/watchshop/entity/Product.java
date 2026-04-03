package com.example.watchshop.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Entity
@Table(name = "Products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double price;

    // Ảnh đại diện (ảnh đầu tiên)
    private String imageUrl;

    // Lưu toàn bộ ảnh: "image1.jpg,image2.jpg,image3.jpg"
    @Column(name = "image_urls", columnDefinition = "NVARCHAR(MAX)")
    private String imageUrls;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    private String brand;
    private Integer stock;
    private Integer discount;

    @Column(name = "machine_type")
    private String machineType;

    @Column(name = "glass_material")
    private String glassMaterial;

    private String diameter;

    @Column(name = "water_resistance")
    private String waterResistance;

    private String code;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "average_rating")
    private Double averageRating = 5.0;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Column(name = "discount_start")
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime discountStart;

    @Column(name = "discount_end")
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime discountEnd;
}