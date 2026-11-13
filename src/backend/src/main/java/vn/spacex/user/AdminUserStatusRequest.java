package vn.spacex.user;

import jakarta.validation.constraints.NotNull;

// Kiểu Boolean (không phải boolean) để phân biệt "thiếu trường" với false; thiếu thì bị từ chối, không ngầm hiểu là khóa.
public record AdminUserStatusRequest(@NotNull(message = "Trạng thái không được để trống") Boolean enabled) {
}
