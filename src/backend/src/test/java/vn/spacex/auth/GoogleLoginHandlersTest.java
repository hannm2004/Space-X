package vn.spacex.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import vn.spacex.user.Role;
import vn.spacex.user.User;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleLoginHandlersTest {

    private static final String SUB = "109876543210987654321";
    private static final String EMAIL = "hocsinh@gmail.com";

    @Mock
    private GoogleAccountService accountService;

    private GoogleLoginSuccessHandler successHandler;
    private GoogleLoginFailureHandler failureHandler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        successHandler = new GoogleLoginSuccessHandler(accountService);
        failureHandler = new GoogleLoginFailureHandler();
        request = new MockHttpServletRequest();
        session = (MockHttpSession) request.getSession(true);
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static Authentication googleLogin(Object emailVerified) {
        OidcIdToken.Builder token = OidcIdToken.withTokenValue("token")
                .subject(SUB)
                .claim("email", EMAIL)
                .claim("name", "Học Sinh")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        if (emailVerified != null) {
            token.claim("email_verified", emailVerified);
        }
        OidcIdToken idToken = token.build();
        DefaultOidcUser principal = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken);
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    private static User taiKhoan() {
        User user = User.newGoogleLearner(EMAIL, "Học Sinh", SUB);
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }

    // ---- Đăng nhập thành công ----

    @Test
    void thanhCongLuuNguoiDungCuaUngDungVaoSessionRoiVeTrangChu() throws Exception {
        when(accountService.signIn(any())).thenReturn(taiKhoan());

        successHandler.onAuthenticationSuccess(request, response, googleLogin(true));

        assertThat(response.getRedirectedUrl()).isEqualTo("/");
        SecurityContext saved = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(saved).isNotNull();
        // Phải là AppUserDetails như đăng nhập bằng mật khẩu: /api/me và các controller đọc id từ đối tượng này.
        assertThat(saved.getAuthentication().getPrincipal()).isInstanceOfSatisfying(AppUserDetails.class, me -> {
            assertThat(me.id()).isEqualTo(7L);
            assertThat(me.email()).isEqualTo(EMAIL);
            assertThat(me.role()).isEqualTo(Role.LEARNER);
        });
        assertThat(saved.getAuthentication().getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_LEARNER");
        assertThat(saved.getAuthentication().isAuthenticated()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(saved.getAuthentication());
    }

    @Test
    void thongTinGuiChoServiceLayDungTuIdTokenCuaGoogle() throws Exception {
        when(accountService.signIn(any())).thenReturn(taiKhoan());

        successHandler.onAuthenticationSuccess(request, response, googleLogin(true));

        ArgumentCaptor<GoogleProfile> profile = ArgumentCaptor.forClass(GoogleProfile.class);
        verify(accountService).signIn(profile.capture());
        assertThat(profile.getValue()).isEqualTo(new GoogleProfile(SUB, EMAIL, true, "Học Sinh"));
    }

    @Test
    void khongCoClaimEmailVerifiedThiXemLaChuaXacMinh() throws Exception {
        when(accountService.signIn(any())).thenReturn(taiKhoan());

        successHandler.onAuthenticationSuccess(request, response, googleLogin(null));

        ArgumentCaptor<GoogleProfile> profile = ArgumentCaptor.forClass(GoogleProfile.class);
        verify(accountService).signIn(profile.capture());
        assertThat(profile.getValue().emailVerified()).isFalse();
    }

    // ---- Bị từ chối ----

    @Test
    void biTuChoiThiHuySessionXoaDangNhapVaChuyenVeTrangDangNhapKemMaLoi() throws Exception {
        when(accountService.signIn(any())).thenThrow(
                new GoogleLoginRejectedException(GoogleLoginRejectedException.Reason.EMAIL_USED_BY_PASSWORD));

        successHandler.onAuthenticationSuccess(request, response, googleLogin(true));

        assertThat(response.getRedirectedUrl()).isEqualTo("/dang-nhap?loi=google-email-mat-khau");
        // Bộ lọc OAuth2 đã lưu đăng nhập Google vào session trước khi gọi handler này; phải hủy session.
        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void moiLyDoTuChoiDeuCoMaLoiRieng() throws Exception {
        for (GoogleLoginRejectedException.Reason reason : GoogleLoginRejectedException.Reason.values()) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.getSession(true);
            MockHttpServletResponse res = new MockHttpServletResponse();
            // doThrow thay vì when(...): gọi lại signIn khi nó đã được cài ném lỗi ở vòng trước sẽ tự ném.
            doThrow(new GoogleLoginRejectedException(reason)).when(accountService).signIn(any());

            successHandler.onAuthenticationSuccess(req, res, googleLogin(true));

            assertThat(res.getRedirectedUrl()).isEqualTo("/dang-nhap?loi=" + reason.code());
        }
    }

    @Test
    void nguoiDungKhongPhaiOidcBiTuChoiChungVaServiceKhongDuocGoi() throws Exception {
        DefaultOAuth2User thuong = new DefaultOAuth2User(
                List.of(new OAuth2UserAuthority(Map.of("sub", SUB))), Map.of("sub", SUB), "sub");
        Authentication auth = new OAuth2AuthenticationToken(thuong, thuong.getAuthorities(), "google");

        successHandler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/dang-nhap?loi=google-that-bai");
        assertThat(session.isInvalid()).isTrue();
        verifyNoInteractions(accountService);
    }

    // ---- Google báo lỗi / người dùng bấm Hủy ----

    @Test
    void nguoiDungTuChoiCapQuyenThiVeTrangDangNhapVoiMaHuy() throws Exception {
        failureHandler.onAuthenticationFailure(request, response,
                new OAuth2AuthenticationException(new OAuth2Error("access_denied")));

        assertThat(response.getRedirectedUrl()).isEqualTo("/dang-nhap?loi=google-huy");
    }

    @Test
    void loiKhacThiVeTrangDangNhapVoiMaThatBaiChungKhongLoChiTiet() throws Exception {
        failureHandler.onAuthenticationFailure(request, response,
                new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "chi tiết nội bộ nhạy cảm", null)));
        assertThat(response.getRedirectedUrl()).isEqualTo("/dang-nhap?loi=google-that-bai");

        MockHttpServletResponse res2 = new MockHttpServletResponse();
        failureHandler.onAuthenticationFailure(request, res2, new BadCredentialsException("khác"));
        assertThat(res2.getRedirectedUrl()).isEqualTo("/dang-nhap?loi=google-that-bai");
        assertThat(res2.getRedirectedUrl()).doesNotContain("nhạy cảm");
    }
}
