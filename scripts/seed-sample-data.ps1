<#
.SYNOPSIS
  Nạp dữ liệu mẫu (3 khóa Tin học 10/11/12 và vài bài mẫu) vào Space-X qua API Admin.

.DESCRIPTION
  Đọc seed/courses.json, seed/lessons.json và các tệp Markdown trong seed/lessons/, rồi gọi API Admin
  (đăng nhập, tạo khóa, tạo bài, đăng bài). Chạy lại nhiều lần an toàn: khóa hoặc bài đã có (theo slug) bị bỏ qua,
  không bao giờ ghi đè nội dung đã sửa.

  Không đụng schema và không cần migration. Mật khẩu chỉ nằm trong bộ nhớ của lần chạy, không ghi ra tệp hay màn hình.

.PARAMETER BaseUrl
  Địa chỉ ứng dụng. Mặc định http://localhost:8080.

.PARAMETER FromEnv
  Đọc ADMIN_EMAIL và ADMIN_PASSWORD từ tệp .env ở gốc dự án (chỉ dùng ở máy). Không có cờ này thì script hỏi
  email và mật khẩu lúc chạy.

.PARAMETER NoPublish
  Chỉ tạo bài ở trạng thái nháp, không đăng.

.PARAMETER DryRun
  Chỉ in những gì sẽ làm, không tạo gì.

.EXAMPLE
  .\scripts\seed-sample-data.ps1 -FromEnv

.EXAMPLE
  .\scripts\seed-sample-data.ps1 -BaseUrl https://space-x-47f1.onrender.com -DryRun
#>
param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$FromEnv,
    [switch]$NoPublish,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$seed = Join-Path $root "seed"
$utf8 = New-Object Text.UTF8Encoding($false)
$dir = Join-Path $env:TEMP ("spacex-seed-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $dir | Out-Null
$jar = Join-Path $dir "cookies.txt"

function Read-Seed($relative) {
    return [IO.File]::ReadAllText((Join-Path $seed $relative), [Text.Encoding]::UTF8)
}

# Windows PowerShell 5.1 trả mảng JSON của ConvertFrom-Json như MỘT đối tượng (và "[]" thành $null), còn PowerShell 7 thì
# tách phần tử. Hàm này cho cùng một kết quả ở cả hai bản: mảng các phần tử, rỗng thì là mảng rỗng.
function ConvertFrom-JsonArray($text) {
    $parsed = $text | ConvertFrom-Json
    if ($null -eq $parsed) { return @() }
    return @($parsed | ForEach-Object { $_ })
}

function Get-Xsrf {
    $line = Select-String -Path $jar -Pattern "XSRF-TOKEN" | Select-Object -Last 1
    return $line.Line.Split("`t")[-1]
}

# Gửi yêu cầu bằng curl.exe. Thân JSON được ghi ra tệp UTF-8 rồi gửi bằng --data-binary @tệp, vì Windows PowerShell 5.1
# làm mất dấu " khi truyền chuỗi JSON thẳng cho chương trình ngoài.
function Send($method, $path, $body) {
    $out = Join-Path $dir "out.txt"
    $args2 = @("-s", "-m", "90", "-b", $jar, "-c", $jar, "-X", $method, "-o", $out, "-w", "%{http_code}", "$BaseUrl$path")
    if ($method -ne "GET") { $args2 += @("-H", ("X-XSRF-TOKEN: " + (Get-Xsrf))) }
    if ($null -ne $body) {
        $file = Join-Path $dir "body.json"
        [IO.File]::WriteAllText($file, ($body | ConvertTo-Json -Depth 5 -Compress), $utf8)
        $args2 += @("-H", "Content-Type: application/json; charset=utf-8", "--data-binary", "@$file")
    }
    $code = [int](& curl.exe @args2)
    $text = [IO.File]::ReadAllText($out, [Text.Encoding]::UTF8)
    return @{ Code = $code; Text = $text }
}

function Require($result, $expected, $what) {
    if ($result.Code -ne $expected) {
        $detail = if ($result.Text.Length -gt 300) { $result.Text.Substring(0, 300) } else { $result.Text }
        throw "$what thất bại: HTTP $($result.Code) $detail"
    }
}

try {
    $isLocal = $BaseUrl -match '^https?://(localhost|127\.0\.0\.1)(:\d+)?(/|$)'

    if ($FromEnv) {
        $envText = Get-Content (Join-Path $root ".env") -Raw
        $email = ([regex]::Match($envText, '(?m)^ADMIN_EMAIL=(.*)$')).Groups[1].Value.Trim()
        $password = ([regex]::Match($envText, '(?m)^ADMIN_PASSWORD=(.*)$')).Groups[1].Value.Trim()
        if (-not $email -or -not $password) { throw "Thiếu ADMIN_EMAIL hoặc ADMIN_PASSWORD trong .env" }
    }
    else {
        $email = Read-Host "Email Admin"
        $secure = Read-Host "Mật khẩu (không hiện khi gõ)" -AsSecureString
        $password = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
    }

    if (-not $isLocal -and -not $DryRun) {
        Write-Host "Bạn sắp nạp dữ liệu mẫu vào $BaseUrl (không phải máy của bạn)."
        if ((Read-Host "Gõ yes để tiếp tục") -ne "yes") { Write-Host "Đã hủy."; return }
    }

    Write-Host "Kết nối $BaseUrl (Render có thể đang ngủ, chờ tới 60 giây)..."
    & curl.exe -s -m 120 -c $jar -o NUL "$BaseUrl/api/health"
    $r = Send "POST" "/api/auth/login" @{ email = $email; password = $password }
    $password = $null
    Require $r 200 "Đăng nhập Admin"

    # Khóa đã có (danh sách khóa là công khai, có id).
    $r = Send "GET" "/api/courses" $null
    Require $r 200 "Đọc danh sách khóa"
    $courseIds = @{}
    foreach ($c in (ConvertFrom-JsonArray $r.Text)) { $courseIds[$c.slug] = $c.id }

    $createdCourses = 0; $skippedCourses = 0
    foreach ($course in (ConvertFrom-JsonArray (Read-Seed "courses.json"))) {
        if ($courseIds.ContainsKey($course.slug)) {
            Write-Host ("  khóa  {0,-12} đã có, bỏ qua" -f $course.slug); $skippedCourses++; continue
        }
        if ($DryRun) { Write-Host ("  khóa  {0,-12} SẼ TẠO" -f $course.slug); $courseIds[$course.slug] = 0; continue }
        $r = Send "POST" "/api/admin/courses" @{
            slug = $course.slug; title = $course.title; description = $course.description; sortOrder = [int]$course.sortOrder
        }
        Require $r 201 "Tạo khóa $($course.slug)"
        $courseIds[$course.slug] = ($r.Text | ConvertFrom-Json).id
        Write-Host ("  khóa  {0,-12} đã tạo" -f $course.slug); $createdCourses++
    }

    # Bài đã có: duyệt hết các trang của danh sách Admin (kể cả bài nháp).
    $existingLessons = @{}
    $page = 0
    do {
        $r = Send "GET" "/api/admin/lessons?page=$page&size=50" $null
        Require $r 200 "Đọc danh sách bài"
        $items = @(($r.Text | ConvertFrom-Json).items | ForEach-Object { $_ })
        foreach ($l in $items) { $existingLessons[$l.slug] = $true }
        $page++
    } while ($items.Count -eq 50)

    $createdLessons = 0; $skippedLessons = 0; $published = 0
    foreach ($lesson in (ConvertFrom-JsonArray (Read-Seed "lessons.json"))) {
        if ($existingLessons.ContainsKey($lesson.slug)) {
            Write-Host ("  bài   {0,-28} đã có, bỏ qua" -f $lesson.slug); $skippedLessons++; continue
        }
        if ($DryRun) { Write-Host ("  bài   {0,-28} SẼ TẠO trong {1}" -f $lesson.slug, $lesson.courseSlug); continue }
        $r = Send "POST" "/api/admin/lessons" @{
            courseId = [long]$courseIds[$lesson.courseSlug]; slug = $lesson.slug; title = $lesson.title
            summary = $lesson.summary; contentMd = (Read-Seed ("lessons/" + $lesson.file)); sortOrder = [int]$lesson.sortOrder
        }
        Require $r 201 "Tạo bài $($lesson.slug)"
        $id = ($r.Text | ConvertFrom-Json).id
        $createdLessons++
        if (-not $NoPublish) {
            $r = Send "PATCH" "/api/admin/lessons/$id/status" @{ status = "PUBLISHED" }
            Require $r 200 "Đăng bài $($lesson.slug)"
            $published++
            Write-Host ("  bài   {0,-28} đã tạo và đăng" -f $lesson.slug)
        }
        else {
            Write-Host ("  bài   {0,-28} đã tạo (nháp)" -f $lesson.slug)
        }
    }

    Write-Host ""
    if ($DryRun) { Write-Host "DryRun: chưa tạo gì." }
    Write-Host ("Khóa: tạo {0}, bỏ qua {1}. Bài: tạo {2} (đăng {3}), bỏ qua {4}." -f `
            $createdCourses, $skippedCourses, $createdLessons, $published, $skippedLessons)
}
finally {
    Remove-Item -Recurse -Force $dir
}
