package org.example.jwtjavaeight.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希生成工具（临时使用）
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 生成 123456 的哈希值
        String password = "123456";
        String hash = encoder.encode(password);

        System.out.println("===========================================");
        System.out.println("原始密码: " + password);
        System.out.println("BCrypt 哈希值:");
        System.out.println(hash);
        System.out.println("===========================================");
        System.out.println();
        System.out.println("SQL 插入语句示例:");
        System.out.println("UPDATE sys_user SET password = '" + hash + "' WHERE username = 'admin';");
        System.out.println();
        System.out.println("或直接插入:");
        System.out.println("INSERT INTO sys_user (username, password, status) VALUES");
        System.out.println("('admin', '" + hash + "', 1);");
        System.out.println("===========================================");

        // 验证哈希值是否正确
        boolean matches = encoder.matches(password, hash);
        System.out.println("\n验证结果: " + (matches ? "✓ 密码匹配成功" : "✗ 密码匹配失败"));
    }
}
