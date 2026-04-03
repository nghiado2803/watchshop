package com.example.watchshop.service.interfaces;

import com.example.watchshop.entity.Product;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<Product> findAll();
    Optional<Product> findById(Long id);
    Product save(Product product);
    void deleteById(Long id);

    List<Product> searchProducts(String keyword);
    List<Product> findByCategory(Long categoryId);

    List<Product> findRelatedProducts(Long categoryId, Long currentProductId);

    List<Product> filterProducts(String keyword, Long categoryId, String priceRange, String sortStr);
}