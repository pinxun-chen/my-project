package com.example.demo.model.dto;

import lombok.Data;

@Data
public class UserDto {
	private Integer userId; // 使用者 Id
	private String username; // 使用者名稱
	private String email; // 使用者Email
	private Boolean active; // 帳號啟動
	private String role; // 角色權限
	
}