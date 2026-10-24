package com.minicat.minicatserver.Utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 密码加密服务单元测试（纯 JUnit，不启动 Spring 上下文）。
 */
class PasswordEncryptionServiceTest {

    @TempDir
    Path tempDir;

    private String keyFile;
    private PasswordEncryptionService service;

    @BeforeEach
    void setUp() {
        keyFile = tempDir.resolve("keys").resolve("secret.key").toString();
        service = new PasswordEncryptionService(keyFile);
    }

    @Test
    void encryptProducesVersionedCiphertextAndRoundTrips() {
        String raw = "mySecretPassword123";
        String encrypted = service.encrypt(raw);

        assertNotEquals(raw, encrypted);
        assertTrue(encrypted.startsWith("v2:"), "新密文必须带版本前缀，便于未来密钥轮换");
        assertTrue(service.isEncrypted(encrypted));
        assertEquals(raw, service.decrypt(encrypted));
        assertTrue(service.matches(raw, encrypted));
        assertFalse(service.matches("wrongPassword", encrypted));
    }

    @Test
    void samePlaintextProducesDifferentCiphertext() {
        String a = service.encrypt("same-password");
        String b = service.encrypt("same-password");
        assertNotEquals(a, b, "GCM 随机 IV 应保证同一明文两次加密结果不同");
        assertEquals("same-password", service.decrypt(b));
    }

    @Test
    void decryptingLegacyCiphertextStillWorks() throws Exception {
        String raw = "legacy-pass-123";
        String legacyCiphertext = legacyEncrypt(raw);

        assertFalse(legacyCiphertext.startsWith("v2:"));
        assertTrue(service.isEncrypted(legacyCiphertext), "旧格式密文必须仍被识别为密文");
        assertEquals(raw, service.decrypt(legacyCiphertext));
        assertTrue(service.matches(raw, legacyCiphertext));
    }

    @Test
    void isEncryptedDoesNotMisjudgePlainText() {
        assertFalse(service.isEncrypted("abcdefghijklmnopqrstuvwxyz123456"));
        assertFalse(service.isEncrypted("mySecretPassword123"));
        assertFalse(service.isEncrypted(null));
        assertFalse(service.isEncrypted(""));
    }

    @Test
    void keyFileIsGeneratedAndReusedAcrossInstances() throws Exception {
        Path key = Path.of(keyFile);
        assertTrue(Files.exists(key), "首次使用应自动生成密钥文件");

        String encrypted = service.encrypt("cross-instance");
        PasswordEncryptionService another = new PasswordEncryptionService(keyFile);
        assertEquals("cross-instance", another.decrypt(encrypted), "同一密钥文件应能跨实例解密");
    }

    /** 复刻 v1.0.0 的加密实现，用于生成旧格式密文，验证升级兼容。 */
    private static String legacyEncrypt(String raw) throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed("MiniCat-Database-Connection-Key-2024!".getBytes(StandardCharsets.UTF_8));
        keyGenerator.init(256, secureRandom);
        SecretKey key = keyGenerator.generateKey();

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return Base64.getEncoder().encodeToString(combined);
    }
}
