package com.example.watchshop.controller;

import com.example.watchshop.entity.*;
import com.example.watchshop.repository.OrderRepository;
import com.example.watchshop.repository.ProductRepository;
import com.example.watchshop.repository.UserRepository;
import com.example.watchshop.service.interfaces.CategoryService;
import com.example.watchshop.service.interfaces.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;

    // --- DASHBOARD (GIỮ NGUYÊN) ---
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Double totalRevenue = orderRepository.sumTotalRevenue();
        model.addAttribute("totalRevenue", totalRevenue == null ? 0.0 : totalRevenue);
        long newOrdersCount = orderRepository.countByStatus("Chờ xác nhận");
        model.addAttribute("newOrdersCount", newOrdersCount);
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long newCustomersCount = userRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        model.addAttribute("newCustomersCount", newCustomersCount);
        List<Order> recentOrders = orderRepository.findTop5ByOrderByOrderDateDesc();
        model.addAttribute("recentOrders", recentOrders);
        return "admin/dashboard";
    }

    // --- THỐNG KÊ (GIỮ NGUYÊN) ---
    @GetMapping("/stats")
    public String stats(Model model) {
        Double totalRevenue = orderRepository.sumTotalRevenue();
        model.addAttribute("totalRevenue", totalRevenue == null ? 0.0 : totalRevenue);
        long successfulOrders = orderRepository.countByStatus("Hoàn thành");
        model.addAttribute("successfulOrders", successfulOrders);
        Long totalStock = productRepository.sumTotalStock();
        model.addAttribute("totalStock", totalStock);

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long newCustomersToday = userRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        model.addAttribute("newCustomersToday", newCustomersToday);

        int currentYear = LocalDate.now().getYear();
        List<Object[]> rawData = orderRepository.getMonthlyRevenue(currentYear);
        Double[] monthlyData = new Double[12];
        for (int i = 0; i < 12; i++) monthlyData[i] = 0.0;
        for (Object[] row : rawData) {
            int month = (int) row[0];
            Double revenue = (Double) row[1];
            monthlyData[month - 1] = revenue;
        }
        model.addAttribute("chartData", monthlyData);

        List<Product> topProducts = productRepository.findTop5ByOrderByPriceDesc();
        model.addAttribute("topProducts", topProducts);

        List<Object[]> categoryStats = orderRepository.getCategoryStats();
        model.addAttribute("categoryStats", categoryStats);

        List<Object[]> vipCustomers = orderRepository.getVipCustomers(PageRequest.of(0, 10));
        model.addAttribute("vipCustomers", vipCustomers);

        return "admin/stats";
    }

    // --- QUẢN LÝ ĐƠN HÀNG (GIỮ NGUYÊN) ---
    @GetMapping("/manage-orders")
    public String orders(Model model) {
        model.addAttribute("orders", orderRepository.findAllByOrderByOrderDateDesc());
        return "admin/manage-orders";
    }

    @PostMapping("/orders/update-status")
    public String updateOrderStatus(@RequestParam("orderId") Long orderId,
                                    @RequestParam("status") String status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        String oldStatus = order.getStatus();

        if ("Chờ xác nhận".equals(oldStatus) && ("Đang giao".equals(status) || "Hoàn thành".equals(status))) {
            updateProductStock(order, -1);
        }

        if (("Đang giao".equals(oldStatus) || "Hoàn thành".equals(oldStatus)) && "Đã hủy".equals(status)) {
            updateProductStock(order, 1);
        }

        order.setStatus(status);

        if ("Hoàn thành".equals(status)) {
            String method = order.getPaymentMethod();
            if ("Tiền mặt".equals(method) || "COD".equals(method)) {
                order.setPaymentStatus("Đã thanh toán");
            }
        }

        orderRepository.save(order);
        return "redirect:/admin/manage-orders?success=true";
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
    public String confirmPayment(@RequestParam("orderId") Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setPaymentStatus("Đã thanh toán");
        orderRepository.save(order);
        return "redirect:/admin/manage-orders?success=PaymentConfirmed";
    }

    // --- QUẢN LÝ SẢN PHẨM ---
    @GetMapping("/manage-products")
    public String products(Model model) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("product", new Product());
        return "admin/manage-products";
    }

    // [ĐÃ SỬA LẠI HÀM NÀY ĐỂ KHÔNG BỊ MẤT TỒN KHO]
    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute Product product,
                              @RequestParam("imageFile") MultipartFile imageFile) throws IOException {

        // 1. Xử lý upload ảnh
        if (!imageFile.isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();
            Path uploadPath = Paths.get("uploads");
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            try (var inputStream = imageFile.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
            product.setImageUrl(fileName);
        }

        // 2. [LOGIC QUAN TRỌNG] Kiểm tra xem là Sửa hay Thêm mới
        if (product.getId() != null) {
            // Đây là sửa: Lấy dữ liệu cũ từ DB lên
            Product oldProduct = productService.findById(product.getId()).orElse(null);

            if (oldProduct != null) {
                // a. Giữ lại số lượng tồn kho cũ (Vì form sửa không gửi stock lên)
                product.setStock(oldProduct.getStock());

                // b. Nếu người dùng không chọn ảnh mới, giữ lại ảnh cũ
                if (product.getImageUrl() == null || product.getImageUrl().isEmpty()) {
                    product.setImageUrl(oldProduct.getImageUrl());
                }
            }
        } else {
            // Đây là thêm mới: Mặc định tồn kho bằng 0 nếu null
            if (product.getStock() == null) {
                product.setStock(0);
            }
        }

        productService.save(product);
        return "redirect:/admin/manage-products";
    }

    @PostMapping("/products/import-stock")
    public String importStock(@RequestParam("id") Long productId, @RequestParam("quantity") int quantity) {
        Product product = productService.findById(productId).orElseThrow();
        int currentStock = (product.getStock() == null) ? 0 : product.getStock();
        product.setStock(currentStock + quantity);
        productService.save(product);
        return "redirect:/admin/manage-products?success=Imported";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteById(id);
        } catch (Exception e) {
            return "redirect:/admin/manage-products?error=CannotDelete";
        }
        return "redirect:/admin/manage-products";
    }

    @GetMapping("/manage-categories")
    public String categories(Model model, @RequestParam(value = "id", required = false) Long id) {
        model.addAttribute("categories", categoryService.findAll());
        if (id != null) {
            model.addAttribute("category", categoryService.findById(id).orElse(new Category()));
        } else {
            model.addAttribute("category", new Category());
        }
        return "admin/manage-categories";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Category category) {
        categoryService.save(category);
        return "redirect:/admin/manage-categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        try { categoryService.deleteById(id); } catch (Exception e) {}
        return "redirect:/admin/manage-categories";
    }

    @GetMapping("/manage-users")
    public String manageUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("userObj", new User());
        return "admin/manage-users";
    }

    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute("userObj") User user) {
        if (user.getId() != null) {
            User old = userRepository.findById(user.getId()).orElseThrow();
            user.setCreatedAt(old.getCreatedAt());
            if (user.getPassword() == null || user.getPassword().isEmpty()) user.setPassword(old.getPassword());
            else user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setCreatedAt(LocalDateTime.now());
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setEnabled(true);
        }
        userRepository.save(user);
        return "redirect:/admin/manage-users";
    }

    @GetMapping("/users/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return "redirect:/admin/manage-users";
    }

}