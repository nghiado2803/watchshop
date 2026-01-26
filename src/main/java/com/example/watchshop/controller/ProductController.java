package com.example.watchshop.controller;

import com.example.watchshop.entity.Product;
import com.example.watchshop.service.interfaces.CategoryService;
import com.example.watchshop.service.interfaces.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.watchshop.repository.ReviewRepository;
import com.example.watchshop.entity.Review;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ReviewRepository reviewRepository;

    // Trang danh sách
    @GetMapping("/products")
    public String listProducts(Model model,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Long categoryId,
                               @RequestParam(required = false) String priceRange,
                               @RequestParam(required = false) String sort,
                               HttpServletRequest request) {
        request.getSession(true);

        // Gọi hàm lọc ở Service
        List<Product> list = productService.filterProducts(keyword, categoryId, priceRange, sort);

        model.addAttribute("products", list);
        model.addAttribute("categories", categoryService.findAll());

        // Gửi lại các tham số về View để giữ trạng thái (Sticky form)
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("priceRange", priceRange);
        model.addAttribute("sort", sort);

        return "products";
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model, HttpServletRequest request) {
        request.getSession(true);

        Product product = productService.findById(id).orElse(null);
        if (product == null) return "redirect:/products";

        // Lấy sản phẩm liên quan
        List<Product> relatedProducts = List.of();
        if (product.getCategory() != null) {
            relatedProducts = productService.findRelatedProducts(product.getCategory().getId(), id);
        }

        // --- LẤY DANH SÁCH ĐÁNH GIÁ TỪ DB ---
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(id);

        // --- LOGIC TÍNH TOÁN SỐ LIỆU THẬT ---
        int totalReviews = reviews.size(); // Đếm số lượng thực tế

        // Tính điểm trung bình (cộng tổng sao / số lượng)
        double avgRating = 0;
        if (totalReviews > 0) {
            double sum = reviews.stream().mapToInt(Review::getRating).sum();
            avgRating = sum / totalReviews;
        } else {
            avgRating = 5.0; // Mặc định 5 sao nếu chưa có đánh giá
        }

        // Làm tròn điểm để hiển thị sao (ví dụ 4.5 -> 5)
        int roundedRating = (int) Math.round(avgRating);

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", relatedProducts);
        model.addAttribute("reviews", reviews);

        // Gửi số liệu đã tính toán sang View (Ghi đè dữ liệu cũ của Product)
        model.addAttribute("realReviewCount", totalReviews);
        model.addAttribute("realRating", roundedRating);

        return "product-detail";
    }

}