package vn.spacex.common.web;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

// Frontend React được đóng chung vào JAR (thư mục static). React Router đổi địa chỉ ở trình duyệt, nên khi người dùng
// nhấn F5 hoặc mở thẳng /khoa-hoc/... thì máy chủ phải trả index.html thay vì 404 để React tự dựng lại trang.
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                // Không bật bộ nhớ đệm của chuỗi resolver: nếu bật, mỗi đường dẫn lạ mà người lạ gõ vào sẽ được nhớ
                // lại một mục, làm đầy bộ nhớ của máy chủ gói Free.
                .resourceChain(false)
                .addResolver(new SpaFallbackResolver());
    }

    static class SpaFallbackResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            // File có thật (assets/*.js, favicon...) vẫn được phục vụ như cũ, có kiểm tra không thoát khỏi thư mục static.
            Resource found = super.getResource(resourcePath, location);
            if (found != null) {
                return found;
            }
            // /api/... không tồn tại phải là 404, không được trả trang HTML. Đường dẫn có dấu chấm là yêu cầu một file
            // (không phải trang của React Router), file đó không có thì cũng 404.
            if (resourcePath.equals("api") || resourcePath.startsWith("api/") || resourcePath.contains(".")) {
                return null;
            }
            Resource index = location.createRelative("index.html");
            return index.isReadable() ? index : null;
        }
    }
}
