package com.vyaparsetu.auth;

import com.vyaparsetu.auth.dto.RegisterRequest;
import com.vyaparsetu.auth.repository.RefreshTokenRepository;
import com.vyaparsetu.auth.service.AuthService;
import com.vyaparsetu.auth.service.OtpService;
import com.vyaparsetu.common.config.AppProperties;
import com.vyaparsetu.common.enums.RoleName;
import com.vyaparsetu.common.exception.BaseException;
import com.vyaparsetu.common.security.JwtTokenProvider;
import com.vyaparsetu.user.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** SECURITY: public self-registration must not allow privileged roles. */
class AuthServiceTest {

    private AuthService newService(UserRepository userRepository) {
        return new AuthService(userRepository, mock(RoleRepository.class),
                mock(RetailerRepository.class), mock(SupplierRepository.class),
                mock(RefreshTokenRepository.class), mock(OtpService.class),
                mock(JwtTokenProvider.class), mock(PasswordEncoder.class), new AppProperties());
    }

    @Test
    void registerRejectsAdminRole() {
        UserRepository userRepository = mock(UserRepository.class);
        AuthService service = newService(userRepository);

        RegisterRequest req = new RegisterRequest("Hacker", "9876543210", null,
                RoleName.ADMIN, null, null, null, null, null, null, null, null, null, null, null, null);

        BaseException ex = assertThrows(BaseException.class, () -> service.register(req));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsNullRole() {
        AuthService service = newService(mock(UserRepository.class));
        RegisterRequest req = new RegisterRequest("X", "9876543210", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertThrows(BaseException.class, () -> service.register(req));
    }
}
