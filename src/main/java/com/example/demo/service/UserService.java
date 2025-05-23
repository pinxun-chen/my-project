package com.example.demo.service;

import com.example.demo.model.dto.UserDto;
import com.example.demo.model.entity.User;

import java.util.Optional;

public interface UserService {
    boolean register(String username, String password, String email);
    UserDto login(String username, String password);
    Optional<UserDto> getUserById(Integer userId);
    Optional<UserDto> getUserByUsername(String username);
    boolean changePassword(String username, String oldPassword, String newPassword);
    boolean verifyUser(String token);

}
