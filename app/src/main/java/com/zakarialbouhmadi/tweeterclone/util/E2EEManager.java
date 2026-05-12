package com.zakarialbouhmadi.tweeterclone.util;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

public class E2EEManager {
    private static final String KEY_ALIAS_PREFIX = "TweeterCloneE2EEKey_";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static String keyAlias(int userId) {
        return KEY_ALIAS_PREFIX + userId;
    }

    public static void generateKeyPairIfNeeded(int userId) throws Exception {
        String alias = keyAlias(userId);
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        if (keyStore.containsAlias(alias)) return;

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER);
        kpg.initialize(new KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(2048)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
                .build());
        kpg.generateKeyPair();
    }

    public static String getPublicKeyBase64(int userId) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        PublicKey pub = keyStore.getCertificate(keyAlias(userId)).getPublicKey();
        return Base64.encodeToString(pub.getEncoded(), Base64.NO_WRAP);
    }

    private static PrivateKey getPrivateKey(int userId) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        return (PrivateKey) keyStore.getKey(keyAlias(userId), null);
    }

    private static OAEPParameterSpec oaepSpec() {
        return new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
    }

    private static byte[] rsaEncrypt(byte[] data, PublicKey publicKey) throws Exception {
        Cipher c = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        c.init(Cipher.ENCRYPT_MODE, publicKey, oaepSpec());
        return c.doFinal(data);
    }

    private static byte[] rsaDecrypt(byte[] data, PrivateKey privateKey) throws Exception {
        Cipher c = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        c.init(Cipher.DECRYPT_MODE, privateKey, oaepSpec());
        return c.doFinal(data);
    }

    public static class EncryptedMessage {
        public final String ciphertext;
        public final String encryptedKeyForSender;
        public final String encryptedKeyForReceiver;

        EncryptedMessage(String c, String ks, String kr) {
            ciphertext = c;
            encryptedKeyForSender = ks;
            encryptedKeyForReceiver = kr;
        }
    }

    public static EncryptedMessage encrypt(String plaintext,
                                           String recipientPublicKeyBase64,
                                           String senderPublicKeyBase64) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey aesKey = kg.generateKey();
        byte[] rawAesKey = aesKey.getEncoded();

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] cipherBytes = aesCipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // IV prepended to ciphertext so decrypt can extract it
        byte[] payload = new byte[GCM_IV_LENGTH + cipherBytes.length];
        System.arraycopy(iv, 0, payload, 0, GCM_IV_LENGTH);
        System.arraycopy(cipherBytes, 0, payload, GCM_IV_LENGTH, cipherBytes.length);

        PublicKey recipientKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.decode(recipientPublicKeyBase64, Base64.NO_WRAP)));
        PublicKey senderKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.decode(senderPublicKeyBase64, Base64.NO_WRAP)));

        return new EncryptedMessage(
                Base64.encodeToString(payload, Base64.NO_WRAP),
                Base64.encodeToString(rsaEncrypt(rawAesKey, senderKey), Base64.NO_WRAP),
                Base64.encodeToString(rsaEncrypt(rawAesKey, recipientKey), Base64.NO_WRAP)
        );
    }

    public static String decrypt(String ciphertextBase64, String encryptedKeyBase64, int userId) throws Exception {
        byte[] rawAesKey = rsaDecrypt(
                Base64.decode(encryptedKeyBase64, Base64.NO_WRAP),
                getPrivateKey(userId));

        byte[] payload = Base64.decode(ciphertextBase64, Base64.NO_WRAP);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] cipherBytes = new byte[payload.length - GCM_IV_LENGTH];
        System.arraycopy(payload, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(payload, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.length);

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(rawAesKey, "AES"),
                new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return new String(aesCipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
    }
}
