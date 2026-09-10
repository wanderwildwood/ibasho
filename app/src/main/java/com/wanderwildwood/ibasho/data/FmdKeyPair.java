package com.wanderwildwood.ibasho.data;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import com.wanderwildwood.ibasho.utils.CypherUtils;

public class FmdKeyPair {
    private PublicKey publicKey;
    private String encryptedPrivateKey;

    // TODO make private
    public FmdKeyPair(PublicKey publicKey, String encryptedPrivateKey) {
        this.publicKey = publicKey;
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public FmdKeyPair(KeyPair rsaKeyPair, String passwordProtectKeyPairWith) {
        String encryptedPrivateKey = CypherUtils.encryptPrivateKeyWithPassword(rsaKeyPair.getPrivate(), passwordProtectKeyPairWith);
        this.publicKey = rsaKeyPair.getPublic();
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public static FmdKeyPair generateNewFmdKeyPair(String passwordProtectKeyPairWith) {
        KeyPair rsaKeyPair = CypherUtils.genRsaKeyPair();
        String encryptedPrivateKey = CypherUtils.encryptPrivateKeyWithPassword(rsaKeyPair.getPrivate(), passwordProtectKeyPairWith);
        return new FmdKeyPair(rsaKeyPair.getPublic(), encryptedPrivateKey);
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getEncryptedPrivateKey() {
        return this.encryptedPrivateKey;
    }

    public String getBase64PublicKey() {
        return CypherUtils.encodeBase64(publicKey.getEncoded());
    }

    public void setEncryptedPrivateKey(String encryptedPrivateKey) {
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public String getFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey.getEncoded());
            return CypherUtils.toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
