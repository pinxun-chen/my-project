package com.example.demo.service.impl;

import com.example.demo.model.dto.UserDto;
import com.example.demo.model.entity.User;
import com.example.demo.model.entity.VerificationToken;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VerificationTokenRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.UserService;
import com.example.demo.util.Hash;
import com.example.demo.exception.CertException;
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

    @Value("${app.base-url:http://localhost:8082}")
    private String baseUrl;


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
        String verifyUrl = baseUrl + "/users/verify?token=" + token;
        String subject = "請驗證您的帳號";
        String content = "<p>親愛的用戶您好，</p>"
                       + "<p>請點擊下方連結完成帳號驗證：</p>"
                       + "<a href=\"" + verifyUrl + "\"><b>👉 點我驗證</b></a>"
                       + "<p>驗證連結 24 小時內有效。</p>";

        emailService.sendMail(email, subject, content);
        return true;
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
    
    // 改密碼
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
    
    // 忘記密碼寄信
    @Override
    public boolean sendResetPasswordEmail(String username, String email) {
        // 同時根據 username 與 email 檢查
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return false;

        // 信箱不符，不寄信
        User user = userOpt.get();
        if (!user.getEmail().equalsIgnoreCase(email)) return false; // 信箱不符

        // 建立 token
        String token = UUID.randomUUID().toString();
        VerificationToken resetToken = new VerificationToken(null, token, user, LocalDateTime.now().plusHours(1));
        tokenRepository.save(resetToken);

        // 組 email 內容
        String resetUrl = "http://localhost:5173/reset?token=" + token;
        String subject = "重設您的密碼";
        String content = "<p>親愛的用戶您好，</p>"
                       + "<p>請點擊下方連結來重設您的密碼：</p>"
                       + "<a href=\"" + resetUrl + "\"><b>👉 點我重設密碼</b></a>"
                       + "<p>此連結 1 小時內有效。</p>";

        emailService.sendMail(email, subject, content);
        return true;
    }

    // 忘記密碼的更改密碼
    @Override
    public boolean resetPassword(String token, String newPassword) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().getExpiryDate().isBefore(LocalDateTime.now())) return false;

        User user = tokenOpt.get().getUser();
        String salt = Hash.getSalt();
        String hash = Hash.getHash(newPassword, salt);
        user.setSalt(salt);
        user.setPasswordHash(hash);
        userRepository.save(user);
        tokenRepository.delete(tokenOpt.get());
        return true;
    }

    // 刪除帳號
    @Override
    public boolean deleteUser(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            userRepository.delete(userOpt.get());
            return true;
        }
        return false;
    }
    
    // 註冊重新發送驗證
    @Override
    public boolean resendVerificationEmail(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return false;

        User user = userOpt.get();

        // 若已啟用就不需要再寄
        if (Boolean.TRUE.equals(user.getActive())) return false;

        // 刪除舊的驗證 token（可選）
        tokenRepository.deleteAll(tokenRepository.findAll().stream()
            .filter(t -> t.getUser().getUserId().equals(user.getUserId()))
            .toList());

        // 建立新 token 並寄出
        String token = UUID.randomUUID().toString();
        VerificationToken newToken = new VerificationToken(null, token, user, LocalDateTime.now().plusHours(24));
        tokenRepository.save(newToken);

        String verifyUrl = baseUrl + "/users/verify?token=" + token;
        String subject = "請驗證您的帳號";
        String content = "<p>親愛的用戶您好，</p>"
                       + "<p>請點擊下方連結完成帳號驗證：</p>"
                       + "<a href=\"" + verifyUrl + "\"><b>👉 點我驗證</b></a>"
                       + "<p>驗證連結 24 小時內有效。</p>";

        emailService.sendMail(user.getEmail(), subject, content);
        return true;
    }


    
}
