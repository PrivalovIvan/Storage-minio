package ru.storage.filestorageservice.common.security;

import java.util.Optional;

/**
 * Provides access to the identity and roles of the currently authenticated user.
 *
 * <p>This abstraction isolates business services from the concrete authentication
 * mechanism used by the application, such as Spring Security context, HTTP headers,
 * or JWT claims. It improves testability and prevents leaking transport/security
 * implementation details into domain services.</p>
 */
public interface CurrentUserProvider {

    /**
     * Returns the identifier of the current user, if available.
     *
     * <p>If the request is not authenticated, the security context is empty, or the
     * user identifier cannot be extracted, this method returns {@link Optional#empty()}.</p>
     *
     * @return an {@link Optional} containing the current user ID when present; otherwise an empty Optional
     */
    Optional<String> getCurrentUserId();
}
