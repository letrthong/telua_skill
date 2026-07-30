package com.example.app.examples;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Standard Repository Pattern Reference Template.
 *
 * Key Principles Demonstrated:
 * 1. Interface abstraction for Unit Testability and Dependency Injection.
 * 2. Comprehensive Null Safety using Optional.
 * 3. Defensive Encapsulation using unmodifiable collection views.
 * 4. Immutable data container using Java Record.
 */
public class RepositoryPatternTemplate {

    /**
     * Immutable Data Model.
     */
    public record UserProfile(
        String userId,
        String userName,
        List<String> roles
    ) {
        public UserProfile {
            Objects.requireNonNull(userId, "userId cannot be null");
            Objects.requireNonNull(userName, "userName cannot be null");
            // Defensive copy for list
            roles = Collections.unmodifiableList(Objects.requireNonNull(roles, "roles cannot be null"));
        }
    }

    /**
     * Repository Contract Interface.
     */
    public interface UserRepository {
        Optional<UserProfile> getUserById(String userId);
        List<UserProfile> getAllActiveUsers();
        void saveUser(UserProfile user);
    }

    /**
     * Concrete Repository Implementation.
     */
    public static class UserRepositoryImpl implements UserRepository {
        private final List<UserProfile> mUserCache;

        // Constructor Dependency Injection
        public UserRepositoryImpl(List<UserProfile> initialCache) {
            this.mUserCache = Objects.requireNonNull(initialCache);
        }

        @Override
        public Optional<UserProfile> getUserById(String userId) {
            if (userId == null || userId.isBlank()) {
                return Optional.empty();
            }

            return mUserCache.stream()
                .filter(user -> user.userId().equals(userId))
                .findFirst();
        }

        @Override
        public List<UserProfile> getAllActiveUsers() {
            // Return immutable defensive copy
            return Collections.unmodifiableList(mUserCache);
        }

        @Override
        public void saveUser(UserProfile user) {
            Objects.requireNonNull(user, "User profile to save cannot be null");
            mUserCache.add(user);
        }
    }
}
