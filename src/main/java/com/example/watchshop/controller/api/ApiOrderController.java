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
                    d.put("productId", p != null ? p.getId() : null);
                    d.put("name", p != null ? p.getName() : "Sản phẩm đã bị xóa");
                    d.put("imageUrl", p != null ? p.getImageUrl() : "");
                    d.put("quantity", od.getQuantity());
                    d.put("price", od.getPrice());

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
        if (targetEmail == null || targetEmail.trim().isEmpty()) return ResponseEntity.ok(new HashMap<>());

        User user = userRepository.findByEmail(targetEmail.trim().toLowerCase()).orElse(null);
        if (user == null) return ResponseEntity.ok(new HashMap<>());

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
            String targetEmail = (email != null && !email.isEmpty()) ? email : (auth != null ? auth.getName() : null);

            if (targetEmail != null && !targetEmail.trim().isEmpty()) {
                String standardizedEmail = targetEmail.trim().toLowerCase();
                User user = userRepository.findByEmail(standardizedEmail).orElse(null);

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
                    user = userRepository.save(user);
                }
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

                    // CHỈ TRỪ KHO KHI COD / TIỀN MẶT
                    if ("COD".equalsIgnoreCase(payload.getPaymentMethod()) || "Tiền mặt".equalsIgnoreCase(payload.getPaymentMethod())) {
                        int currentStock = p.getStock() != null ? p.getStock() : 0;
                        p.setStock(Math.max(0, currentStock - item.getQuantity()));
                        productRepository.save(p);
                    }
                }
            }

            savedOrder.setTotalPrice(total);
            orderRepository.save(savedOrder);

            return ResponseEntity.ok(Map.of("message", "Đặt hàng thành công", "orderId", savedOrder.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            order = orderRepository.findByOrderCodePayos(id).orElse(null);
        }

        if (order == null) return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy đơn hàng"));

        List<String> bankingMethods = Arrays.asList("Chuyển khoản", "BANK_TRANSFER", "BANKING");
        boolean isBanking = bankingMethods.contains(order.getPaymentMethod());
        boolean isPaid = "Đã thanh toán".equals(order.getPaymentStatus());

        // LOGIC CHUẨN MỚI NHẤT
        // 1. NẾU LÀ ĐƠN BANKING MÀ CHƯA THANH TOÁN (Tức là khách quét QR xong bấm Hủy)
        // -> Đây là đơn rác, Xóa sạch khỏi DB, không cộng kho vì lúc tạo chưa trừ kho.
        if (isBanking && !isPaid) {
            orderDetailRepository.deleteAll(order.getOrderDetails());
            orderRepository.delete(order);
            return ResponseEntity.ok(Map.of(
                    "message", "Đã xóa đơn hàng rác từ PayOS",
                    "paymentMethod", order.getPaymentMethod(),
                    "paymentStatus", order.getPaymentStatus()
            ));
        }

        // 2. NẾU LÀ ĐƠN COD HOẶC ĐƠN BANKING MÀ ĐÃ THANH TOÁN THÀNH CÔNG
        // -> Đơn này hợp lệ, đổi trạng thái Đã Hủy, CỘNG DỒN VÀO KHO (vì COD trừ lúc tạo, Banking trừ lúc Webhook)
        if (!"Đã hủy".equals(order.getStatus())) {
            order.setStatus("Đã hủy");
            updateProductStock(order, 1);
            orderRepository.save(order);
        }

        return ResponseEntity.ok(Map.of(
                "message", "Đã hủy đơn hàng thành công",
                "paymentMethod", order.getPaymentMethod(),
                "paymentStatus", order.getPaymentStatus()
        ));
    }

    @GetMapping("/detail/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrderDetail(@PathVariable Long id, @RequestParam(required = false) String email, Authentication auth) {
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
    @Transactional
    public ResponseEntity<?> handlePayosWebhook(@RequestBody com.fasterxml.jackson.databind.JsonNode body) {
        try {
            long orderCode = body.get("data").get("orderCode").asLong();
            Order order = orderRepository.findByOrderCodePayos(orderCode).orElse(null);

            if (order != null && !"Đã thanh toán".equals(order.getPaymentStatus())) {
                order.setPaymentStatus("Đã thanh toán");
                order.setStatus("Đang chuẩn bị hàng");
                // TRỪ KHO KHI BANKING THANH TOÁN THÀNH CÔNG
                updateProductStock(order, -1);
                orderRepository.save(order);
            }
        } catch (Exception e) {
            System.err.println("[PAYOS Error]: " + e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

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

        if (!"Đã hủy".equals(oldStatus) && "Đã hủy".equals(status)) {
            boolean isCOD = "COD".equalsIgnoreCase(order.getPaymentMethod()) || "Tiền mặt".equalsIgnoreCase(order.getPaymentMethod());
            boolean isPaidBanking = "BANKING".equalsIgnoreCase(order.getPaymentMethod()) && "Đã thanh toán".equals(order.getPaymentStatus());

            if (isCOD || isPaidBanking) {
                updateProductStock(order, 1);
            }
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