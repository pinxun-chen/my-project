package com.example.demo.model.dto;

import lombok.Data;

@Data
// 建立一個 RegisterRequestDto 類別
public class RegisterRequestDto {
    private String username;
    private String password;
    private String email;
    // getter, setter
}
