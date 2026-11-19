# Vòng lặp trong lập trình

## Vì sao cần vòng lặp?

Muốn vẽ một hình vuông, em ra lệnh: đi 100 bước, quay 90 độ; lặp lại bốn lần như thế. Thay vì viết 8 lệnh liên tiếp, ta chỉ cần viết nhóm hai lệnh và bảo máy **lặp 4 lần**. Chương trình ngắn hơn, ít lỗi và dễ sửa hơn.

## Hai kiểu vòng lặp phổ biến

**1. Lặp với số lần biết trước**

```
lặp 4 lần
    đi 100 bước
    quay phải 90 độ
```

**2. Lặp cho đến khi điều kiện xảy ra** (hoặc lặp trong khi điều kiện còn đúng)

```
lặp cho đến khi chạm vào tường
    đi 10 bước
```

Kiểu thứ hai hữu ích khi ta không biết trước cần lặp bao nhiêu lần.

## Vòng lặp kết hợp biến

Tính tổng các số từ 1 đến 5:

```
tong = 0
lặp với i từ 1 đến 5
    tong = tong + i
```

Sau từng vòng: tong lần lượt là 1, 3, 6, 10, 15. Kết quả cuối cùng là 15.

## Cẩn thận với vòng lặp vô hạn

Nếu điều kiện dừng không bao giờ xảy ra, chương trình sẽ lặp mãi. Ví dụ "lặp cho đến khi 1 = 2" thì không bao giờ dừng. Luôn kiểm tra xem trong mỗi vòng có điều gì đưa chương trình đến gần điều kiện dừng không.

## Kiểm tra nhanh

1. Viết vòng lặp để vẽ hình tam giác đều (mỗi lần quay 120 độ).
2. Vòng lặp "lặp 10 lần: tong = tong + 2" với tong ban đầu bằng 0 cho kết quả bao nhiêu?
3. Khi nào nên dùng vòng lặp lặp cho đến khi thay vì lặp số lần cố định?

> Bài viết bám theo khung Chương trình giáo dục phổ thông 2018 môn Tin học (Thông tư 32/2018/TT-BGDĐT). Nội dung được soạn lại bằng lời mới, không sao chép sách giáo khoa.
