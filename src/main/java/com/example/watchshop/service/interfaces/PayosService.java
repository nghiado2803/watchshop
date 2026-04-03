package com.example.watchshop.service.interfaces;

import com.example.watchshop.entity.Order;
import com.example.watchshop.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.CheckoutResponseData;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PayosService {
    @Autowired
    private PayOS payOS;

    @Autowired
    private OrderRepository orderRepository;

    public String createPaymentLink(Order order) throws Exception {
        long orderCode = System.currentTimeMillis() / 1000;

        String rawDescription = "Thanh toan don " + order.getCode();
        String safeDescription = rawDescription.length() > 25
                ? rawDescription.substring(0, 25)
                : rawDescription;

        ItemData item = ItemData.builder()
                .name("Đơn hàng " + order.getCode())
                .quantity(1)
                .price(order.getTotalPrice().intValue())
                .build();

        PaymentData paymentData = PaymentData.builder()
                .orderCode(orderCode)
                .amount(order.getTotalPrice().intValue())
                .description(safeDescription)
                .item(item)
                .returnUrl("http://localhost:5173/orders")
                .cancelUrl("http://localhost:5173/orders")
                .build();

        try {
            CheckoutResponseData data = payOS.createPaymentLink(paymentData);

            order.setOrderCodePayos(orderCode);
            orderRepository.save(order);
            return data.getCheckoutUrl();

        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                errorMsg += e.getCause().getMessage();
            }

            Pattern pattern = Pattern.compile("\"checkoutUrl\":\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(errorMsg);

            if (matcher.find()) {
                String checkoutUrl = matcher.group(1);

                order.setOrderCodePayos(orderCode);
                orderRepository.save(order);

                return checkoutUrl;
            }
            throw e;
        }
    }
}