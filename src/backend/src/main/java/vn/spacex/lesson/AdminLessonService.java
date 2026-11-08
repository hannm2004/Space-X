package vn.spacex.lesson;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.spacex.common.api.PageResponse;
import vn.spacex.course.CourseNotFoundException;
import vn.spacex.course.CourseRepository;

@Service
public class AdminLessonService {

    // Sắp theo khóa rồi thứ tự rồi id: cùng thứ tự với chỉ mục (course_id, status, sort_order) và ổn định giữa các trang.
    private static final Sort LIST_ORDER = Sort.by("courseId", "sortOrder", "id");

    private final AdminLessonRepository adminLessonRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;

    public AdminLessonService(AdminLessonRepository adminLessonRepository, LessonRepository lessonRepository,
                              CourseRepository courseRepository) {
        this.adminLessonRepository = adminLessonRepository;
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
    }

    // Danh sách dùng entity Lesson (không có content_md) nên không tải nội dung của bất kỳ bài nào.
    @Transactional(readOnly = true)
    public PageResponse<AdminLessonSummaryResponse> list(Long courseId, LessonStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, LIST_ORDER);
        return PageResponse.of(lessonRepository.findForAdmin(courseId, status, pageable),
                AdminLessonSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminLessonResponse get(Long id) {
        return AdminLessonResponse.from(adminLessonRepository.findById(id).orElseThrow(LessonNotFoundException::new));
    }

    // adminId lấy từ session ở controller, không bao giờ từ dữ liệu trình duyệt gửi lên.
    @Transactional
    public AdminLessonResponse create(Long adminId, AdminLessonRequest request) {
        requireCourse(request.courseId());
        if (adminLessonRepository.existsBySlug(request.slug())) {
            throw new LessonSlugAlreadyExistsException();
        }
        AdminLesson lesson = AdminLesson.create(request.courseId(), adminId, request.slug(), request.title(),
                request.summary(), request.contentMd(), request.sortOrder());
        return saveBySlug(lesson);
    }

    @Transactional
    public AdminLessonResponse update(Long id, AdminLessonRequest request) {
        AdminLesson lesson = adminLessonRepository.findById(id).orElseThrow(LessonNotFoundException::new);
        requireCourse(request.courseId());
        if (adminLessonRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new LessonSlugAlreadyExistsException();
        }
        lesson.update(request.courseId(), request.slug(), request.title(), request.summary(),
                request.contentMd(), request.sortOrder());
        return saveBySlug(lesson);
    }

    @Transactional
    public AdminLessonResponse changeStatus(Long id, LessonStatus status) {
        AdminLesson lesson = adminLessonRepository.findById(id).orElseThrow(LessonNotFoundException::new);
        lesson.changeStatus(status);
        return AdminLessonResponse.from(adminLessonRepository.saveAndFlush(lesson));
    }

    // Ảnh, tiến độ và đoạn tri thức của bài bị xóa theo bằng ON DELETE CASCADE của database.
    @Transactional
    public void delete(Long id) {
        AdminLesson lesson = adminLessonRepository.findById(id).orElseThrow(LessonNotFoundException::new);
        adminLessonRepository.delete(lesson);
    }

    private void requireCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException();
        }
    }

    private AdminLessonResponse saveBySlug(AdminLesson lesson) {
        try {
            // saveAndFlush để ràng buộc UNIQUE của slug báo lỗi ngay tại đây (không đợi đến lúc commit).
            return AdminLessonResponse.from(adminLessonRepository.saveAndFlush(lesson));
        } catch (DataIntegrityViolationException e) {
            // Hai Admin dùng cùng slug cùng lúc: cả hai qua được kiểm tra trước, chỉ một người ghi được.
            throw new LessonSlugAlreadyExistsException();
        }
    }
}
