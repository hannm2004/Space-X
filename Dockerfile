# Giai đoạn 1: build giao diện React thành file tĩnh (dist/)
FROM node:24-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Giai đoạn 2: build JAR Spring Boot, đặt giao diện vào static/ để một ứng dụng phục vụ cả hai
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
# chmod: phòng khi file mất quyền thực thi do sao chép từ Windows, khi đó "./mvnw" báo "Permission denied" trên Linux.
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY backend/src src
COPY --from=frontend /frontend/dist src/main/resources/static
RUN ./mvnw -B -q -DskipTests package && mv target/spacex-*.jar app.jar

# Giai đoạn 3: ảnh chạy thật, chỉ có JRE (nhỏ hơn JDK) và chạy bằng user thường
FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home spacex
WORKDIR /app
COPY --from=backend /backend/app.jar app.jar
USER spacex
EXPOSE 8080
# MaxRAMPercentage: JVM tự co theo giới hạn bộ nhớ của container (Render Free chỉ có 512 MB).
# SerialGC và TieredStopAtLevel=1: giảm thời gian khởi động khi CPU rất ít.
# Render truyền cổng qua biến PORT; mặc định 8080 khi chạy ở máy.
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Dserver.port=${PORT:-8080} -jar app.jar"]
