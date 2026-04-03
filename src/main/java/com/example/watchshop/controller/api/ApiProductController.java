package com.example.watchshop.controller.api;

import com.example.watchshop.entity.Product;
import com.example.watchshop.entity.Review;
import com.example.watchshop.repository.ReviewRepository;
import com.example.watchshop.service.interfaces.ProductService;
import com.example.watchshop.service.interfaces.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ApiProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ReviewRepository reviewRepository;

    @GetMapping("/best-sellers")
    public ResponseEntity<List<Product>> getBestSellers() {
        List<Product> products = productService.findAll();
        int limit = Math.min(products.size(), 4);
        return ResponseEntity.ok(products.subList(0, limit));
    }

    @GetMapping("")
    public ResponseEntity<?> listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) String sort) {

        List<Product> list = productService.filterProducts(keyword, categoryId, priceRange, sort);

        Map<String, Object> response = new HashMap<>();
        response.put("products", list);
        response.put("categories", categoryService.findAll());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductDetail(@PathVariable Long id) {
        Product product = productService.findById(id).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();

        Map<String, Object> pMap = new HashMap<>();
        pMap.put("id", product.getId());
        pMap.put("name", product.getName());
        pMap.put("brand", product.getBrand());
        pMap.put("price", product.getPrice());
        pMap.put("discount", product.getDiscount());
        pMap.put("discountStart", product.getDiscountStart() != null ? product.getDiscountStart().toString() : null);
        pMap.put("discountEnd", product.getDiscountEnd() != null ? product.getDiscountEnd().toString() : null);
        pMap.put("stock", product.getStock());
        pMap.put("imageUrl", product.getImageUrl());
        pMap.put("imageUrls", product.getImageUrls());
        pMap.put("description", product.getDescription());
        pMap.put("machineType", product.getMachineType());
        pMap.put("glassMaterial", product.getGlassMaterial());
        pMap.put("diameter", product.getDiameter());
        pMap.put("waterResistance", product.getWaterResistance());
        pMap.put("code", product.getCode());

        if (product.getCategory() != null) {
            pMap.put("categoryName", product.getCategory().getName());
        }

        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(id);

        int totalReviews = reviews.size();
        double avgRating = totalReviews > 0 ? reviews.stream().mapToInt(Review::getRating).sum() / (double) totalReviews : 5.0;
        int roundedRating = (int) Math.round(avgRating);

        Map<String, Object> response = new HashMap<>();
        response.put("product", pMap);
        response.put("reviews", reviews.stream().map(rv -> {
            Map<String, Object> rMap = new HashMap<>();
            rMap.put("id", rv.getId());
            rMap.put("comment", rv.getComment());
            rMap.put("rating", rv.getRating());
            rMap.put("createdAt", rv.getCreatedAt().toString());
            rMap.put("user", rv.getUser() != null ? rv.getUser().getFullName() : "Khách hàng");
            return rMap;
        }).collect(Collectors.toList()));

        response.put("realReviewCount", totalReviews);
        response.put("realRating", roundedRating);

        return ResponseEntity.ok(response);
    }
}