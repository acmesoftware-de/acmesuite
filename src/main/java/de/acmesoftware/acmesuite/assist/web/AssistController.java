package de.acmesoftware.acmesuite.assist.web;

import de.acmesoftware.acmesuite.assist.AssistEvent;
import de.acmesoftware.acmesuite.assist.AssistProperties;
import de.acmesoftware.acmesuite.assist.AssistRequest;
import de.acmesoftware.acmesuite.assist.AssistantEngine;
import de.acmesoftware.acmesuite.assist.CallerContext;
import de.acmesoftware.acmesuite.assist.audit.AssistAuditService;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * ACMEassist HTTP API (phase-1 slice, ADR-0008). {@code POST /messages} streams a turn over SSE;
 * {@code GET /capabilities} reports what is available to the caller. The turn runs on a bounded
 * pool and forwards each {@link AssistEvent} to the emitter. Reachable by any authenticated user
 * (WATCH+); write safety comes from the tool layer, not this endpoint (see ADR-0008).
 */
@RestController
@RequestMapping("/api/base/assist")
public class AssistController {

    private static final Logger LOG = LoggerFactory.getLogger(AssistController.class);

    private final AssistProperties props;
    private final AssistantEngine engine;
    private final AssistAuditService audit;
    private final ExecutorService stream = Executors.newFixedThreadPool(8, runnable -> {
        Thread thread = new Thread(runnable, "assist-sse");
        thread.setDaemon(true);
        return thread;
    });

    public AssistController(AssistProperties props, AssistantEngine engine, AssistAuditService audit) {
        this.props = props;
        this.engine = engine;
        this.audit = audit;
    }

    @PostMapping(value = "/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter messages(@RequestBody AssistRequest request, HttpServletRequest http,
            Principal principal) {
        if (!props.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ACMEassist is disabled");
        }
        String user = principal == null ? "anonymous" : principal.getName();
        String role = roleOf(principal);
        String provider = props.providerOrDefault();
        // Captured on the request thread so the async turn can dispatch tools AS this user.
        String baseUrl = http.getScheme() + "://" + http.getServerName() + ":" + http.getServerPort();
        CallerContext caller = new CallerContext(user, http.getHeader("Authorization"), baseUrl);

        List<String> tools = new CopyOnWriteArrayList<>();
        AtomicBoolean failed = new AtomicBoolean(false);
        SseEmitter emitter = new SseEmitter(30_000L);
        stream.execute(() -> {
            try {
                engine.converse(request, caller, event -> {
                    track(event, tools, failed);
                    emit(emitter, event);
                });
                emitter.complete();
            } catch (RuntimeException e) {
                failed.set(true);
                emitter.completeWithError(e);
            } finally {
                try {
                    audit.record(request.conversationId(), user, role, "customer-360", provider,
                            tools, failed.get());
                } catch (RuntimeException e) {
                    LOG.warn("assist audit write failed", e);
                }
            }
        });
        return emitter;
    }

    private static void track(AssistEvent event, List<String> tools, AtomicBoolean failed) {
        if (event instanceof AssistEvent.ToolCall toolCall) {
            tools.add(toolCall.tool());
        } else if (event instanceof AssistEvent.Error) {
            failed.set(true);
        }
    }

    private static String roleOf(Principal principal) {
        if (principal instanceof Authentication authentication) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String name = authority.getAuthority();
                if (name.startsWith("ROLE_")) {
                    return name.substring("ROLE_".length());
                }
            }
        }
        return "";
    }

    private static void emit(SseEmitter emitter, AssistEvent event) {
        try {
            emitter.send(SseEmitter.event().name(event.type()).data(event, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @GetMapping("/capabilities")
    public Capabilities capabilities() {
        return new Capabilities(props.providerOrDefault(), List.of("customer-360"));
    }

    /** What the assistant can do for the caller (phase 1: one read-only agent). */
    public record Capabilities(String provider, List<String> agents) {
    }

    @PreDestroy
    void shutdown() {
        stream.shutdown();
    }
}
