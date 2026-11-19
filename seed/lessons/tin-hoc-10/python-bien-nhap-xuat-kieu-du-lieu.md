# Python đầu tiên: biến, nhập xuất, kiểu dữ liệu

## Vì sao học Python?

Python là ngôn ngữ lập trình có cú pháp gần với tiếng Anh nên dễ đọc, dễ học, nhưng vẫn đủ mạnh để dùng trong làm web, phân tích dữ liệu hay trí tuệ nhân tạo.

## Xuất dữ liệu với `print`

```python
print("Xin chào Space-X")
print("Tổng là", 3 + 4)
```

Chương trình in ra `Xin chào Space-X` rồi `Tổng là 7`. Các giá trị cách nhau bằng dấu phẩy sẽ được in cách nhau một dấu cách.

## Biến và phép gán

```python
ten = "Lan"
tuoi = 16
tuoi = tuoi + 1
```

Dấu `=` trong Python là **phép gán**: lấy giá trị bên phải cất vào biến bên trái. Sau ba lệnh trên, `tuoi` bằng 17. Tên biến nên có nghĩa, viết bằng chữ thường, không có dấu cách (dùng gạch dưới nếu cần).

## Các kiểu dữ liệu cơ bản

| Kiểu | Ví dụ | Ý nghĩa |
|---|---|---|
| `int` | `5`, `-12` | Số nguyên |
| `float` | `3.14`, `0.5` | Số thực |
| `str` | `"Lan"` | Xâu ký tự |
| `bool` | `True`, `False` | Đúng hoặc sai |

## Nhập dữ liệu với `input`

```python
ten = input("Tên của bạn: ")
tuoi = int(input("Tuổi của bạn: "))
print("Chào", ten)
print("Năm sau bạn", tuoi + 1, "tuổi")
```

Hàm `input` luôn trả về **xâu**. Muốn tính toán, phải đổi sang số bằng `int(...)` (số nguyên) hoặc `float(...)` (số thực). Nếu quên đổi, `"16" + 1` sẽ báo lỗi vì không cộng được xâu với số.

## Lỗi thường gặp

- Quên đóng dấu ngoặc hoặc dấu nháy.
- Gõ sai tên biến (Python phân biệt chữ hoa và chữ thường).
- Nhập chữ khi chương trình chờ số nên `int(...)` báo lỗi.

## Kiểm tra nhanh

1. Viết chương trình nhập hai số nguyên và in tổng của chúng.
2. Sau các lệnh `a = 4`, `b = a * 2`, `a = 1`, biến `b` bằng bao nhiêu?
3. Vì sao `input` thường đi kèm `int` hoặc `float`?

> Bài viết bám theo khung Chương trình giáo dục phổ thông 2018 môn Tin học (Thông tư 32/2018/TT-BGDĐT). Nội dung được soạn lại bằng lời mới, không sao chép sách giáo khoa.
