package com.example.watchshop.repository;

import com.example.watchshop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByNameContaining(String keyword);

    List<Product> findByCategoryId(Long categoryId);


    @Query("SELECT COALESCE(SUM(p.stock), 0) FROM Product p")
    Long sumTotalStock();

    List<Product> findTop5ByOrderByPriceDesc();
}