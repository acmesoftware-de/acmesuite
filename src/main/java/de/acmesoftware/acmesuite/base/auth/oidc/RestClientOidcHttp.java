package de.acmesoftware.acmesuite.base.auth.oidc;

import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Production {@link OidcHttp} over Spring {@link RestClient}. */
@Component
class RestClientOidcHttp implements OidcHttp {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient http = RestClient.create();

    @Override
    public Map<String, Object> getJson(String url) {
        return http.get().uri(url).retrieve().body(JSON_MAP);
    }

    @Override
    public Map<String, Object> postForm(String url, Map<String, String> form) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        form.forEach(body::add);
        return http.post().uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JSON_MAP);
    }
}
