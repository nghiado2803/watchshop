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
        List<Product> list = productRepository.findByCategoryId(categoryId);

        list.removeIf(p -> p.getId().equals(currentProductId));

        if (list.size() > 4) {
            return list.subList(0, 4);
        }
        return list;
    }

    @Override
    public List<Product> filterProducts(String keyword, Long categoryId, String priceRange, String sortStr) {

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("name"), "%" + keyword + "%"));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (StringUtils.hasText(priceRange)) {
                switch (priceRange) {
                    case "under100":
                        predicates.add(cb.lessThanOrEqualTo(root.get("price"), 100000000.0));
                        break;
                    case "100to300":
                        predicates.add(cb.between(root.get("price"), 100000000.0, 300000000.0));
                        break;
                    case "over300":
                        predicates.add(cb.greaterThanOrEqualTo(root.get("price"), 300000000.0));
                        break;
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.unsorted();
        if ("price-asc".equals(sortStr)) {
            sort = Sort.by("price").ascending();
        } else if ("price-desc".equals(sortStr)) {
            sort = Sort.by("price").descending();
        } else {
            sort = Sort.by("id").descending();
        }

        return productRepository.findAll(spec, sort);
    }
}