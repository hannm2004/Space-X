package vn.spacex.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoverImageLookupTest {

    @Mock
    private LessonImageRepository imageRepository;

    @InjectMocks
    private CoverImageLookup lookup;

    private static CoverRef bia(long baiId, long anhId) {
        CoverRef ref = mock(CoverRef.class);
        when(ref.getLessonId()).thenReturn(baiId);
        when(ref.getImageId()).thenReturn(anhId);
        return ref;
    }

    @Test
    void doiMoiBaiCoAnhBiaThanhDuongDanAnh() {
        CoverRef bia11 = bia(11L, 5L);
        CoverRef bia13 = bia(13L, 8L);
        when(imageRepository.findCovers(List.of(11L, 12L, 13L))).thenReturn(List.of(bia11, bia13));

        Map<Long, String> ketQua = lookup.urlsByLessonId(List.of(11L, 12L, 13L));

        assertThat(ketQua).containsOnly(
                Map.entry(11L, "/api/images/5"), Map.entry(13L, "/api/images/8"));
    }

    @Test
    void khongCoBaiNaoThiKhongHoiDatabase() {
        assertThat(lookup.urlsByLessonId(List.of())).isEmpty();

        verifyNoInteractions(imageRepository);
    }
}
