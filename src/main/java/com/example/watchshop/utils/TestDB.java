package com.example.watchshop.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class TestDB implements CommandLineRunner {

    // Inject DataSource (nguồn dữ liệu) đã được cấu hình trong application.properties
    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("-------------------------------------");
        System.out.println("ĐANG KIỂM TRA KẾT NỐI DATABASE...");

        try (Connection connection = dataSource.getConnection()) {
            // Nếu lấy được connection nghĩa là kết nối thành công
            System.out.println("KẾT NỐI SQL SERVER THÀNH CÔNG!");
            System.out.println("URL: " + connection.getMetaData().getURL());
            System.out.println("Database Product Name: " + connection.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            System.out.println("KẾT NỐI THẤT BẠI!");
            e.printStackTrace();
        }
        System.out.println("-------------------------------------");
    }
}