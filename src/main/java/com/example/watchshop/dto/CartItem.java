package com.example.watchshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {
    private Long productId;
    private String name;
    private Double price;
    private int quantity;
    private String imageUrl;

    // Logic tính thành tiền của item này (Giá * Số lượng)
    public Double getTotalPrice() {
        return price * quantity;
    }
}