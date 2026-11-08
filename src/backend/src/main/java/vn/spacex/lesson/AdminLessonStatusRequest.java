package vn.spacex.lesson;

import jakarta.validation.constraints.NotNull;

public record AdminLessonStatusRequest(
        @NotNull(message = "Trạng thái không được để trống")
        LessonStatus status) {
}
