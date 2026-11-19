# SQL cơ bản: truy vấn dữ liệu

## SQL là gì?

**SQL** (Structured Query Language) là ngôn ngữ dùng để làm việc với cơ sở dữ liệu quan hệ: tạo bảng, thêm, sửa, xóa và truy vấn dữ liệu. Phần dưới dùng bảng `hocsinh`:

| ma_hs | ho_ten | lop | diem_tb |
|---|---|---|---|
| HS01 | Nguyễn Lan | 12A1 | 8.5 |
| HS02 | Trần Minh | 12A2 | 7.0 |
| HS03 | Lê Hoa | 12A1 | 9.0 |

## Đọc dữ liệu với `SELECT`

```sql
SELECT ho_ten, diem_tb
FROM hocsinh
WHERE lop = '12A1'
ORDER BY diem_tb DESC;
```

- `SELECT`: chọn những cột cần xem (dùng `*` để lấy tất cả).
- `FROM`: lấy từ bảng nào.
- `WHERE`: điều kiện lọc bản ghi.
- `ORDER BY`: sắp xếp, `DESC` là giảm dần, `ASC` là tăng dần.

Kết quả: Lê Hoa (9.0) rồi Nguyễn Lan (8.5).

## Thống kê với hàm

```sql
SELECT COUNT(*) FROM hocsinh WHERE diem_tb >= 8;
SELECT AVG(diem_tb) FROM hocsinh;
```

Câu thứ nhất đếm số học sinh có điểm từ 8 trở lên (kết quả 2). Câu thứ hai tính điểm trung bình toàn bảng: (8.5 + 7.0 + 9.0) / 3 = 8.17 (làm tròn).

## Thêm, sửa, xóa

```sql
INSERT INTO hocsinh (ma_hs, ho_ten, lop, diem_tb)
VALUES ('HS04', 'Phạm Nam', '12A2', 6.5);

UPDATE hocsinh SET diem_tb = 7.5 WHERE ma_hs = 'HS02';

DELETE FROM hocsinh WHERE ma_hs = 'HS04';
```

**Cẩn thận:** `UPDATE` và `DELETE` mà **thiếu `WHERE`** sẽ tác động lên mọi bản ghi trong bảng. Hãy kiểm tra bằng `SELECT` với cùng điều kiện trước khi sửa hoặc xóa.

## Ghi chú an toàn

Khi chương trình ghép dữ liệu người dùng nhập vào câu SQL, kẻ xấu có thể chèn lệnh độc (tấn công **SQL injection**). Cách phòng là dùng câu lệnh có tham số thay vì nối chuỗi.

## Kiểm tra nhanh

1. Viết câu truy vấn lấy họ tên các học sinh lớp 12A2.
2. Điều gì xảy ra nếu chạy `DELETE FROM hocsinh;` không có `WHERE`?
3. Khóa chính `ma_hs` giúp ích gì khi sửa điểm một học sinh?

> Bài viết bám theo khung Chương trình giáo dục phổ thông 2018 môn Tin học (Thông tư 32/2018/TT-BGDĐT). Nội dung được soạn lại bằng lời mới, không sao chép sách giáo khoa.
