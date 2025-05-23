package com.example.demo.controller;

import com.example.demo.model.dto.ChangePasswordRequestDto;
import com.example.demo.model.dto.RegisterRequestDto;
import com.example.demo.model.dto.UserDto;
import com.example.demo.model.entity.User;
import com.example.demo.model.entity.VerificationToken;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 驗證信箱 
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyUser(@RequestParam String token) {
        boolean verified = userService.verifyUser(token);
        if (verified) {
            return ResponseEntity.ok(ApiResponse.success("帳號驗證成功", null));
        } else {
            return ResponseEntity.status(400).body(ApiResponse.error(400, "驗證連結無效或已過期"));
        }
    }


    // 註冊
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegisterRequestDto request) {
        boolean success = userService.register(request.getUsername(), request.getPassword(), request.getEmail());

        if (success) {
            return ResponseEntity.ok(ApiResponse.success("註冊成功", null));
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(400, "帳號或信箱已存在"));
        }
    }


    // 依 ID 查詢使用者
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getUserById(@PathVariable Integer id) {
        Optional<UserDto> userOpt = userService.getUserById(id);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("查詢成功", userOpt.get()));
        } else {
            return ResponseEntity
                    .status(404)
                    .body(ApiResponse.error(404, "找不到使用者"));
        }
    }

    // 依 name 查詢使用者
    @GetMapping("/name/{username}")
    public ResponseEntity<ApiResponse<?>> getUserByUsername(@PathVariable String username) {
        Optional<UserDto> userOpt = userService.getUserByUsername(username);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("查詢成功", userOpt.get()));
        } else {
            return ResponseEntity
                    .status(404)
                    .body(ApiResponse.error(404, "找不到使用者"));
        }
    }
    
    // 更改密碼
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody ChangePasswordRequestDto request) {

        boolean success = userService.changePassword(
            request.getUsername(), 
            request.getOldPassword(), 
            request.getNewPassword()
        );

        if (success) {
            return ResponseEntity.ok(ApiResponse.success("密碼更新成功", null));
        } else {
            return ResponseEntity.status(400).body(ApiResponse.error(400, "密碼錯誤或使用者不存在"));
        }
    }
}

