package com.example.watchshop.service.interfaces;

import com.example.watchshop.dto.CartItem;
import java.util.Collection;

public interface CartService {
    void add(Long productId, int quantity);
    void remove(Long productId);
    void update(Long productId, int quantity);
    void clear();
    Collection<CartItem> getCartItems(); // Lấy danh sách để hiển thị
    double getTotalAmount(); // Tính tổng tiền cả giỏ
    int getCount(); // Đếm tổng số lượng sản phẩm
}