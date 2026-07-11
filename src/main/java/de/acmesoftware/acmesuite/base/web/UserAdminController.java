package de.acmesoftware.acmesuite.base.web;

import de.acmesoftware.acmesuite.base.AccessRole;
import de.acmesoftware.acmesuite.base.UserAdminService;
import de.acmesoftware.acmesuite.base.UserAdminService.DuplicateUsernameException;
import de.acmesoftware.acmesuite.base.domain.UserStatus;
import de.acmesoftware.acmesuite.base.web.AdminViews.CreateUserRequest;
import de.acmesoftware.acmesuite.base.web.AdminViews.CreateUserResponse;
import de.acmesoftware.acmesuite.base.web.AdminViews.RoleRequest;
import de.acmesoftware.acmesuite.base.web.AdminViews.StatusRequest;
import de.acmesoftware.acmesuite.base.web.AdminViews.UserView;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ACMEbase user &amp; role administration (ADMIN only; guarded by URL rule in security config). */
@RestController
@RequestMapping("/api/base/users")
public class UserAdminController {

    private final UserAdminService users;

    public UserAdminController(UserAdminService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserView> list() {
        return users.list().stream().map(AdminViews::user).toList();
    }

    @PostMapping
    public ResponseEntity<CreateUserResponse> create(@RequestBody CreateUserRequest req) {
        Optional<AccessRole> role = role(req.role());
        if (role.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            UserAdminService.Created created =
                    users.createLocalUser(req.username(), req.displayName(), req.email(), role.get());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new CreateUserResponse(AdminViews.user(created.user()), created.temporaryPassword()));
        } catch (DuplicateUsernameException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserView> setRole(@PathVariable String id, @RequestBody RoleRequest req) {
        Optional<AccessRole> role = role(req.role());
        if (role.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return users.setRole(id, role.get())
                .map(u -> ResponseEntity.ok(AdminViews.user(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<UserView> setStatus(@PathVariable String id, @RequestBody StatusRequest req) {
        Optional<UserStatus> status = status(req.status());
        if (status.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return users.setStatus(id, status.get())
                .map(u -> ResponseEntity.ok(AdminViews.user(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static Optional<AccessRole> role(String value) {
        try {
            return Optional.of(AccessRole.valueOf(value));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static Optional<UserStatus> status(String value) {
        try {
            return Optional.of(UserStatus.valueOf(value));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
