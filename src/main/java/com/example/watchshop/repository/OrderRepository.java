package com.example.watchshop.repository;

import com.example.watchshop.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ==========================================
    // PHẦN 1: TRUY VẤN DANH SÁCH ĐƠN HÀNG
    // ==========================================

    // 1. Lấy lịch sử đơn hàng của 1 user cụ thể
    List<Order> findByUserId(Long userId);

    // 2. Lấy tất cả đơn hàng, sắp xếp mới nhất lên đầu (Cho trang Admin)
    List<Order> findAllByOrderByOrderDateDesc();

    // 3. Tìm kiếm đơn hàng theo Email khách
    List<Order> findByUser_EmailOrderByOrderDateDesc(String email);


    // ==========================================
    // PHẦN 2: THỐNG KÊ DASHBOARD CƠ BẢN
    // ==========================================

    // 4. Đếm số đơn theo trạng thái (VD: Chờ xác nhận)
    long countByStatus(String status);

    // 5. Lấy 5 đơn mới nhất (Cho bảng 'Đơn hàng vừa đặt')
    List<Order> findTop5ByOrderByOrderDateDesc();

    // 6. Tính doanh thu trong khoảng thời gian (VD: Hôm nay)
    // Chỉ tính đơn 'Hoàn thành'
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status = 'Hoàn thành' AND o.orderDate BETWEEN :start AND :end")
    Double sumRevenueByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 7. Tính TỔNG DOANH THU TÍCH LŨY (Toàn bộ thời gian)
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status = 'Hoàn thành'")
    Double sumTotalRevenue();


    // ==========================================
    // PHẦN 3: [MỚI] TRUY VẤN CHO BIỂU ĐỒ (CHART)
    // ==========================================

    // 8. Lấy tổng doanh thu của từng tháng trong năm cụ thể
    // Kết quả trả về dạng List các mảng: [Tháng, Tổng tiền]
    // Ví dụ:
    // - Tháng 1: 5.000.000
    // - Tháng 2: 10.000.000
    @Query("SELECT MONTH(o.orderDate), SUM(o.totalPrice) " +
            "FROM Order o " +
            "WHERE o.status = 'Hoàn thành' AND YEAR(o.orderDate) = :year " +
            "GROUP BY MONTH(o.orderDate)")
    List<Object[]> getMonthlyRevenue(@Param("year") int year);
    @Query("SELECT c.name, " +
            "SUM(od.price * od.quantity), " +
            "SUM(od.quantity), " +
            "MIN(od.price), " +
            "MAX(od.price), " +
            "AVG(od.price) " +
            "FROM OrderDetail od " +
            "JOIN od.product p " +
            "JOIN p.category c " +
            "JOIN od.order o " +
            "WHERE o.status = 'Hoàn thành' " +
            "GROUP BY c.name")
    List<Object[]> getCategoryStats();

    // 2. Top 10 Khách hàng VIP (Trả về mảng Object[])
    // Index: [0]=Tên Khách, [1]=Tổng tiền, [2]=Ngày đầu, [3]=Ngày cuối
    @Query("SELECT o.fullName, " +
            "SUM(o.totalPrice), " +
            "MIN(o.orderDate), " +
            "MAX(o.orderDate) " +
            "FROM Order o " +
            "WHERE o.status = 'Hoàn thành' " +
            "GROUP BY o.fullName " +
            "ORDER BY SUM(o.totalPrice) DESC")
    List<Object[]> getVipCustomers(Pageable pageable);

}