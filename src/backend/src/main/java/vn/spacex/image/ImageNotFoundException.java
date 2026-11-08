package vn.spacex.image;

public class ImageNotFoundException extends RuntimeException {

    // Dùng chung cho ảnh không tồn tại và ảnh của bài nháp, để người ngoài không phân biệt được hai trường hợp.
    public ImageNotFoundException() {
        super("Khong tim thay anh");
    }
}
