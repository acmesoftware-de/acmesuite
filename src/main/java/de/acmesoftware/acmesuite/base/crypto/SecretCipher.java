package de.acmesoftware.acmesuite.base.crypto;

import de.acmesoftware.acmesuite.base.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM envelope encryption for secrets held at rest in Base (e.g. auth-provider client
 * secrets). The master key comes from {@code acme.base.auth.crypto.master-key} (base64, 32 bytes),
 * sourced from the environment in production. Deliberately behind a small interface so the master
 * key can later be backed by a real secret store (OpenBao/KMS) without touching callers.
 *
 * <p>Wire format: base64( 12-byte random IV || GCM ciphertext+tag ).
 */
@Component
public class SecretCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public SecretCipher(AuthProperties props) {
        byte[] raw = Base64.getDecoder().decode(props.getCrypto().getMasterKey());
        if (raw.length != 32) {
            throw new IllegalStateException(
                    "acme.base.auth.crypto.master-key must be a base64-encoded 32-byte (AES-256) key");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    /** Encrypts a UTF-8 plaintext; returns base64(iv || ciphertext+tag). */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("secret encryption failed", e);
        }
    }

    /** Reverses {@link #encrypt(String)}. */
    public String decrypt(String encoded) {
        try {
            byte[] in = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(in, 0, iv, 0, IV_BYTES);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] pt = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("secret decryption failed", e);
        }
    }
}
