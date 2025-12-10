# Prompt: Tối ưu HomeScreen với CachedNetworkImage (dựa trên code hiện tại)

Bạn copy toàn bộ nội dung file này, dán vào GPT khác, rồi gửi kèm code HomeScreen hiện tại của bạn phía dưới.

---

## Bối cảnh

Bạn là chuyên gia Flutter (Flutter 3.x, Dart null-safety).

Tôi sẽ gửi cho bạn **CODE THẬT** từ project của tôi:

- Ít nhất gồm:
  - Màn hình Home hiện tại (HomeScreen đang dùng sau login).
  - Các widget con liên quan đến item/ảnh (nếu có).

---

## Mục tiêu tổng quát

1. **Không sửa** file/màn hình Home hiện tại (coi đó là bản “cũ” – không cache).
2. Tạo **thêm**:
   - Một màn hình Home mới (ví dụ `HomeScreenCached`) dựa trên code Home hiện tại, giống gần như 100%, chỉ khác **cách load ảnh** (dùng cached).
   - Một màn “menu demo” sau login, cho phép tôi chọn:
     - Vào Home **cũ** (không cache).
     - Vào Home **mới** (có cache).
3. Mục đích: tôi có thể build **1 lần**, login **1 flow**, rồi dùng menu để vào:
   - Lượt đo 1: chỉ vào Home cũ → đo **cold** của bản cũ.
   - Lượt đo 2: clear cache, chỉ vào Home cached → đo **cold** của bản mới.
4. Không cần bạn triển khai logic đo thời gian phức tạp; warm/cold, đo thế nào là do tôi tự thao tác khi demo.  
   Trọng tâm của bạn là: **tách bản cũ/bản mới rõ ràng, dựa trực tiếp trên HomeScreen hiện có**.

---

## 1. Phân tích code hiện tại

Sau khi tôi gửi code:

- Xác định:
  - Class Home hiện tại là gì (ví dụ: `HomeScreen`, `CoursesHomeScreen`, …).
  - File chứa Home hiện tại tên gì (ví dụ: `home_screen.dart`).
  - Ở đâu đang render ảnh từ URL:
    - `Image.network(...)`
    - `NetworkImage(...)`
    - `FadeInImage.assetNetwork(...)`
    - `CircleAvatar(backgroundImage: NetworkImage(...))`
    - Hoặc bất kỳ cách nào khác load từ network.
- Mô tả **ngắn gọn (2–5 dòng)**:
  - “Hiện tại HomeScreen là class X trong file Y. Ảnh đang được load ở widget A/B/C bằng Z…”

---

## 2. Tạo màn hình Home mới dựa trên Home cũ

- **Không được sửa file Home cũ.**
- Tạo file mới, ví dụ:
  - Nếu file cũ là `home_screen.dart` thì file mới là `home_screen_cached.dart`.
- Trong file mới:
  - Tạo class màn hình mới, ví dụ `HomeScreenCached`.
  - Code của `HomeScreenCached` phải:
    - Copy lại logic từ HomeScreen cũ:
      - Layout: `Scaffold`, `AppBar`, `Body`, `ListView`, `GridView`, `CustomScrollView`… giữ nguyên.
      - Cách load dữ liệu: API, local list, `FutureBuilder`/`StreamBuilder`, state, lazy loading… giữ nguyên.
      - Các widget con (nếu cần, bạn có thể copy sang file mới và chỉnh trong bản copy).
      - `onTap`, navigation, UI text, v.v. giữ nguyên.
    - **Chỉ khác ở chỗ load ảnh: dùng cached ảnh.**

---

## 3. Thay cách load ảnh trong bản mới

Trong bản mới (`HomeScreenCached` + các widget con trong file mới), bạn thay:

- Mọi chỗ `Image.network(url, ...)` → `CachedNetworkImage`.
- Mọi chỗ `NetworkImage(url)` → `CachedNetworkImageProvider` hoặc `CachedNetworkImage`.
- Mọi chỗ `FadeInImage.assetNetwork(...)` → phương án dùng `CachedNetworkImage`.
- Mọi chỗ `CircleAvatar(backgroundImage: NetworkImage(url))` → dùng `CachedNetworkImageProvider(url)`.

Ví dụ (áp **vào code thật của tôi**, không demo giả):

```dart
// CODE CŨ (trong HomeScreen hiện tại) – chỉ để đối chiếu, KHÔNG sửa trong file cũ
leading: Image.network(
  course.imageUrl,
  width: 60,
  height: 60,
  fit: BoxFit.cover,
),

// CODE MỚI (dùng trong HomeScreenCached – file mới)
leading: CachedNetworkImage(
  imageUrl: course.imageUrl,
  width: 60,
  height: 60,
  fit: BoxFit.cover,
  placeholder: (context, url) => const SizedBox(
    width: 60,
    height: 60,
    child: CircularProgressIndicator(),
  ),
  errorWidget: (context, url, error) => const SizedBox(
    width: 60,
    height: 60,
    child: Icon(Icons.error),
  ),
),
```

Nếu ảnh được render trong widget con (ví dụ `CourseItem`, `CourseCard`, …):

- Trong file mới, cũng tạo **bản copy** của widget con đó (ví dụ `CourseItemCached`) và chỉnh cách load ảnh trong bản copy.
- **Không sửa widget gốc.**

---

## 4. Màn “Demo Menu” sau login

Giả sử hiện tại sau login bạn đang `Navigator.pushReplacement` vào HomeScreen cũ.

Bạn tạo thêm một màn mới, ví dụ `DemoMenuScreen` (file mới `demo_menu_screen.dart` hoặc theo cấu trúc của tôi), với yêu cầu:

- Giao diện đơn giản:
  - AppBar: “Demo tối ưu ảnh”.
  - Body: 2 nút lớn:
    - “Home cũ (Image.network)” → `Navigator.push` đến HomeScreen cũ.
    - “Home mới (Cached ảnh)” → `Navigator.push` đến `HomeScreenCached`.

Sau login:

- Thay vì đi thẳng vào Home cũ, chuyển sang `DemoMenuScreen`.

Lưu ý:

- Không tự bịa lại flow login; chỉ chỉnh đúng chỗ điều hướng sau login sao cho có thể mở `DemoMenuScreen`.
- Nếu project của tôi đang dùng `GoRouter` / `auto_route` / navigator khác, bạn phải tích hợp đúng cách, không phá routing hiện tại.

---

## 5. Phụ thuộc và import

- Chỉ rõ cho tôi phần cần thêm vào `pubspec.yaml` (không cần viết cả file), ví dụ:

```yaml
dependencies:
  cached_network_image: ^3.3.1
```

- Trong file mới (`home_screen_cached.dart` và các file liên quan), thêm đầy đủ:

```dart
import 'package:cached_network_image/cached_network_image.dart';
```

- Import các file model, widget con, repository… giống như file Home cũ, để bản cached compile được.

---

## 6. Không được làm

- Không tạo app/demo rời kiểu “project mẫu riêng”; phải làm **trên project hiện tại**.
- Không bịa thêm data, model khác; phải dùng đúng field đã có (ví dụ `course.imageUrl`, `course.title`).
- Không sửa đổi logic nghiệp vụ, không đổi tên class gốc, không xóa/đổi luồng.
- Không viết pseudo-code hoặc để `// TODO`.

---

## 7. Cách trả kết quả

Sau khi xử lý xong, bạn phải trả về:

1. **Giải thích ngắn (5–10 dòng)**:
   - HomeScreen cũ là class nào, file nào.
   - HomeScreenCached (màn mới) là class nào, file nào.
   - `DemoMenuScreen` tên gì, file nào, sau login sẽ đi như thế nào.
   - Ở những chỗ nào trong bản cached bạn đã đổi từ `Image.network` sang `CachedNetworkImage` (nêu tổng quan).

2. **Full code các file mới**:
   - Toàn bộ nội dung file `home_screen_cached.dart` (hoặc tên bạn đặt).
   - Toàn bộ nội dung file `demo_menu_screen.dart` (hoặc tên bạn đặt).
   - Nếu bạn tạo widget con “cached” (ví dụ `CourseItemCached`), cũng phải nằm trong các file này hoặc chỉ rõ file, và gửi **full nội dung file đó**.

3. Nếu cần sửa rất nhẹ một vài dòng ở file login/router để trỏ sang `DemoMenuScreen`, hãy:
   - Giải thích rõ “Sửa ở file X, method Y”.
   - Gửi đoạn code đầy đủ của method/class đó (để tôi copy vào).

---

## 8. Về đo đạc (chỉ gợi ý, không bắt buộc implement)

Bạn có thể gợi ý (bằng lời) cách tôi nên đo “cold”:

- Lượt 1:
  - Clear cache.
  - Login → `DemoMenuScreen` → chỉ bấm “Home cũ”.
  - Đo cảm nhận/thời gian cho bản cũ.

- Lượt 2:
  - Clear cache.
  - Login → `DemoMenuScreen` → chỉ bấm “Home mới (Cached)”.
  - Đo cảm nhận/thời gian cho bản mới.

Không cần bạn viết code `Stopwatch`/log, trừ khi tôi yêu cầu thêm.

---

## Tóm lại

- Dùng HomeScreen hiện tại làm gốc.
- Tạo bản copy có cache ảnh (`HomeScreenCached`) trong file mới.
- Tạo `DemoMenuScreen` để chọn vào Home cũ / Home cached sau login.
- Không đụng file cũ, chỉ đọc để clone sang file mới.
- Trả về **full code** file mới để tôi copy vào project và chạy trực tiếp.
