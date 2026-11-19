# Các hàm cơ bản trong bảng tính

## Hàm là gì?

**Hàm** là công thức viết sẵn có tên riêng, nhận dữ liệu và trả về một kết quả. Cách viết chung: `=TÊN_HÀM(vùng dữ liệu)`.

## Năm hàm thường dùng

| Hàm | Công dụng |
|---|---|
| `SUM` | Tính tổng |
| `AVERAGE` | Tính trung bình cộng |
| `MAX` | Tìm giá trị lớn nhất |
| `MIN` | Tìm giá trị nhỏ nhất |
| `COUNT` | Đếm số ô chứa số |

## Ví dụ bảng điểm

Giả sử các ô `B2:B6` chứa điểm của năm môn: 8, 7, 9, 6, 10.

| Công thức | Kết quả |
|---|---|
| `=SUM(B2:B6)` | 40 |
| `=AVERAGE(B2:B6)` | 8 |
| `=MAX(B2:B6)` | 10 |
| `=MIN(B2:B6)` | 6 |
| `=COUNT(B2:B6)` | 5 |

Em có thể kiểm tra: trung bình = 40 : 5 = 8.

## Lưu ý

- Chọn đúng vùng dữ liệu. Nếu thiếu một ô, kết quả sẽ sai mà máy vẫn không báo lỗi.
- `COUNT` chỉ đếm ô chứa số, bỏ qua ô chữ hoặc ô trống.
- Nếu hiện `#NAME?` hãy kiểm tra lại tên hàm có gõ đúng không.

## Làm thế nào để tự học thêm

Phần mềm có sẵn mục trợ giúp liệt kê hàm theo nhóm. Khi gặp bài toán mới, em hãy tìm hàm phù hợp thay vì tính tay.

## Kiểm tra nhanh

1. Viết công thức tính điểm trung bình của vùng `C2:C11`.
2. Cột D có các số 3, 5, trống, "hai", 7. `COUNT` cho kết quả là bao nhiêu?

> Bài viết bám theo khung Chương trình giáo dục phổ thông 2018 môn Tin học (Thông tư 32/2018/TT-BGDĐT). Nội dung được soạn lại bằng lời mới, không sao chép sách giáo khoa.
