package com.example.watchshop.controller.api;

import com.example.watchshop.dto.CartItem;
import com.example.watchshop.entity.*;
import com.example.watchshop.repository.*;
import com.example.watchshop.service.interfaces.PayosService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"}, allowCredentials = "true")
public class ApiOrderController {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Autowired
    private PayosService payosService;

    @GetMapping("/my-orders")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyOrders(@RequestParam(required = false) String email, Authentication auth) {
        // ĐÃ SỬA LỖI: Ưu tiên email từ frontend truyền lên để tránh dính ID của Google
        String targetEmail = (email != null && !email.isEmpty()) ? email : (auth != null ? auth.getName() : null);

        if (targetEmail == null || targetEmail.trim().isEmpty())
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));

        List<Order> orders = orderRepository.findByUser_EmailOrderByOrderDateDesc(targetEmail.trim().toLowerCase());

        List<Map<String, Object>> result = orders.stream().map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", o.getId());
            map.put("code", o.getCode());
            map.put("paymentMethod", o.getPaymentMethod());
            map.put("orderDate", o.getOrderDate().toString());
            map.put("status", o.getStatus());
            map.put("address", o.getAddress());
            map.put("totalPrice", o.getTotalPrice());

            List<Map<String, Object>> details = new ArrayList<>();
            if (o.getOrderDetails() != null) {
                for (OrderDetail od : o.getOrderDetails()) {
                    Map<String, Object> d = new HashMap<>();
                    Product p = od.getProduct();
                    // BẮT BUỘC PHẢI CÓ productId ĐỂ VUE GỬI ĐÁNH GIÁ
                    d.put("productId", p != null ? p.getId() : null);
                    d.put("name", p != null ? p.getName() : "Sản phẩm đã bị xóa");
                    d.put("imageUrl", p != null ? p.getImageUrl() : "");
                    d.put("quantity", od.getQuantity());
                    d.put("price", od.getPrice());

                    // Logic tính giảm giá lịch sử
                    if (p != null && p.getPrice() > od.getPrice()) {
                        double phanTram = ((p.getPrice() - od.getPrice()) / p.getPrice()) * 100;
                        d.put("discount", (int) Math.round(phanTram));
                    } else {
                        d.put("discount", 0);
                    }
                    details.add(d);
                }
            }
            map.put("orderDetails", details);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }


    @GetMapping("/user-info")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getUserInfoForCheckout(@RequestParam(required = false) String email, Authentication auth) {
        String targetEmail = (email != null && !email.isEmpty()) ? email : (auth != null ? auth.getName() : null);

        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            return ResponseEntity.ok(new HashMap<>());
        }

        User user = userRepository.findByEmail(targetEmail.trim().toLowerCase()).orElse(null);

        if (user == null) {
            return ResponseEntity.ok(new HashMap<>());
        }

        Map<String, String> info = new HashMap<>();
        info.put("fullName", user.getFullName());
        info.put("phoneNumber", user.getPhoneNumber());
        info.put("address", user.getAddress());

        return ResponseEntity.ok(info);
    }

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<?> placeOrder(@RequestBody CheckoutRequest payload, @RequestParam(required = false) String email, Authentication auth) {
        try {
            Order order = new Order();

            // ĐÃ SỬA LỖI: Ưu tiên lấy email truyền lên để gắn đúng vào User
            String targetEmail = (email != null && !email.isEmpty()) ? email : (auth != null ? auth.getName() : null);

            if (targetEmail != null && !targetEmail.trim().isEmpty()) {
                // Chuẩn hóa email
                String standardizedEmail = targetEmail.trim().toLowerCase();
                User user = userRepository.findByEmail(standardizedEmail).orElse(null);

                // Tự động tạo user nếu chưa tồn tại
                if (user == null) {
                    user = new User();
                    user.setEmail(standardizedEmail);
                    user.setFullName(payload.getFullName());
                    user.setPhoneNumber(payload.getPhoneNumber());
                    user.setAddress(payload.getAddress());
                    user.setRole("ROLE_USER");
                    user.setEnabled(true);
                    user.setCreatedAt(LocalDateTime.now());
                    user.setPassword("DUMMY_GOOGLE_PASSWORD");
                    // Lưu thẳng user mới này vào Database
                    user = userRepository.save(user);
                }

                // Lúc này biến 'user' chắc chắn có dữ liệu (không bao giờ bị null nữa)
                order.setUser(user);
            }

            order.setFullName(payload.getFullName());
            order.setPhoneNumber(payload.getPhoneNumber());
            order.setAddress(payload.getAddress());
            order.setNote(payload.getNote());
            order.setPaymentMethod(payload.getPaymentMethod());
            order.setPaymentStatus("Chưa thanh toán");
            order.setOrderDate(LocalDateTime.now());
            order.setStatus("Chờ xác nhận");
            order.setCode("ORD-" + System.currentTimeMillis());

            Order savedOrder = orderRepository.save(order);
            double total = 0;

            for (CartItem item : payload.getItems()) {
                Product p = productRepository.findById(item.getProductId()).orElse(null);
                if (p != null) {
                    OrderDetail detail = new OrderDetail();
                    detail.setOrder(savedOrder);
                    detail.setProduct(p);
                    detail.setQuantity(item.getQuantity());
                    detail.setPrice(item.getPrice());
                    orderDetailRepository.save(detail);

                    total += (item.getPrice() * item.getQuantity());

                    int currentStock = p.getStock() != null ? p.getStock() : 0;
                    p.setStock(Math.max(0, currentStock - item.getQuantity()));
                    productRepository.save(p);
                }
            }

            savedOrder.setTotalPrice(total);
            orderRepository.save(savedOrder);

            return ResponseEntity.ok(Map.of("message", "Đặt hàng thành công", "orderId", savedOrder.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrderDetail(@PathVariable Long id, @RequestParam(required = false) String email, Authentication auth) {

        // ĐÃ SỬA LỖI: Ưu tiên email từ frontend truyền lên
        String targetEmail = (email != null && !email.isEmpty()) ? email : (auth != null ? auth.getName() : null);

        if (targetEmail == null || targetEmail.trim().isEmpty())
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));

        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy đơn hàng"));

        Map<String, Object> response = new HashMap<>();
        response.put("id", order.getId());
        response.put("code", order.getCode());
        response.put("status", order.getStatus());
        response.put("orderDate", order.getOrderDate() != null ? order.getOrderDate().toString() : null);
        response.put("fullName", order.getFullName());
        response.put("phoneNumber", order.getPhoneNumber());
        response.put("address", order.getAddress());
        response.put("totalPrice", order.getTotalPrice());
        response.put("paymentMethod", order.getPaymentMethod());
        response.put("paymentStatus", order.getPaymentStatus());

        List<Map<String, Object>> orderDetailsList = new ArrayList<>();
        if (order.getOrderDetails() != null) {
            for (OrderDetail od : order.getOrderDetails()) {
                Map<String, Object> item = new HashMap<>();
                Product p = od.getProduct();

                item.put("productId", p != null ? p.getId() : null);

                item.put("productName", p != null ? p.getName() : "Sản phẩm đã bị xóa");
                item.put("imageUrl", p != null ? p.getImageUrl() : "");
                item.put("quantity", od.getQuantity());
                item.put("price", od.getPrice());

                if (p != null && p.getPrice() > od.getPrice()) {
                    double phanTram = ((p.getPrice() - od.getPrice()) / p.getPrice()) * 100;
                    item.put("discount", (int) Math.round(phanTram));
                } else {
                    item.put("discount", 0);
                }

                orderDetailsList.add(item);
            }
        }
        response.put("orderDetails", orderDetailsList);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> payOrder(@PathVariable Long id) throws Exception {
        Order order = orderRepository.findById(id).orElseThrow();
        String url = payosService.createPaymentLink(order);
        return ResponseEntity.ok(Collections.singletonMap("url", url));
    }

    @PostMapping("/payos-webhook")
    public ResponseEntity<?> handlePayosWebhook(@RequestBody com.fasterxml.jackson.databind.JsonNode body) {
        try {
            long orderCode = body.get("data").get("orderCode").asLong();
            Order order = orderRepository.findByOrderCodePayos(orderCode).orElse(null);
            if (order != null) {
                order.setPaymentStatus("Đã thanh toán");
                order.setStatus("Đang chuẩn bị hàng");
                orderRepository.save(order);
                System.out.println("[PAYOS] Thanh toán thành công đơn: " + orderCode);
            }
        } catch (Exception e) {
            System.err.println("[PAYOS Error]: " + e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @Data
    public static class CheckoutRequest {
        private String fullName;
        private String phoneNumber;
        private String address;
        private String note;
        private String paymentMethod;
        private List<CartItem> items;
    }
}