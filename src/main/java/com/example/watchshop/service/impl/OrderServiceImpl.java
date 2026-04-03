package com.example.watchshop.service.impl;

import com.example.watchshop.dto.CartItem;
import com.example.watchshop.entity.Order;
import com.example.watchshop.entity.OrderDetail;
import com.example.watchshop.entity.Product;
import com.example.watchshop.repository.OrderDetailRepository;
import com.example.watchshop.repository.OrderRepository;
import com.example.watchshop.repository.ProductRepository;
import com.example.watchshop.service.interfaces.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    @Transactional
    public Order save(Order order, List<CartItem> items) {
        Order savedOrder = orderRepository.save(order);

        for (CartItem item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setOrder(savedOrder);
            detail.setPrice(item.getPrice());
            detail.setQuantity(item.getQuantity());

            Product product = productRepository.findById(item.getProductId()).orElse(null);
            detail.setProduct(product);

            orderDetailRepository.save(detail); // Lưu chi tiết
        }

        return savedOrder;
    }

    @Override
    public List<Order> findOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}