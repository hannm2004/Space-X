# Thuật toán tìm kiếm tuần tự và nhị phân

## Bài toán

Cho một danh sách các số và một giá trị cần tìm. Hãy cho biết giá trị đó có trong danh sách không, và nếu có thì ở vị trí nào.

## Tìm kiếm tuần tự

Xem **lần lượt** từng phần tử từ đầu đến cuối cho đến khi gặp giá trị cần tìm hoặc hết danh sách.

```python
def tim_tuan_tu(ds, x):
    for i in range(len(ds)):
        if ds[i] == x:
            return i
    return -1
```

Ưu điểm: đơn giản, dùng được cho mọi danh sách. Nhược điểm: danh sách có n phần tử thì trường hợp xấu nhất phải xem n phần tử.

## Tìm kiếm nhị phân

Yêu cầu quan trọng: danh sách phải được **sắp xếp tăng dần**. Mỗi bước so giá trị cần tìm với phần tử **ở giữa** rồi loại bỏ một nửa danh sách.

```python
def tim_nhi_phan(ds, x):
    lo, hi = 0, len(ds) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if ds[mid] == x:
            return mid
        elif ds[mid] < x:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1
```

## Chạy tay

Danh sách `[2, 5, 8, 12, 16, 23, 38, 56, 72, 91]`, tìm 23:

1. `lo = 0`, `hi = 9`, `mid = 4`: phần tử là 16, nhỏ hơn 23 nên bỏ nửa trái, `lo = 5`.
2. `mid = 7`: phần tử là 56, lớn hơn 23 nên bỏ nửa phải, `hi = 6`.
3. `mid = 5`: phần tử là 23. Tìm thấy ở vị trí 5 sau 3 bước.

Tìm tuần tự phải xem 6 phần tử mới gặp 23.

## So sánh

Mỗi bước nhị phân làm số phần tử cần xét giảm một nửa, nên với 1000 phần tử chỉ cần khoảng 10 bước, trong khi tuần tự có thể cần đến 1000 bước. Đổi lại phải tốn công sắp xếp danh sách trước.

## Kiểm tra nhanh

1. Có thể dùng tìm kiếm nhị phân trên danh sách chưa sắp xếp không? Vì sao?
2. Chạy tay tìm 8 trong danh sách trên bằng thuật toán nhị phân.
3. Khi nào tìm kiếm tuần tự là lựa chọn hợp lý?

> Bài viết bám theo khung Chương trình giáo dục phổ thông 2018 môn Tin học (Thông tư 32/2018/TT-BGDĐT). Nội dung được soạn lại bằng lời mới, không sao chép sách giáo khoa.
