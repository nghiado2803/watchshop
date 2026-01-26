package com.example.watchshop.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "shop_settings")
@Data
public class ShopSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "bank_name")
    private String bankName; // MB, VCB...

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_account_name")
    private String bankAccountName;
}