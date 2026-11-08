package vn.spacex.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.spacex.lesson.Lesson;
import vn.spacex.lesson.LessonNotFoundException;
import vn.spacex.lesson.LessonRepository;
import vn.spacex.lesson.LessonStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    private static final long LESSON_ID = 11L;

    @Mock
    private LessonImageRepository imageRepository;

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private ImageService service;

    private void luuGanId(long id) {
        lenient().when(imageRepository.saveAndFlush(any(LessonImage.class))).thenAnswer(inv -> {
            LessonImage image = inv.getArgument(0);
            ReflectionTestUtils.setField(image, "id", id);
            return image;
        });
    }

    // Có thể gọi nhiều lần trong một test; trả về ảnh được lưu ở lần gọi cuối.
    private LessonImage luuLaiAnh(String tenGoc) {
        byte[] png = ImageInspectorTest.png(10, 10);
        lenient().when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
        luuGanId(7L);

        service.upload(LESSON_ID, tenGoc, png);

        ArgumentCaptor<LessonImage> saved = ArgumentCaptor.forClass(LessonImage.class);
        verify(imageRepository, atLeastOnce()).saveAndFlush(saved.capture());
        return saved.getValue();
    }

    // ---- Tải lên ----

    @Test
    void taiAnhPngThatLuuDungLoaiTheoNoiDungKichThuocVaDuLieu() {
        byte[] png = ImageInspectorTest.png(10, 10);
        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
        luuGanId(7L);

        ImageUploadResponse response = service.upload(LESSON_ID, "so-do.png", png);

        ArgumentCaptor<LessonImage> saved = ArgumentCaptor.forClass(LessonImage.class);
        verify(imageRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getLessonId()).isEqualTo(LESSON_ID);
        assertThat(saved.getValue().getFileName()).isEqualTo("so-do.png");
        assertThat(saved.getValue().getContentType()).isEqualTo("image/png");
        assertThat(saved.getValue().getSizeBytes()).isEqualTo(png.length);
        assertThat(saved.getValue().getData()).isEqualTo(png);
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.url()).isEqualTo("/api/images/7");
    }

    @Test
    void loaiLuuLaLoaiPhatHienTuNoiDungKhongTinDuoiTep() {
        LessonImage anh = luuLaiAnh("hinh.jpg");

        assertThat(anh.getContentType()).isEqualTo("image/png");
    }

    @Test
    void taiAnhVaoBaiKhongTonTaiNemLessonNotFoundVaKhongGhiGi() {
        when(lessonRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.upload(99L, "a.png", ImageInspectorTest.png(10, 10)))
                .isInstanceOf(LessonNotFoundException.class);

        verify(imageRepository, never()).saveAndFlush(any());
    }

    @Test
    void tepKhongPhaiAnhNemUnsupportedVaKhongGhiGi() {
        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.upload(LESSON_ID, "dep.png", "<svg onload=alert(1)>".getBytes()))
                .isInstanceOf(UnsupportedImageTypeException.class);
        assertThatThrownBy(() -> service.upload(LESSON_ID, "rong.png", new byte[0]))
                .isInstanceOf(UnsupportedImageTypeException.class);

        verify(imageRepository, never()).saveAndFlush(any());
    }

    @Test
    void anhVuot1MBNemTooLargeNhungDungBang1MBThiQuaBuocKichThuoc() {
        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.upload(LESSON_ID, "to.png", new byte[1_048_577]))
                .isInstanceOf(ImageTooLargeException.class);
        // Đúng 1 MB không bị chặn ở bước kích thước; dữ liệu rác nên bị chặn ở bước nội dung.
        assertThatThrownBy(() -> service.upload(LESSON_ID, "vua.png", new byte[1_048_576]))
                .isInstanceOf(UnsupportedImageTypeException.class);

        verify(imageRepository, never()).saveAndFlush(any());
    }

    @Test
    void tenTepChiGiuPhanCuoiKhongCoDuongDan() {
        assertThat(luuLaiAnh("../../etc/passwd.png").getFileName()).isEqualTo("passwd.png");
    }

    @Test
    void tenTepKieuWindowsChiGiuPhanCuoi() {
        assertThat(luuLaiAnh("C:\\Users\\hs\\hinh-1.png").getFileName()).isEqualTo("hinh-1.png");
    }

    @Test
    void tenTepBiXoaKyTuDieuKhien() {
        assertThat(luuLaiAnh("a\u0000b\nc\r.png").getFileName()).isEqualTo("abc.png");
    }

    @Test
    void tenTepThieuHoacTrongThiDungTenMacDinhTheoLoaiAnh() {
        assertThat(luuLaiAnh(null).getFileName()).isEqualTo("image.png");
    }

    @Test
    void tenTepChiCoKhoangTrangHoacChiLaDuongDanThiDungTenMacDinh() {
        assertThat(luuLaiAnh("   ").getFileName()).isEqualTo("image.png");
        assertThat(luuLaiAnh("thu-muc/").getFileName()).isEqualTo("image.png");
    }

    @Test
    void tenTepQuaDaiBiCatChoVuaCot255() {
        assertThat(luuLaiAnh("a".repeat(400) + ".png").getFileName()).hasSize(255);
    }

    // ---- Danh sách và xóa ----

    @Test
    void danhSachAnhCuaBaiTraDungCacTruongKhongCoDuLieu() {
        when(lessonRepository.existsById(LESSON_ID)).thenReturn(true);
        LessonImageInfo info = mock(LessonImageInfo.class);
        when(info.getId()).thenReturn(5L);
        when(info.getFileName()).thenReturn("a.png");
        when(info.getContentType()).thenReturn("image/png");
        when(info.getSizeBytes()).thenReturn(123);
        when(imageRepository.findByLessonIdOrderByIdAsc(LESSON_ID)).thenReturn(List.of(info));

        List<ImageResponse> list = service.list(LESSON_ID);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(5L);
        assertThat(list.get(0).fileName()).isEqualTo("a.png");
        assertThat(list.get(0).contentType()).isEqualTo("image/png");
        assertThat(list.get(0).sizeBytes()).isEqualTo(123);
        assertThat(list.get(0).url()).isEqualTo("/api/images/5");
    }

    @Test
    void danhSachAnhCuaBaiKhongTonTaiNemLessonNotFound() {
        when(lessonRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.list(99L)).isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void xoaAnhTonTaiGoiXoaDungId() {
        when(imageRepository.existsById(5L)).thenReturn(true);

        service.delete(5L);

        verify(imageRepository).deleteById(5L);
    }

    @Test
    void xoaAnhKhongTonTaiNemImageNotFound() {
        when(imageRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ImageNotFoundException.class);

        verify(imageRepository, never()).deleteById(any());
    }

    // ---- Đọc ảnh ----

    private LessonImageInfo thongTin(long lessonId) {
        LessonImageInfo info = mock(LessonImageInfo.class);
        lenient().when(info.getLessonId()).thenReturn(lessonId);
        return info;
    }

    private Lesson bai(LessonStatus status) {
        Lesson bai = mock(Lesson.class);
        when(bai.getStatus()).thenReturn(status);
        return bai;
    }

    private void coAnh(long id, LessonImageInfo info) {
        when(imageRepository.findById(eq(id), eq(LessonImageInfo.class))).thenReturn(Optional.of(info));
    }

    private void coDuLieu(long id) {
        LessonImage entity = LessonImage.create(LESSON_ID, "a.png", "image/png", new byte[]{1, 2, 3});
        when(imageRepository.findById(id)).thenReturn(Optional.of(entity));
    }

    @Test
    void hocSinhDocAnhCuaBaiDaDangNhanDuocDuLieuVaLoai() {
        coAnh(5L, thongTin(LESSON_ID));
        Lesson daDang = bai(LessonStatus.PUBLISHED);
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(daDang));
        coDuLieu(5L);

        StoredImage image = service.load(5L, false);

        assertThat(image.contentType()).isEqualTo("image/png");
        assertThat(image.data()).containsExactly(1, 2, 3);
    }

    @Test
    void hocSinhDocAnhCuaBaiNhapNemImageNotFoundVaKhongTaiDuLieuAnh() {
        coAnh(5L, thongTin(LESSON_ID));
        Lesson nhap = bai(LessonStatus.DRAFT);
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(nhap));

        assertThatThrownBy(() -> service.load(5L, false)).isInstanceOf(ImageNotFoundException.class);

        verify(imageRepository, never()).findById(5L);
    }

    @Test
    void hocSinhDocAnhCuaBaiKhongConTonTaiNemImageNotFound() {
        coAnh(5L, thongTin(LESSON_ID));
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.load(5L, false)).isInstanceOf(ImageNotFoundException.class);
    }

    @Test
    void adminDocDuocAnhCuaBaiNhapVaKhongCanTraCuuBai() {
        coAnh(5L, thongTin(LESSON_ID));
        coDuLieu(5L);

        StoredImage image = service.load(5L, true);

        assertThat(image.data()).containsExactly(1, 2, 3);
        verify(lessonRepository, never()).findById(any());
    }

    @Test
    void anhKhongTonTaiNemImageNotFound() {
        when(imageRepository.findById(eq(99L), eq(LessonImageInfo.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.load(99L, true)).isInstanceOf(ImageNotFoundException.class);
        assertThatThrownBy(() -> service.load(99L, false)).isInstanceOf(ImageNotFoundException.class);
    }
}
