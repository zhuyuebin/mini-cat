package com.minicat.minicatserver.Utils;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码加密服务 - 使用AES-256-GCM算法对数据库连接密码进行加密存储
 * 支持加密和解密,适用于需要还原原始密码的场景
 */
@Component
public class PasswordEncryptionService {
    
    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    // 固定密钥(生产环境应该从配置文件或密钥管理服务获取)
    private static final String SECRET_KEY = "MiniCat-Database-Connection-Key-2024!";
    private final SecretKey key;
    
    public PasswordEncryptionService() throws Exception {
        // 从固定字符串生成密钥
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(SECRET_KEY.getBytes());
        keyGenerator.init(KEY_SIZE, secureRandom);
        this.key = keyGenerator.generateKey();
    }
    
    /**
     * 加密密码
     * @param rawPassword 原始密码
     * @return 加密后的密码(Base64编码,格式: iv:ciphertext)
     */
    public String encrypt(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return rawPassword;
        }
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            
            byte[] ciphertext = cipher.doFinal(rawPassword.getBytes("UTF-8"));
            
            // 将IV和密文组合并Base64编码
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }
    
    /**
     * 解密密码
     * @param encryptedPassword 加密后的密码(Base64编码)
     * @return 原始密码
     */
    public String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return encryptedPassword;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedPassword);
            
            // 提取IV和密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);
            
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }
    
    /**
     * 验证密码是否匹配
     * @param rawPassword 原始密码
     * @param encryptedPassword 加密后的密码
     * @return 是否匹配
     */
    public boolean matches(String rawPassword, String encryptedPassword) {
        if (rawPassword == null || encryptedPassword == null) {
            return false;
        }
        try {
            String decrypted = decrypt(encryptedPassword);
            return rawPassword.equals(decrypted);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查密码是否已加密
     * @param password 密码
     * @return 是否已加密(通过检查是否为有效的Base64且长度合理)
     */
    public boolean isEncrypted(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        // AES-GCM加密后的密码通常是Base64编码,长度至少32字符
        if (password.length() < 32) {
            return false;
        }
        // 尝试Base64解码验证
        try {
            Base64.getDecoder().decode(password);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
