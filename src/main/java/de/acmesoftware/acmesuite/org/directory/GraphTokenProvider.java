package de.acmesoftware.acmesuite.org.directory;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * App token via the client-credentials flow ({@code login.microsoftonline.com/{tenant}/oauth2/v2.0/
 * token}, scope {@code https://graph.microsoft.com/.default}). One token per request — the run is
 * short and the caller is not worth a refresh cache.
 */
class GraphTokenProvider {

    private final RestClient login;
    private final DirectoryProperties props;

    GraphTokenProvider(RestClient login, DirectoryProperties props) {
        this.login = login;
        this.props = props;
    }

    String token() {
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
            throw new IllegalStateException("No access_token received from the directory");
        }
        return t.access_token();
    }

    /** Only the access token is read; the rest of the token response is ignored. */
    record TokenResponse(String access_token) {
    }
}
