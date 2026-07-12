package de.acmesoftware.acmesuite.search.web;

import de.acmesoftware.acmesuite.search.SearchService;
import de.acmesoftware.acmesuite.search.SearchViews;
import de.acmesoftware.acmesuite.shared.SearchDocument;
import java.util.Set;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Central search API across all modules. GET is a read (WATCH+); results are filtered to the
 * caller's audiences. Reindex is ADMIN-only (guarded by URL rule in the security config).
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService search;

    public SearchController(SearchService search) {
        this.search = search;
    }

    @GetMapping
    public SearchViews.Results search(@RequestParam String q,
            @RequestParam(required = false) Set<String> types,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal Jwt jwt) {
        return new SearchViews.Results(q, search.query(q, types, audiences(jwt), limit));
    }

    @PostMapping("/reindex")
    public SearchViews.ReindexResult reindex(@RequestParam(required = false) String module) {
        return new SearchViews.ReindexResult(module, search.reindex(module));
    }

    /** Everyone authenticated sees the public audience; the role is added for future ACLs. */
    private static Set<String> audiences(Jwt jwt) {
        if (jwt == null) {
            return Set.of(SearchDocument.AUDIENCE_ALL);
        }
        String role = jwt.getClaimAsString("role");
        return role == null ? Set.of(SearchDocument.AUDIENCE_ALL)
                : Set.of(SearchDocument.AUDIENCE_ALL, role);
    }
}
