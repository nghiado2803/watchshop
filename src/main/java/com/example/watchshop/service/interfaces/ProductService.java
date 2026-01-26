package com.example.watchshop.service.interfaces;

import com.example.watchshop.entity.Product;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<Product> findAll();
    Optional<Product> findById(Long id);
    Product save(Product product);
    void deleteById(Long id);

    // Các hàm tìm kiếm cơ bản
    List<Product> searchProducts(String keyword);
    List<Product> findByCategory(Long categoryId);

    // Hàm tìm sản phẩm liên quan (cho trang chi tiết)
    List<Product> findRelatedProducts(Long categoryId, Long currentProductId);

    // --- HÀM QUAN TRỌNG: Lọc tổng hợp (Keyword + Hãng + Giá + Sắp xếp) ---
    List<Product> filterProducts(String keyword, Long categoryId, String priceRange, String sortStr);
}