package com.example.watchshop.service.impl;

import com.example.watchshop.dto.CartItem;
import com.example.watchshop.entity.Product;
import com.example.watchshop.repository.ProductRepository;
import com.example.watchshop.service.interfaces.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@SessionScope
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final ProductRepository productRepository;

    private Map<Long, CartItem> maps = new HashMap<>();

    @Override
    public void add(Long productId, int quantity) {
        CartItem item = maps.get(productId);

        if (item == null) {
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isPresent()) {
                Product p = productOpt.get();
                item = new CartItem(p.getId(), p.getName(), p.getPrice(), quantity, p.getImageUrl());
                maps.put(productId, item);
            }
        } else {
            item.setQuantity(item.getQuantity() + quantity);
        }
    }

    @Override
    public void remove(Long productId) {
        maps.remove(productId);
    }

    @Override
    public void update(Long productId, int quantity) {
        CartItem item = maps.get(productId);
        if (item != null) {
            if (quantity > 0) {
                item.setQuantity(quantity);
            } else {
                maps.remove(productId);
            }
        }
    }

    @Override
    public void clear() {
        maps.clear();
    }

    @Override
    public Collection<CartItem> getCartItems() {
        return maps.values();
    }

    @Override
    public double getTotalAmount() {
        return maps.values().stream()
                .mapToDouble(CartItem::getTotalPrice)
                .sum();
    }

    @Override
    public int getCount() {
        return maps.values().size();
    }
}