package com.example.watchshop.repository;

import com.example.watchshop.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {


    List<Order> findByUserId(Long userId);

    List<Order> findAllByOrderByOrderDateDesc();

    List<Order> findByUser_EmailOrderByOrderDateDesc(String email);


    long countByStatus(String status);

    List<Order> findTop5ByOrderByOrderDateDesc();

    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status = 'Hoàn thành' AND o.orderDate BETWEEN :start AND :end")
    Double sumRevenueByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status = 'Hoàn thành'")
    Double sumTotalRevenue();


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

    @Query("SELECT o.fullName, " +
            "SUM(o.totalPrice), " +
            "MIN(o.orderDate), " +
            "MAX(o.orderDate) " +
            "FROM Order o " +
            "WHERE o.status = 'Hoàn thành' " +
            "GROUP BY o.fullName " +
            "ORDER BY SUM(o.totalPrice) DESC")
    List<Object[]> getVipCustomers(Pageable pageable);

    Optional<Order> findByOrderCodePayos(Long orderCodePayos);

}