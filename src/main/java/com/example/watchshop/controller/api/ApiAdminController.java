package com.example.watchshop.controller.api;

import com.example.watchshop.entity.*;
import com.example.watchshop.repository.*;
import com.example.watchshop.service.interfaces.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class ApiAdminController {

    private final UserRepository userRepository;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getDashboardStats() {
        Map<String, Object> data = new HashMap<>();

        Double totalRevenue = orderRepository.sumTotalRevenue();
        data.put("totalRevenue", totalRevenue == null ? 0.0 : totalRevenue);
        data.put("newOrdersCount", orderRepository.countByStatus("Chờ xác nhận"));

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        data.put("newCustomersCount", userRepository.countByCreatedAtBetween(start, end));

        data.put("successfulOrders", orderRepository.countByStatus("Hoàn thành"));
        Long totalStock = productRepository.sumTotalStock();
        data.put("totalStock", totalStock == null ? 0 : totalStock);
        data.put("newCustomersToday", userRepository.countByCreatedAtBetween(start, end));

        int currentYear = LocalDate.now().getYear();
        List<Object[]> rawData = orderRepository.getMonthlyRevenue(currentYear);
        Double[] monthlyData = new Double[12];
        Arrays.fill(monthlyData, 0.0);
        for (Object[] row : rawData) {
            int month = (int) row[0];
            monthlyData[month - 1] = (Double) row[1];
        }
        data.put("chartData", monthlyData);

        data.put("categoryStats", orderRepository.getCategoryStats());
        data.put("vipCustomers", orderRepository.getVipCustomers(PageRequest.of(0, 10)));

        List<Map<String, Object>> recentOrders = orderRepository.findTop5ByOrderByOrderDateDesc()
                .stream().map(o -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", o.getId());
                    m.put("fullName", o.getFullName());
                    m.put("totalPrice", o.getTotalPrice());
                    m.put("status", o.getStatus());
                    m.put("orderDate", o.getOrderDate().toString());
                    return m;
                }).collect(Collectors.toList());
        data.put("recentOrders", recentOrders);

        List<Map<String, Object>> topProducts = productRepository.findTop5ByOrderByPriceDesc()
                .stream().map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getName());
                    m.put("price", p.getPrice());
                    m.put("brand", p.getBrand());
                    m.put("imageUrl", p.getImageUrl());
                    return m;
                }).collect(Collectors.toList());
        data.put("topProducts", topProducts);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories() {
        List<Map<String, Object>> cats = categoryService.findAll().stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("productCount", c.getProducts() != null ? c.getProducts().size() : 0);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(cats);
    }

    @PostMapping("/categories")
    public ResponseEntity<?> saveCategory(@RequestBody Category category) {
        Category saved = categoryService.save(category);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Xóa thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể xóa danh mục đang có sản phẩm"));
        }
    }

    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        List<Map<String, Object>> productList = productService.findAll().stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("code", p.getCode());
            m.put("price", p.getPrice());
            m.put("stock", p.getStock());
            m.put("brand", p.getBrand());
            m.put("imageUrl", p.getImageUrl());

            if (p.getImageUrls() != null && !p.getImageUrls().isEmpty()) {
                m.put("imageUrls", p.getImageUrls().split(","));
            } else {
                m.put("imageUrls", new String[0]);
            }

            m.put("categoryId", p.getCategory() != null ? p.getCategory().getId() : null);
            m.put("categoryName", p.getCategory() != null ? p.getCategory().getName() : "Không rõ");
            m.put("discount", p.getDiscount() != null ? p.getDiscount() : 0);
            m.put("discountStart", p.getDiscountStart());
            m.put("discountEnd", p.getDiscountEnd());
            m.put("machineType", p.getMachineType());
            m.put("glassMaterial", p.getGlassMaterial());
            m.put("diameter", p.getDiameter());
            m.put("waterResistance", p.getWaterResistance());
            m.put("description", p.getDescription());

            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(productList);
    }

    @PostMapping(value = "/products", consumes = {"multipart/form-data"})
    public ResponseEntity<?> saveProduct(@ModelAttribute Product product,
                                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                         @RequestParam(value = "albumFiles", required = false) List<MultipartFile> albumFiles) throws IOException {

        Path uploadPath = Paths.get("uploads");
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();
            Files.copy(imageFile.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            product.setImageUrl(fileName);
        }

        if (albumFiles != null && !albumFiles.isEmpty()) {
            List<String> fileNames = new ArrayList<>();
            for (MultipartFile file : albumFiles) {
                if (!file.isEmpty()) {
                    String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                    Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                    fileNames.add(fileName);
                }
            }
            if (!fileNames.isEmpty()) {
                product.setImageUrls(String.join(",", fileNames));
            }
        }

        if (product.getId() != null) {
            Product oldProduct = productService.findById(product.getId()).orElse(null);
            if (oldProduct != null) {
                if (product.getImageUrl() == null) product.setImageUrl(oldProduct.getImageUrl());
                if (product.getImageUrls() == null) product.setImageUrls(oldProduct.getImageUrls());
                if (product.getStock() == null) product.setStock(oldProduct.getStock());
                if (product.getMachineType() == null) product.setMachineType(oldProduct.getMachineType());
                if (product.getGlassMaterial() == null) product.setGlassMaterial(oldProduct.getGlassMaterial());
                if (product.getDiameter() == null) product.setDiameter(oldProduct.getDiameter());
                if (product.getWaterResistance() == null) product.setWaterResistance(oldProduct.getWaterResistance());
                if (product.getDescription() == null) product.setDescription(oldProduct.getDescription());
                if (product.getBrand() == null) product.setBrand(oldProduct.getBrand());
                if (product.getCode() == null) product.setCode(oldProduct.getCode());
                if (product.getCategory() == null) product.setCategory(oldProduct.getCategory());
            }
        } else {
            if (product.getStock() == null) product.setStock(0);
        }

        Product saved = productService.save(product);
        return ResponseEntity.ok(Map.of("message", "Lưu sản phẩm thành công", "product", saved));
    }

    @PostMapping("/products/import-stock")
    public ResponseEntity<?> importStock(@RequestBody Map<String, Object> payload) {
        Long productId = Long.valueOf(payload.get("id").toString());
        int quantity = Integer.parseInt(payload.get("quantity").toString());

        Product product = productService.findById(productId).orElseThrow();
        int currentStock = (product.getStock() == null) ? 0 : product.getStock();
        product.setStock(currentStock + quantity);
        productService.save(product);

        return ResponseEntity.ok(Map.of("message", "Nhập kho thành công"));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Xóa sản phẩm thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: Không thể xóa sản phẩm này. Có thể sản phẩm đang nằm trong đơn hàng."));
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders() {
        List<Map<String, Object>> orderList = orderRepository.findAllByOrderByOrderDateDesc().stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("fullName", o.getFullName());
            m.put("phoneNumber", o.getPhoneNumber());
            m.put("address", o.getAddress());
            m.put("totalPrice", o.getTotalPrice());

            String currentStatus = o.getStatus();
            if (currentStatus == null || currentStatus.trim().isEmpty() || "null".equalsIgnoreCase(currentStatus.trim())) {
                currentStatus = "Chờ xác nhận";
            }
            m.put("status", currentStatus);

            m.put("orderDate", o.getOrderDate() != null ? o.getOrderDate().toString() : "");
            m.put("paymentMethod", o.getPaymentMethod());
            m.put("paymentStatus", o.getPaymentStatus());

            List<Map<String, Object>> details = new ArrayList<>();
            if (o.getOrderDetails() != null) {
                for (OrderDetail od : o.getOrderDetails()) {
                    Map<String, Object> d = new HashMap<>();
                    Product p = od.getProduct();

                    d.put("productName", p != null ? p.getName() : "Sản phẩm đã bị xóa");
                    d.put("imageUrl", p != null ? p.getImageUrl() : "");
                    d.put("quantity", od.getQuantity());
                    d.put("price", od.getPrice());

                    if (p != null && p.getPrice() != null && p.getPrice() > od.getPrice()) {
                        double giaNiemYet = p.getPrice();
                        double giaThucTe = od.getPrice();
                        double phanTram = ((giaNiemYet - giaThucTe) / giaNiemYet) * 100;
                        d.put("discount", (int) Math.round(phanTram));
                    } else {
                        d.put("discount", 0);
                    }

                    details.add(d);
                }
            }
            m.put("orderDetails", details);

            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(orderList);
    }

    // =========================================================
    // ĐÃ SỬA LỖI: THÊM @Transactional VÀ FIX LOGIC CỘNG/TRỪ KHO
    // =========================================================
    @PostMapping("/orders/update-status")
    @Transactional
    public ResponseEntity<?> updateOrderStatus(@RequestBody Map<String, Object> payload) {
        Long orderId = Long.valueOf(payload.get("orderId").toString());
        String status = payload.get("status").toString();

        Order order = orderRepository.findById(orderId).orElseThrow();

        String oldStatus = order.getStatus();
        if (oldStatus == null || "null".equalsIgnoreCase(oldStatus.trim())) {
            oldStatus = "Chờ xác nhận";
        }

        // CHỈ CỘNG LẠI KHO KHI ADMIN CHUYỂN TRẠNG THÁI THÀNH "ĐÃ HỦY"
        if (!"Đã hủy".equals(oldStatus) && "Đã hủy".equals(status)) {
            updateProductStock(order, 1); // 1 = Cộng thêm vào kho
        }

        order.setStatus(status);

        if ("Hoàn thành".equals(status)) {
            String method = order.getPaymentMethod();
            if ("Tiền mặt".equals(method) || "COD".equals(method)) {
                order.setPaymentStatus("Đã thanh toán");
            }
        }

        orderRepository.save(order);
        return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công"));
    }

    private void updateProductStock(Order order, int multiplier) {
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                if (product != null) {
                    int quantity = detail.getQuantity();
                    int currentStock = product.getStock() != null ? product.getStock() : 0;
                    int newStock = currentStock + (quantity * multiplier);
                    if (newStock < 0) newStock = 0;
                    product.setStock(newStock);
                    productRepository.save(product);
                }
            }
        }
    }

    @PostMapping("/orders/confirm-payment")
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, Object> payload) {
        Long orderId = Long.valueOf(payload.get("orderId").toString());
        Order order = orderRepository.findById(orderId).orElseThrow();

        List<String> bankingMethods = Arrays.asList("Chuyển khoản", "BANK_TRANSFER", "BANKING");
        if (bankingMethods.contains(order.getPaymentMethod())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Đơn hàng chuyển khoản không sử dụng chức năng thu tiền mặt."));
        }

        order.setPaymentStatus("Đã thanh toán");
        orderRepository.save(order);
        return ResponseEntity.ok(Map.of("message", "Đã xác nhận thu tiền mặt"));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("fullName", u.getFullName());
            m.put("email", u.getEmail());
            m.put("phoneNumber", u.getPhoneNumber());
            m.put("address", u.getAddress());
            m.put("role", u.getRole());
            m.put("enabled", u.isEnabled());
            m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
            return m;
        }).collect(Collectors.toList()));
    }

    @PostMapping("/users/save")
    public ResponseEntity<?> saveUser(@RequestBody User user) {
        if (user.getId() != null) {
            User oldUser = userRepository.findById(user.getId()).orElseThrow();
            user.setCreatedAt(oldUser.getCreatedAt());

            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                user.setPassword(oldUser.getPassword());
            } else {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        } else {
            user.setCreatedAt(LocalDateTime.now());
            user.setEnabled(true);

            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode("123456"));
            } else {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Lưu người dùng thành công!"));
    }

    @GetMapping("/users/toggle-status/{id}")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "message", "Đã " + (user.isEnabled() ? "mở khóa" : "khóa") + " tài khoản",
                "newStatus", user.isEnabled()
        ));
    }
}