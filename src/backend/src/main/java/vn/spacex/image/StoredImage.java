package vn.spacex.image;

// Ảnh đã kiểm quyền, sẵn sàng trả về cho trình duyệt.
public record StoredImage(String contentType, byte[] data) {
}
