package com.example.watchshop.repository;

import com.example.watchshop.entity.ShopSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopSettingsRepository extends JpaRepository<ShopSettings, Integer> {
    // Không cần viết thêm hàm gì cả, vì ta chỉ dùng findById(1)
}