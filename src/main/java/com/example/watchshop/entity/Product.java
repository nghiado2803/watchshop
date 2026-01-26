package com.example.watchshop.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double price;
    private String imageUrl;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    private String brand;
    private Integer stock;
    private Integer discount;

    // --- CÁC CỘT MỚI ---
    @Column(name = "machine_type")
    private String machineType;

    @Column(name = "glass_material")
    private String glassMaterial;

    private String diameter;

    @Column(name = "water_resistance")
    private String waterResistance;

    // ===> BẠN ĐANG THIẾU DÒNG NÀY <===
    private String code;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "average_rating")
    private Double averageRating = 5.0;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // --- THÊM GETTER THỦ CÔNG CHO CHẮC ĂN ---
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    // (Lombok @Data sẽ lo các field còn lại, nhưng field mới thêm cứ viết rõ ra cho an toàn)
}