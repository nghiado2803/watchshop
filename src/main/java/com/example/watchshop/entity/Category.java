package com.example.watchshop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore; // Import cái này
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import java.util.List;

@Entity
@Table(name = "Categories")
@Data
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private List<Product> products;
}