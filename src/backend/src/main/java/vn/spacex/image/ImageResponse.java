package vn.spacex.image;

// Một dòng trong danh sách ảnh của bài (Admin): không có dữ liệu nhị phân.
public record ImageResponse(Long id, String fileName, String contentType, int sizeBytes, String url) {
}
