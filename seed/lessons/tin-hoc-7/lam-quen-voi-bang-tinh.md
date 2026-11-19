# Làm quen với phần mềm bảng tính

## Bảng tính dùng để làm gì?

**Phần mềm bảng tính** (như Excel, Google Sheets, LibreOffice Calc) giúp em lưu dữ liệu thành bảng và tính toán tự động. Thay vì bấm máy tính từng phép, em chỉ cần viết công thức một lần, khi dữ liệu thay đổi kết quả tự cập nhật.

## Các thành phần chính

- **Trang tính** gồm nhiều **hàng** (đánh số 1, 2, 3...) và **cột** (đánh chữ A, B, C...).
- **Ô** là giao của một hàng và một cột. Địa chỉ ô ghi cột trước, hàng sau: ô ở cột B, hàng 3 là `B3`.
- **Vùng dữ liệu** là nhóm ô liền nhau, ví dụ `B2:B6` gồm các ô từ B2 đến B6.

## Nhập dữ liệu

Nhấn vào ô rồi gõ nội dung. Dữ liệu có thể là chữ (họ tên), số (điểm), ngày tháng. Số thường được căn lề phải, chữ căn lề trái.

## Công thức

Công thức luôn **bắt đầu bằng dấu `=`**. Ví dụ:

| Ô | Nội dung | Kết quả |
|---|---|---|
| B2 | 8 | 8 |
| C2 | 7 | 7 |
| D2 | `=B2+C2` | 15 |

Em không gõ số 15 vào D2. Nếu đổi B2 thành 9 thì D2 tự thành 16.

## Sao chép công thức

Khi kéo công thức sang hàng bên dưới, địa chỉ ô trong công thức tự thay đổi theo vị trí. Công thức `=B2+C2` ở hàng 2 thành `=B3+C3` ở hàng 3. Nhờ vậy em chỉ viết một công thức cho cả cột.

## Kiểm tra nhanh

1. Ô ở cột D, hàng 5 có địa chỉ là gì?
2. Vì sao nên dùng công thức `=B2+C2` thay vì gõ trực tiếp kết quả?
3. Vùng `A1:A4` gồm những ô nào?

> Bài viết bám theo khung Chương trình giáo dục phổ thông 2018 môn Tin học (Thông tư 32/2018/TT-BGDĐT). Nội dung được soạn lại bằng lời mới, không sao chép sách giáo khoa.
