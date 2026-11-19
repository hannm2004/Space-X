# Trang web cơ bản với HTML

## HTML là gì?

**HTML** là ngôn ngữ đánh dấu dùng để mô tả **nội dung và cấu trúc** của trang web. Trình duyệt đọc tệp HTML rồi hiển thị thành trang em thấy. HTML không phải ngôn ngữ lập trình; nó dùng các **thẻ** để gắn ý nghĩa cho nội dung.

## Bộ khung của một trang

```html
<!DOCTYPE html>
<html lang="vi">
  <head>
    <meta charset="UTF-8">
    <title>Trang của em</title>
  </head>
  <body>
    <h1>Xin chào</h1>
    <p>Đây là trang web đầu tiên của em.</p>
  </body>
</html>
```

- `<head>` chứa thông tin về trang (tiêu đề trên thẻ trình duyệt, bảng mã).
- `<body>` chứa nội dung hiển thị.
- Hầu hết thẻ có **thẻ mở và thẻ đóng**: `<p>...</p>`.

## Các thẻ thường dùng

| Thẻ | Dùng để |
|---|---|
| `<h1>` đến `<h6>` | Tiêu đề, từ lớn đến nhỏ |
| `<p>` | Đoạn văn |
| `<a href="...">` | Liên kết sang trang khác |
| `<img src="..." alt="...">` | Hình ảnh, `alt` mô tả ảnh cho người dùng trình đọc màn hình |
| `<ul>`, `<ol>`, `<li>` | Danh sách không thứ tự, có thứ tự và các mục |

Ví dụ liên kết và danh sách:

```html
<a href="https://example.com">Xem thêm</a>
<ul>
  <li>Học HTML</li>
  <li>Học CSS</li>
</ul>
```

## HTML và CSS

HTML nói trang **có gì**. **CSS** nói trang **trông thế nào**: màu sắc, cỡ chữ, bố cục. Tách hai phần giúp thay đổi giao diện mà không phải sửa nội dung.

## Thực hành

Tạo tệp `index.html` bằng trình soạn thảo văn bản, dán đoạn mã bộ khung, lưu rồi mở bằng trình duyệt. Sửa nội dung, lưu và tải lại trang để thấy thay đổi.

## Kiểm tra nhanh

1. Thẻ nào dùng để chèn ảnh? Vì sao nên có thuộc tính `alt`?
2. Phần nào của trang chứa nội dung hiển thị: `head` hay `body`?
3. Nêu sự khác nhau về vai trò giữa HTML và CSS.

> Bài viết bám theo khung Chương trình giáo dục phổ thông 2018 môn Tin học (Thông tư 32/2018/TT-BGDĐT). Nội dung được soạn lại bằng lời mới, không sao chép sách giáo khoa.
