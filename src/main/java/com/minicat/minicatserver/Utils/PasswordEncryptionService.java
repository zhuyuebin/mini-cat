package com.minicat.minicatserver.Utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码加密服务 - 使用 AES-256-GCM 算法对数据库连接密码进行加密存储。
 *
 * <p>密钥来源（按优先级）：
 * <ol>
 *   <li>环境变量 {@code MINICAT_SECRET_KEY}（适合容器/CI 部署）；</li>
 *   <li>密钥文件 {@code minicat.security.key-file}（默认 {@code ./data/secret.key}），
 *       首次启动时随机生成并写入。</li>
 * </ol>
 *
 * <p>密文格式：{@code v2:Base64(IV + ciphertext)}。旧版本（v1.0.0 及之前）
 * 使用固定种子派生密钥，密文没有前缀；这里保留了对旧密文的解密兼容，
 * 但新写入的数据一律使用 v2 格式。旧数据在下次保存连接时会自然升级。
 */
@Component
public class PasswordEncryptionService {

    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String CIPHERTEXT_PREFIX = "v2:";
    private static final String KEY_ENV = "MINICAT_SECRET_KEY";
    private static final int SECRET_BYTES = 32;

    /**
     * 旧版本固定种子，仅用于解密历史数据，禁止用于新数据。
     * 保留它是为了老用户升级后仍能连接已保存的数据库。
     */
    private static final String LEGACY_SEED = "MiniCat-Database-Connection-Key-2024!";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey key;
    private final SecretKey legacyKey;

    public PasswordEncryptionService(
            @Value("${minicat.security.key-file:./data/secret.key}") String keyFile) {
        this.key = new SecretKeySpec(sha256(resolveSecret(keyFile)), "AES");
        this.legacyKey = legacyKeyFromFixedSeed();
    }

    /**
     * 加密密码。
     *
     * @param rawPassword 原始密码
     * @return {@code v2:Base64(IV + ciphertext)}；入参为空时原样返回
     */
    public String encrypt(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return rawPassword;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(rawPassword.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return CIPHERTEXT_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt password", e);
        }
    }

    /**
     * 解密密码，同时兼容 v2 格式与旧版本（v1.0.0 及之前）的历史密文。
     *
     * @param encryptedPassword 加密后的密码
     * @return 原始密码
     */
    public String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return encryptedPassword;
        }
        if (encryptedPassword.startsWith(CIPHERTEXT_PREFIX)) {
            return decryptWith(key, encryptedPassword.substring(CIPHERTEXT_PREFIX.length()));
        }
        // 兼容历史数据：旧版本没有前缀，使用旧密钥解密
        return decryptWith(legacyKey, encryptedPassword);
    }

    /**
     * 验证密码是否匹配。
     */
    public boolean matches(String rawPassword, String encryptedPassword) {
        if (rawPassword == null || encryptedPassword == null) {
            return false;
        }
        try {
            return rawPassword.equals(decrypt(encryptedPassword));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 判断给定字符串是否是本服务产生的密文。
     * 不再使用"长度 + Base64"这类启发式判断，而是真正尝试解密，
     * 避免把恰好是 Base64 形态的明文密码误判为密文。
     *
     * @param password 待判断的字符串
     * @return 是否为本服务可解密的密文
     */
    public boolean isEncrypted(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        if (password.startsWith(CIPHERTEXT_PREFIX)) {
            return true;
        }
        if (password.length() < 32) {
            return false;
        }
        try {
            Base64.getDecoder().decode(password);
            decryptWith(legacyKey, password);
            return true;
        } catch (RuntimeException e) {
            // Base64 解码失败（IllegalArgumentException）或解密失败（IllegalStateException）
            // 都说明这不是本服务产生的密文
            return false;
        }
    }

    /**
     * 返回当前密钥格式版本，便于未来做密钥轮换迁移。
     */
    public String keyVersion() {
        return CIPHERTEXT_PREFIX.substring(0, CIPHERTEXT_PREFIX.length() - 1);
    }

    private String decryptWith(SecretKey secretKey, String base64Payload) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64Payload);
            if (combined.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid ciphertext length");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt password", e);
        }
    }

    /**
     * 读取密钥：环境变量优先；否则读密钥文件，不存在则随机生成并落盘。
     */
    private static String resolveSecret(String keyFile) {
        String fromEnv = System.getenv(KEY_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        Path path = Paths.get(keyFile).toAbsolutePath().normalize();
        try {
            if (Files.exists(path)) {
                String saved = Files.readString(path, StandardCharsets.UTF_8).trim();
                if (!saved.isEmpty()) {
                    return saved;
                }
            }
            String generated = generateSecret();
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, generated + System.lineSeparator(), StandardCharsets.UTF_8);
            restrictPermissions(path);
            return generated;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "无法读取或创建加密密钥文件: " + path
                            + "。请检查目录权限，或通过环境变量 " + KEY_ENV + " 直接提供密钥。", e);
        }
    }

    private static String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 尽力把密钥文件权限收紧到仅当前用户可读写（Windows 上通常不支持该 API，忽略即可）。
     */
    private static void restrictPermissions(Path path) {
        try {
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(path,
                        java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            }
        } catch (Exception ignored) {
            // 非 POSIX 文件系统（如 Windows），保持默认权限即可
        }
    }

    private static byte[] sha256(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * 复现旧版本的密钥派生逻辑，仅用于解密历史密文。
     */
    private static SecretKey legacyKeyFromFixedSeed() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(LEGACY_SEED.getBytes(StandardCharsets.UTF_8));
            keyGenerator.init(KEY_SIZE, secureRandom);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive legacy key", e);
        }
    }
}
