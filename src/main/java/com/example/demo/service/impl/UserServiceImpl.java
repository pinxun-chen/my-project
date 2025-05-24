package com.example.demo.service.impl;

import com.example.demo.model.dto.UserDto;
import com.example.demo.model.entity.User;
import com.example.demo.model.entity.VerificationToken;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VerificationTokenRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.UserService;
import com.example.demo.util.Hash;
import com.example.demo.mapper.UserMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.verification-url}")
    private String verificationBaseUrl;

    // 使用者註冊
    @Override
    public boolean register(String username, String password, String email) {
    	// 只檢查帳號是否存在，不檢查 Email（方便測試）
        if (userRepository.existsByUsername(username)) return false;

        String salt = Hash.getSalt();
        String hash = Hash.getHash(password, salt);

        // 建立使用者
        User user = new User(null, username, hash, salt, email, false, "USER");
        userRepository.save(user);
        
        // 建立驗證 Token
        String token = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken(null, token, user, LocalDateTime.now().plusHours(24));
        tokenRepository.save(vt);

        // 寄出 HTML 格式的驗證信
        String verifyUrl = verificationBaseUrl + token;
        String subject = "請驗證您的帳號";
        String content = "<p>親愛的用戶您好，</p>"
                       + "<p>請點擊下方連結完成帳號驗證：</p>"
                       + "<a href=\"" + verifyUrl + "\"><b>👉 點我驗證</b></a>"
                       + "<p>驗證連結 24 小時內有效。</p>";

        emailService.sendMail(email, subject, content);
        return true;
    }

    // 使用者登入
    @Override
    public UserDto login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String hash = Hash.getHash(password, user.getSalt());
            if (hash.equals(user.getPasswordHash())) {
                return userMapper.toDto(user);
            }
        }

        return null;
    }

    // 依 ID 查詢使用者
    @Override
    public Optional<UserDto> getUserById(Integer userId) {
        return userRepository.findById(userId).map(userMapper::toDto);
    }

    // 依帳號查詢使用者
    @Override
    public Optional<UserDto> getUserByUsername(String username) {
        return userRepository.findByUsername(username).map(userMapper::toDto);
    }
    
    @Override
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 驗證舊密碼
            String hashedOldPassword = Hash.getHash(oldPassword, user.getSalt());
            if (!hashedOldPassword.equals(user.getPasswordHash())) {
                return false; // 原密碼錯誤
            }

            // 設定新密碼
            String newSalt = Hash.getSalt();
            String newHash = Hash.getHash(newPassword, newSalt);
            user.setSalt(newSalt);
            user.setPasswordHash(newHash);

            userRepository.save(user);
            return true;
        }

        return false; // 使用者不存在
    }
    
    @Override
    public boolean verifyUser(String token) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        VerificationToken verificationToken = tokenOpt.get();

        // 檢查是否過期
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }

        User user = verificationToken.getUser();
        user.setActive(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);

        return true;
    }

    
}
