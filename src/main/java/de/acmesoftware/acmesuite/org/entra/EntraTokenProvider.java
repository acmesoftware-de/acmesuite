package de.acmesoftware.acmesuite.org.entra;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Fetches an app token via the client credentials flow from Entra
 * ({@code https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token}, scope
 * {@code https://graph.microsoft.com/.default}).
 */
class EntraTokenProvider implements EntraTokenSource {

    private final RestClient login;
    private final EntraProperties props;

    EntraTokenProvider(RestClient login, EntraProperties props) {
        this.login = login;
        this.props = props;
    }

    @Override
    public String token() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        form.add("scope", "https://graph.microsoft.com/.default");
        TokenResponse t = login.post()
                .uri("/{tenant}/oauth2/v2.0/token", props.tenantId())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (t == null || t.access_token() == null || t.access_token().isBlank()) {
            throw new IllegalStateException("No access_token received from Entra");
        }
        return t.access_token();
    }

    /** Token response; unknown fields are ignored (Boot Jackson default). */
    record TokenResponse(String access_token, String token_type, Long expires_in) {
    }
}
