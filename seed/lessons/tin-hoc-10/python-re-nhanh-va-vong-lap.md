# Python: rẽ nhánh và vòng lặp

## Rẽ nhánh với `if`

```python
diem = float(input("Nhập điểm: "))
if diem >= 8:
    print("Giỏi")
elif diem >= 6.5:
    print("Khá")
else:
    print("Cần cố gắng")
```

Máy kiểm tra điều kiện từ trên xuống, nhánh nào **đúng trước** thì chạy rồi bỏ qua phần còn lại. Chú ý hai điều:

- Cuối dòng điều kiện có dấu hai chấm `:`.
- Các lệnh thuộc nhánh phải **thụt lề** cùng một mức. Python dùng thụt lề để biết lệnh nào nằm trong nhánh.

Các phép so sánh: `==` (bằng), `!=` (khác), `<`, `<=`, `>`, `>=`. Có thể ghép điều kiện bằng `and`, `or`, `not`.

## Vòng lặp `for`

Dùng khi biết trước số lần lặp.

```python
tong = 0
for i in range(1, 6):
    tong = tong + i
print(tong)
```

`range(1, 6)` cho các số 1, 2, 3, 4, 5 (**không gồm 6**). Sau vòng lặp, `tong` bằng 15.

## Vòng lặp `while`

Dùng khi lặp đến khi điều kiện không còn đúng.

```python
so = int(input("Nhập số dương: "))
while so <= 0:
    so = int(input("Sai rồi, nhập lại: "))
print("Cảm ơn")
```

Chương trình hỏi lại cho đến khi người dùng nhập số dương. Cần chắc chắn trong vòng lặp có lệnh làm điều kiện dần thành sai, nếu không sẽ lặp vô hạn.

## Kiểm tra nhanh

1. Viết chương trình in các số chẵn từ 2 đến 10.
2. `range(5)` cho những giá trị nào?
3. Vì sao thiếu thụt lề thì chương trình Python chạy sai hoặc báo lỗi?

> Bài viết bám theo khung Chương trình giáo dục phổ thông 2018 môn Tin học (Thông tư 32/2018/TT-BGDĐT). Nội dung được soạn lại bằng lời mới, không sao chép sách giáo khoa.
