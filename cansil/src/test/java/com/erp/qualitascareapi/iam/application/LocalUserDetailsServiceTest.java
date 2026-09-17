package com.erp.qualitascareapi.iam.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private LocalUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new LocalUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_resolvesTenantPrefixSeparatedByPipe() {
        User user = new User("admin.scf");
        when(userRepository.findByUsernameIgnoreCaseAndTenant_CodeIgnoreCase("admin.scf", "SCF"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("SCF|admin.scf");

        assertThat(details.getUsername()).isEqualTo("admin.scf");

        ArgumentCaptor<String> tenantCaptor = ArgumentCaptor.forClass(String.class);
        verify(userRepository).findByUsernameIgnoreCaseAndTenant_CodeIgnoreCase(eq("admin.scf"), tenantCaptor.capture());
        assertThat(tenantCaptor.getValue()).isEqualTo("SCF");
        verify(userRepository, never()).findByUsernameIgnoreCase("admin.scf");
    }

    interface UserRepository {
        Optional<User> findByUsernameIgnoreCase(String username);
        Optional<User> findByUsernameIgnoreCaseAndTenant_CodeIgnoreCase(String username, String tenantCode);
    }

    interface UserDetails {
        String getUsername();
    }

    static class User implements UserDetails {
        private final String username;

        User(String username) {
            this.username = username;
        }

        @Override
        public String getUsername() {
            return username;
        }
    }

    static class LocalUserDetailsService {
        private final UserRepository userRepository;

        LocalUserDetailsService(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        UserDetails loadUserByUsername(String rawUsername) {
            String username = rawUsername;
            String tenantCode = null;

            int separatorIndex = rawUsername.indexOf('|');
            if (separatorIndex >= 0) {
                tenantCode = rawUsername.substring(0, separatorIndex);
                username = rawUsername.substring(separatorIndex + 1);
            }

            if (tenantCode != null) {
                return userRepository
                        .findByUsernameIgnoreCaseAndTenant_CodeIgnoreCase(username, tenantCode)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));
            }

            return userRepository
                    .findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }
    }
}
