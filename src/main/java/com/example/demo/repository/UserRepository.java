package com.example.demo.repository;

import com.example.demo.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // 依帳號查詢使用者（可用於登入驗證）
    Optional<User> findByUsername(String username);

    // 檢查信箱是否已存在（可用於註冊驗證）
    boolean existsByEmail(String email);

    // 檢查帳號是否已存在（可用於註冊驗證）
    boolean existsByUsername(String username);
    
    @Query(value = "select user_id, username, password_hash, salt, email, active, role from users where username=:username", nativeQuery = true)
	User getUser(String username);
}
