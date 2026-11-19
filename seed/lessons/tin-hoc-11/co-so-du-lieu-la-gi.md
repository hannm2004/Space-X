# Cơ sở dữ liệu là gì

## Vấn đề của dữ liệu rời rạc

Giả sử thư viện trường ghi sách mượn vào nhiều tệp văn bản khác nhau. Sau một thời gian: cùng một học sinh có tên viết khác nhau ở mỗi tệp, tìm "ai đang mượn cuốn nào" rất lâu, sửa một thông tin phải sửa ở nhiều nơi. Dữ liệu bị **trùng lặp** và dễ **mâu thuẫn**.

## Cơ sở dữ liệu

**Cơ sở dữ liệu** (CSDL) là tập hợp dữ liệu có tổ chức, được lưu trữ để nhiều người và nhiều chương trình có thể truy xuất, cập nhật. Phần mềm quản lý CSDL gọi là **hệ quản trị cơ sở dữ liệu** (hệ QTCSDL).

## Bảng, trường, bản ghi

Loại CSDL phổ biến nhất lưu dữ liệu trong các **bảng**:

| ma_hs | ho_ten | lop | diem_tb |
|---|---|---|---|
| HS01 | Nguyễn Lan | 11A1 | 8.5 |
| HS02 | Trần Minh | 11A2 | 7.0 |
| HS03 | Lê Hoa | 11A1 | 9.0 |

- Mỗi **cột** là một **trường** (thuộc tính): `ma_hs`, `ho_ten`...
- Mỗi **hàng** là một **bản ghi**: thông tin về một học sinh.
- **Khóa chính** là trường (hoặc nhóm trường) có giá trị **không trùng** ở mọi bản ghi, dùng để nhận ra duy nhất một bản ghi. Ở bảng trên, `ma_hs` là khóa chính; `ho_ten` thì không vì có thể trùng tên.

## Lợi ích

- Giảm trùng lặp, dữ liệu nhất quán.
- Tìm kiếm, thống kê nhanh.
- Nhiều người dùng chung, có phân quyền truy cập.
- Dễ sao lưu và bảo vệ.

## Kiểm tra nhanh

1. Trong bảng trên có bao nhiêu bản ghi và bao nhiêu trường?
2. Vì sao không nên chọn `ho_ten` làm khóa chính?
3. Nêu một vấn đề xảy ra khi lưu thông tin sinh viên rải rác trong nhiều tệp.

> Bài viết bám theo khung Chương trình giáo dục phổ thông 2018 môn Tin học (Thông tư 32/2018/TT-BGDĐT). Nội dung được soạn lại bằng lời mới, không sao chép sách giáo khoa.
