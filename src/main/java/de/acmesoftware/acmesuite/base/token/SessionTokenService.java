package de.acmesoftware.acmesuite.base.token;

import de.acmesoftware.acmesuite.base.AuthProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues the Base session token after a successful login (local or federated). Downstream module
 * APIs validate this token only — they never see the external IdP. Subject = Base user id; the
 * {@code role} claim carries the locally assigned {@code AccessRole}.
 */
@Service
public class SessionTokenService {

    private final JwtEncoder encoder;
    private final Duration ttl;

    public SessionTokenService(JwtEncoder encoder, AuthProperties props) {
        this.encoder = encoder;
        this.ttl = props.getJwt().getTtl();
    }

    public String issue(String userId, String role, String displayName) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("acmebase")
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(userId)
                .claim("role", role)
                .claim("name", displayName == null ? "" : displayName)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
