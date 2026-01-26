package com.example.watchshop.repository;

import com.example.watchshop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // Tìm kiếm theo tên
    List<Product> findByNameContaining(String keyword);

    // Lọc theo danh mục
    List<Product> findByCategoryId(Long categoryId);

    // 1. Tính tổng số lượng sản phẩm đang có trong kho
    // Dùng COALESCE để trả về 0 nếu kho trống, tránh lỗi null
    @Query("SELECT COALESCE(SUM(p.stock), 0) FROM Product p")
    Long sumTotalStock();

    // 2. Lấy 5 sản phẩm có giá cao nhất
    List<Product> findTop5ByOrderByPriceDesc();
}