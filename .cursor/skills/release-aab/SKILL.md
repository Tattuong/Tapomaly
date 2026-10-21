---
name: release-aab
description: >-
  Builds a signed Tapomaly release Android App Bundle, bumps versionCode,
  and renames the AAB to Tapomaly-<versionName>-<versionCode>.aab. Use when
  the user asks to build, rebuild, đóng gói, bundleRelease, AAB, APK release,
  upload Play, ký keystore, nâng versionCode, or buildcode.
---

# Tapomaly release AAB

Mỗi khi build bản release thì đổi file name + version, gắn key/pass, và nâng buildcode.

`installDebug` cho emulator không dùng skill này. Không tăng `versionCode` cho bản debug.

## Trước khi build

1. Đọc `versionCode` và `versionName` trong `app/build.gradle.kts`.
2. Nâng `versionCode` đúng 1. Giữ `versionName` trừ khi người dùng yêu cầu số version mới.
3. Sửa xong `versionCode` rồi mới chạy Gradle.

`key.properties` nằm ở thư mục gốc project. Bản release phải ký bằng file này:

- `storeFile=Taopomaly.jks` (cùng thư mục gốc)
- `keyAlias=key0`
- `storePassword` và `keyPassword` lấy từ `key.properties`

Không in mật khẩu. Không đổi sang `app/upload-keystore.jks`. Play đã từ chối key đó.

`app/build.gradle.kts` phải đọc key ở gốc project:

```kotlin
val keystorePropertiesFile = rootProject.file("key.properties")
storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
```

`signingConfig` của `release` phải gắn config đó. Thiếu đoạn này thì sửa lại trước khi build. Không build bản chưa ký.

SHA1 bắt buộc của certificate upload: `CD:55:1F:7B:84:DA:9F:C0:18:3B:8A:8B:D8:0A:16:2E:17:14:06:6B`

## Build

Từ thư mục gốc project:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:bundleRelease
```

Gradle xuất `app/build/outputs/bundle/release/app-release.aab`. Chưa được đưa file này cho người dùng upload.

## Sau khi build

Xác nhận chữ ký:

```bash
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
```

SHA1 phải đúng chuỗi ở trên. Sai thì dừng, không đổi tên và không bảo người dùng upload.

Đúng thì copy sang tên có version:

```text
app/release/Tapomaly-<versionName>-<versionCode>.aab
```

Ví dụ `versionName` `1.1` và `versionCode` `3` thành `app/release/Tapomaly-1.1-3.aab`.

Trả lời bằng đường dẫn file đã đổi tên, `versionName`, `versionCode`, và SHA1.
