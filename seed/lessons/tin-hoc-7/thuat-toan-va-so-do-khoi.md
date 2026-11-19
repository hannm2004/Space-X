# Thuật toán và sơ đồ khối

## Thuật toán là gì?

**Thuật toán** là một dãy hữu hạn các bước rõ ràng, thực hiện lần lượt để giải một bài toán. Công thức nấu ăn, cách giải phương trình hay hướng dẫn gấp thuyền giấy đều giống thuật toán.

Một thuật toán tốt cần: các bước **rõ ràng**, thực hiện được, và **dừng lại** sau một số bước hữu hạn.

## Ba phần của một thuật toán

- **Đầu vào**: dữ liệu ban đầu (ví dụ ba số a, b, c).
- **Xử lý**: các bước biến đổi.
- **Đầu ra**: kết quả (ví dụ số lớn nhất trong ba số).

## Ví dụ: tìm số lớn nhất trong ba số a, b, c

Mô tả bằng lời:

1. Gán `max = a`.
2. Nếu `b > max` thì gán `max = b`.
3. Nếu `c > max` thì gán `max = c`.
4. Kết quả là `max`.

Thử với a = 4, b = 9, c = 6: ban đầu max = 4; vì 9 > 4 nên max = 9; vì 6 không lớn hơn 9 nên max vẫn là 9. Kết quả đúng.

## Sơ đồ khối

**Sơ đồ khối** mô tả thuật toán bằng hình vẽ:

- Hình bầu dục: **bắt đầu** hoặc **kết thúc**.
- Hình chữ nhật: một bước **xử lý**.
- Hình thoi: **điều kiện** (có hai nhánh Đúng và Sai).
- Mũi tên chỉ thứ tự thực hiện.

Cách vẽ giúp em nhìn ra cấu trúc rẽ nhánh và phát hiện bước còn thiếu trước khi viết chương trình.

## Kiểm tra nhanh

1. Viết thuật toán tìm số nhỏ nhất trong ba số a, b, c.
2. Hình thoi trong sơ đồ khối dùng để làm gì?
3. Vì sao thuật toán phải dừng lại sau hữu hạn bước?

> Bài viết bám theo khung Chương trình giáo dục phổ thông 2018 môn Tin học (Thông tư 32/2018/TT-BGDĐT). Nội dung được soạn lại bằng lời mới, không sao chép sách giáo khoa.
