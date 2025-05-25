package com.example.demo.service;

import com.example.demo.exception.CertException;
import com.example.demo.model.dto.UserDto;
import com.example.demo.model.entity.User;

import java.util.Optional;

public interface UserService {
    boolean register(String username, String password, String email);
    Optional<UserDto> getUserById(Integer userId);
    Optional<UserDto> getUserByUsername(String username);
    boolean changePassword(String username, String oldPassword, String newPassword);
    boolean verifyUser(String token);
    
    boolean sendResetPasswordEmail(String username, String email); // 寄送 reset 連結
    boolean resetPassword(String token, String newPassword); // 重設密碼
    boolean deleteUser(String username); // 刪除帳號
    boolean resendVerificationEmail(String username); // 註冊重新發送驗證信

}
