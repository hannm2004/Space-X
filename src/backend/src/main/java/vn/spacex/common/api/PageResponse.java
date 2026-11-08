package vn.spacex.common.api;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// Hình dạng phân trang chung của API (docs/API.md): { items, page, size, totalItems }. Trang đếm từ 0.
public record PageResponse<T>(List<T> items, int page, int size, long totalItems) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
