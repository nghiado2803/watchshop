package com.example.watchshop.service.interfaces;

import com.example.watchshop.dto.CartItem;
import com.example.watchshop.entity.Order;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    List<Order> findAll();
    Optional<Order> findById(Long id);
    Order save(Order order, List<CartItem> items); // Đặt hàng
    List<Order> findOrdersByUserId(Long userId); // Lịch sử mua hàng
}