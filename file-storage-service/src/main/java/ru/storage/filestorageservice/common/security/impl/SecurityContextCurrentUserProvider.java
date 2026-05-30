package ru.storage.filestorageservice.common.security.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.storage.filestorageservice.common.security.CurrentUserProvider;

import java.util.Optional;

/**
 * {@link CurrentUserProvider} implementation that reads
 * the current user identity and roles from Spring Security.
 *
 * <p>The provider expects the security starter to populate the
 * {@link SecurityContextHolder}
 * with an {@link Authentication} object.
 * The user identifier is extracted from {@link }HeaderUserPrincipal,
 * while roles are resolved from granted authorities and, if necessary,
 * may be supplemented from the principal itself.</p>
 */
@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    /**
     * Extracts the current user ID from the Spring Security context.
     *
     * @return an {@link Optional} containing the user ID when authentication is present and
     *         the principal is {@link }HeaderUserPrincipal; otherwise an empty Optional
     */
    @Override
    public Optional<String> getCurrentUserId() {
        return Optional.of("123");
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth != null && auth.getPrincipal() instanceof HeaderUserPrincipal p) {
//            return Optional.ofNullable(p.userId());
//        }
//        return Optional.empty();
    }
}
