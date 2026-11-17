package vn.spacex.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // Đăng nhập Google tra theo mã `sub` cố định, không theo email (email có thể đổi hoặc được cấp lại cho người khác).
    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    long countByRoleAndEnabled(Role role, boolean enabled);

    // Dashboard: số tài khoản tạo từ mốc thời gian trở đi (bao gồm đúng mốc).
    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    // Tìm cho Admin: pattern null nghĩa là không lọc. Người gọi đã thoát \ % _ và bọc % hai đầu (AdminUserService).
    // Collation utf8mb4_0900_ai_ci của MySQL nên không phân biệt hoa thường và dấu.
    @Query("""
            select u from User u
            where (:pattern is null
                   or u.email like :pattern escape '\\'
                   or u.displayName like :pattern escape '\\')""")
    Page<User> search(@Param("pattern") String pattern, Pageable pageable);
}
