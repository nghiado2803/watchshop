package com.example.watchshop.service.impl;

import com.example.watchshop.entity.Product;
import com.example.watchshop.repository.ProductRepository;
import com.example.watchshop.service.interfaces.ProductService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            return productRepository.findByNameContaining(keyword);
        }
        return productRepository.findAll();
    }

    @Override
    public List<Product> findByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    @Override
    public List<Product> findRelatedProducts(Long categoryId, Long currentProductId) {
        // Lấy tất cả sản phẩm cùng danh mục
        List<Product> list = productRepository.findByCategoryId(categoryId);

        // Loại bỏ sản phẩm đang xem khỏi danh sách gợi ý (tránh trùng lặp)
        list.removeIf(p -> p.getId().equals(currentProductId));

        // Chỉ lấy tối đa 4 sản phẩm để hiển thị đẹp giao diện
        if (list.size() > 4) {
            return list.subList(0, 4);
        }
        return list;
    }

    // --- LOGIC LỌC NÂNG CAO (SPECIFICATION) ---
    @Override
    public List<Product> filterProducts(String keyword, Long categoryId, String priceRange, String sortStr) {

        // 1. Tạo Specification để gom các điều kiện lọc (WHERE ...)
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Điều kiện 1: Tìm theo tên (LIKE %keyword%)
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("name"), "%" + keyword + "%"));
            }

            // Điều kiện 2: Tìm theo Hãng (Category)
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // Điều kiện 3: Tìm theo Khoảng giá (Price Range)
            if (StringUtils.hasText(priceRange)) {
                switch (priceRange) {
                    case "under100": // Dưới 100 triệu
                        predicates.add(cb.lessThanOrEqualTo(root.get("price"), 100000000.0));
                        break;
                    case "100to300": // Từ 100 - 300 triệu
                        predicates.add(cb.between(root.get("price"), 100000000.0, 300000000.0));
                        break;
                    case "over300": // Trên 300 triệu
                        predicates.add(cb.greaterThanOrEqualTo(root.get("price"), 300000000.0));
                        break;
                }
            }

            // Kết hợp tất cả điều kiện bằng toán tử AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 2. Tạo đối tượng Sort để sắp xếp (ORDER BY ...)
        Sort sort = Sort.unsorted();
        if ("price-asc".equals(sortStr)) {
            sort = Sort.by("price").ascending();
        } else if ("price-desc".equals(sortStr)) {
            sort = Sort.by("price").descending();
        } else {
            // Mặc định: Sắp xếp theo ID giảm dần (Sản phẩm mới nhất lên đầu)
            sort = Sort.by("id").descending();
        }

        // 3. Gọi Repository để thực thi câu lệnh SQL động
        return productRepository.findAll(spec, sort);
    }
}