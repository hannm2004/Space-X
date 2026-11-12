package vn.spacex.progress;

import jakarta.validation.constraints.NotNull;

// Chỉ có trạng thái. Các trường khác trong thân (userId, completedAt...) bị bỏ qua: người dùng và thời điểm do máy chủ quyết định.
public record ProgressRequest(@NotNull ProgressStatus status) {
}
