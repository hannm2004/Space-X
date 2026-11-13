package vn.spacex.auth;

import org.junit.jupiter.api.Test;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtherSessionsRevokerTest {

    private static final String EMAIL = "a@example.com";

    @SuppressWarnings("unchecked")
    private final FindByIndexNameSessionRepository<Session> repository = mock(FindByIndexNameSessionRepository.class);

    private final OtherSessionsRevoker revoker = new OtherSessionsRevoker(repository);

    @Test
    void xoaMoiSessionCuaEmailTruSessionHienTai() {
        when(repository.findByPrincipalName(EMAIL)).thenReturn(Map.of(
                "hien-tai", mock(Session.class),
                "may-khac-1", mock(Session.class),
                "may-khac-2", mock(Session.class)));

        revoker.revokeOthers(EMAIL, "hien-tai");

        verify(repository).deleteById("may-khac-1");
        verify(repository).deleteById("may-khac-2");
        verify(repository, never()).deleteById("hien-tai");
    }

    @Test
    void chiCoSessionHienTaiThiKhongXoaGi() {
        when(repository.findByPrincipalName(EMAIL)).thenReturn(Map.of("hien-tai", mock(Session.class)));

        revoker.revokeOthers(EMAIL, "hien-tai");

        verify(repository, never()).deleteById(anyString());
    }

    @Test
    void chiTimSessionTheoDungEmailDuocTruyenVao() {
        when(repository.findByPrincipalName(EMAIL)).thenReturn(Map.of());

        revoker.revokeOthers(EMAIL, "hien-tai");

        verify(repository).findByPrincipalName(EMAIL);
        verify(repository, never()).findByPrincipalName("b@example.com");
    }

    // Admin khóa tài khoản hoặc đặt lại mật khẩu: người bị tác động không có "session hiện tại" nào ở đây, mọi thiết bị đều bị đăng xuất.
    @Test
    void revokeAllXoaMoiSessionCuaEmail() {
        when(repository.findByPrincipalName(EMAIL)).thenReturn(Map.of(
                "may-1", mock(Session.class),
                "may-2", mock(Session.class)));

        revoker.revokeAll(EMAIL);

        verify(repository).deleteById("may-1");
        verify(repository).deleteById("may-2");
    }

    @Test
    void revokeAllKhongCoSessionNaoThiKhongXoaGi() {
        when(repository.findByPrincipalName(EMAIL)).thenReturn(Map.of());

        revoker.revokeAll(EMAIL);

        verify(repository, never()).deleteById(anyString());
    }

    @Test
    void revokeAllChiDungEmailDuocTruyenVaoKhongDungSessionCuaNguoiKhac() {
        when(repository.findByPrincipalName(EMAIL)).thenReturn(Map.of("may-1", mock(Session.class)));

        revoker.revokeAll(EMAIL);

        verify(repository).findByPrincipalName(EMAIL);
        verify(repository, never()).findByPrincipalName("b@example.com");
    }
}
