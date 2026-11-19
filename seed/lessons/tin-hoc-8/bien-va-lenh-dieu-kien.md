# Biến và lệnh điều kiện trong lập trình

## Biến

Khi chương trình cần nhớ một giá trị (điểm số, số lượt chơi, tên người dùng), nó lưu giá trị đó trong một **biến**. Có thể hình dung biến là chiếc hộp có dán nhãn tên, bên trong cất một giá trị. Giá trị có thể thay đổi trong lúc chương trình chạy, đó là lý do gọi là "biến".

Ví dụ trò chơi bắt sao:

- Tạo biến `diem`, ban đầu `diem = 0`.
- Mỗi lần bắt được sao: `diem = diem + 1`.

Trong các ngôn ngữ lập trình kéo thả như Scratch, khối "đặt biến … là …" dùng để gán giá trị, còn "thay đổi biến … một lượng …" dùng để tăng hoặc giảm.

## Lệnh điều kiện

Cuộc sống có những lúc ta chọn việc làm tùy theo hoàn cảnh: *nếu trời mưa thì mang ô, nếu không thì đội mũ*. Chương trình cũng vậy, và dùng **lệnh điều kiện** (rẽ nhánh):

```
nếu điều_kiện thì
    làm việc A
nếu không thì
    làm việc B
```

Điều kiện là một phép so sánh cho kết quả **đúng** hoặc **sai**, ví dụ `diem >= 10`.

## Ví dụ kết hợp

```
nếu diem >= 10 thì
    hiện "Bạn thắng!"
nếu không thì
    hiện "Chơi tiếp nhé"
```

Nếu `diem` là 12, chương trình hiện "Bạn thắng!". Nếu `diem` là 7, nó hiện "Chơi tiếp nhé".

## Lỗi thường gặp

- Quên gán giá trị ban đầu cho biến.
- Nhầm phép so sánh `>=` với phép gán `=`.
- Viết điều kiện không bao giờ đúng nên một nhánh không bao giờ chạy.

## Kiểm tra nhanh

1. Biến `so_mang` ban đầu bằng 3, mỗi lần bị trúng bẫy giảm 1. Sau hai lần trúng bẫy, `so_mang` bằng bao nhiêu?
2. Viết điều kiện cho trò chơi: nếu `so_mang` bằng 0 thì hiện "Hết lượt".

> Bài viết bám theo khung Chương trình giáo dục phổ thông 2018 môn Tin học (Thông tư 32/2018/TT-BGDĐT). Nội dung được soạn lại bằng lời mới, không sao chép sách giáo khoa.
