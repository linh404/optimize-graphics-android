# Prompt demo lazy loading ảnh với RecyclerView

```text
Bạn là Android refactor agent, đang làm trên project Android Java (app demo Graphics).

MỤC TIÊU:
Thêm 1 cặp demo mới về ảnh, tập trung vào **1 kỹ thuật duy nhất**:

> Kỹ thuật: **Lazy loading danh sách ảnh với RecyclerView**  
> (chỉ nạp thêm item ảnh khi người dùng cuộn xuống gần cuối danh sách)

YÊU CẦU BỔ SUNG:
- Danh sách phải hiển thị **ảnh nhìn phân biệt được từng item**:
  - Không được dùng 100 ảnh giống hệt nhau.
- Giải pháp: dùng **1 resource ảnh gốc**, nhưng:
  - Mỗi item được tô **màu khác nhau** (color filter) theo vị trí,
  - Đồng thời hiển thị **TextView “Image #index”** để người xem dễ phân biệt.

Cặp demo:
- BAD:  "5. IMAGE EAGER LOAD (BAD)"  → nạp đủ 100 item ảnh ngay khi mở màn.
- GOOD: "5. IMAGE LAZY LOAD (GOOD)" → ban đầu chỉ nạp 1 phần (ví dụ 20 ảnh), cuộn xuống mới nạp thêm.

Không dùng các kỹ thuật tối ưu khác (không inSampleSize, không Glide/Picasso, không cache phức tạp).  
Ngôn ngữ: **Java**.

------------------------------------------------
A. Cập nhật màn hình chính
------------------------------------------------
1. Tìm Activity/Fragment/layout đang hiển thị danh sách demo (MainActivity hoặc tương tự).
2. Thêm 2 entry mới theo đúng style hiện có:
   - "5. IMAGE EAGER LOAD (BAD)"
   - "5. IMAGE LAZY LOAD (GOOD)"
3. Khi bấm:
   - "5. IMAGE EAGER LOAD (BAD)" → startActivity(ImageEagerLoadActivity.class)
   - "5. IMAGE LAZY LOAD (GOOD)" → startActivity(ImageLazyLoadActivity.class)
4. Giữ nguyên format text/màu/layout của các dòng demo khác, chỉ thêm 2 case mới.

------------------------------------------------
B. Data & ảnh sử dụng chung
------------------------------------------------
1. Dùng một resource ảnh có sẵn trong project, ví dụ:
   - R.drawable.sample_image
   Nếu chưa có, hãy thêm 1 ảnh đơn giản vào res/drawable và đặt tên sample_image (hoặc đặt tên phù hợp rồi dùng thống nhất).
2. Tổng số item:
   ```java
   private static final int TOTAL_IMAGES = 100;
   ```
3. Định nghĩa model cho data (để hiển thị được số thứ tự):
   ```java
   public class ImageItem {
       public final int resId;
       public final int index; // 1..TOTAL_IMAGES

       public ImageItem(int resId, int index) {
           this.resId = resId;
           this.index = index;
       }
   }
   ```
4. Tạo danh sách đầy đủ:
   ```java
   List<ImageItem> fullData = new ArrayList<>();
   for (int i = 0; i < TOTAL_IMAGES; i++) {
       fullData.add(new ImageItem(R.drawable.sample_image, i + 1));
   }
   ```

------------------------------------------------
C. Layout item & layout Activity
------------------------------------------------
1. Item layout dùng chung cho cả BAD và GOOD:
   - Tạo file: res/layout/item_image_simple.xml
   - Nội dung (ảnh + text số thứ tự):
     ```xml
     <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
         android:layout_width="match_parent"
         android:layout_height="wrap_content"
         android:orientation="vertical">

         <ImageView
             android:id="@+id/imageView"
             android:layout_width="match_parent"
             android:layout_height="140dp"
             android:scaleType="centerCrop" />

         <TextView
             android:id="@+id/tvLabel"
             android:layout_width="match_parent"
             android:layout_height="wrap_content"
             android:padding="4dp"
             android:text="Image #1" />
     </LinearLayout>
     ```
   - Mục tiêu: mỗi item có ảnh + dòng chữ “Image #x” để dễ phân biệt.

2. Layout Activity cho 2 màn:
   - res/layout/activity_image_eager_load.xml
   - res/layout/activity_image_lazy_load.xml
   - Hai file giống nhau, chỉ khác tên:
     ```xml
     <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:orientation="vertical">

         <TextView
             android:id="@+id/tvInfo"
             android:layout_width="match_parent"
             android:layout_height="wrap_content"
             android:padding="8dp"
             android:text="Info" />

         <androidx.recyclerview.widget.RecyclerView
             android:id="@+id/recyclerView"
             android:layout_width="match_parent"
             android:layout_height="0dp"
             android:layout_weight="1" />
     </LinearLayout>
     ```

------------------------------------------------
D. Adapter RecyclerView dùng chung
------------------------------------------------
1. Tạo adapter Java:
   - Tên: ImageListAdapter
   - Constructor nhận: List<ImageItem> data.
2. ViewHolder:
   ```java
   static class VH extends RecyclerView.ViewHolder {
       ImageView imageView;
       TextView tvLabel;

       VH(View itemView) {
           super(itemView);
           imageView = itemView.findViewById(R.id.imageView);
           tvLabel = itemView.findViewById(R.id.tvLabel);
       }
   }
   ```
3. onCreateViewHolder:
   - Inflate item_image_simple.xml.
4. onBindViewHolder:
   - Lấy item:
     ```java
     ImageItem item = data.get(position);
     holder.imageView.setImageResource(item.resId);
     holder.tvLabel.setText("Image #" + item.index);
     ```
   - Để các ảnh **trông khác nhau**, áp dụng **màu tint/colorFilter** theo vị trí, ví dụ dùng 1 mảng màu:
     ```java
     private static final int[] COLORS = {
         0x55FF0000, // đỏ nhạt
         0x5500FF00, // xanh lá nhạt
         0x550000FF, // xanh dương nhạt
         0x55FFFF00, // vàng nhạt
         0x55FF00FF, // tím nhạt
         0x5500FFFF  // cyan nhạt
     };
     ```

     Trong onBindViewHolder:
     ```java
     int color = COLORS[position % COLORS.length];
     holder.imageView.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
     ```
   - Kết quả: cùng 1 ảnh gốc, nhưng mỗi item có **màu overlay khác nhau + label “Image #x”** → dễ phân biệt bằng mắt.

5. getItemCount:
   ```java
   return data.size();
   ```

Adapter không làm lazy loading, không cache gì thêm. Lazy loading được xử lý ở Activity GOOD.

------------------------------------------------
E. IMAGE EAGER LOAD (BAD)
------------------------------------------------
1. Tạo Activity: ImageEagerLoadActivity (extends AppCompatActivity).
2. Dùng layout: activity_image_eager_load.xml.
3. Mục tiêu: **Eager loading** – vừa mở màn là RecyclerView có đủ 100 item.
4. Trong onCreate():
   - setContentView(...)
   - Tìm tvInfo, recyclerView.
   - Set LayoutManager:
     ```java
     LinearLayoutManager lm = new LinearLayoutManager(this);
     recyclerView.setLayoutManager(lm);
     ```
   - Tạo fullData:
     ```java
     List<ImageItem> data = new ArrayList<>();
     for (int i = 0; i < TOTAL_IMAGES; i++) {
         data.add(new ImageItem(R.drawable.sample_image, i + 1));
     }
     ```
   - (Tùy chọn) đo thời gian setup:
     ```java
     long start = SystemClock.elapsedRealtime();
     ImageListAdapter adapter = new ImageListAdapter(data);
     recyclerView.setAdapter(adapter);
     long end = SystemClock.elapsedRealtime();
     long duration = end - start;
     ```
   - Cập nhật tvInfo:
     ```java
     tvInfo.setText(
         "IMAGE EAGER LOAD (BAD)
" +
         "Total images: " + TOTAL_IMAGES + "
" +
         "Load strategy: load all items on start
" +
         "Setup time: " + duration + " ms"
     );
     ```
5. Không phân trang, không lazy.

------------------------------------------------
F. IMAGE LAZY LOAD (GOOD)
------------------------------------------------
1. Tạo Activity: ImageLazyLoadActivity (extends AppCompatActivity).
2. Dùng layout: activity_image_lazy_load.xml.
3. Mục tiêu: **Lazy loading** – chia data thành nhiều page, chỉ nạp dần khi cuộn.
4. Định nghĩa hằng:
   ```java
   private static final int TOTAL_IMAGES = 100;
   private static final int PAGE_SIZE = 20;
   ```
5. Trong onCreate():
   - setContentView(...)
   - Tìm tvInfo, recyclerView.
   - Set LayoutManager:
     ```java
     LinearLayoutManager lm = new LinearLayoutManager(this);
     recyclerView.setLayoutManager(lm);
     ```
   - Tạo fullData:
     ```java
     List<ImageItem> fullData = new ArrayList<>();
     for (int i = 0; i < TOTAL_IMAGES; i++) {
         fullData.add(new ImageItem(R.drawable.sample_image, i + 1));
     }
     ```
   - Tạo currentData rỗng:
     ```java
     List<ImageItem> currentData = new ArrayList<>();
     ImageListAdapter adapter = new ImageListAdapter(currentData);
     recyclerView.setAdapter(adapter);
     ```
   - Khai báo biến:
     ```java
     private int currentLoadedCount = 0;
     ```
   - Gọi `loadNextPage()` một lần trong onCreate để nạp page đầu tiên (PAGE_SIZE item).

6. Cài đặt loadNextPage() trong Activity:
   ```java
   private void loadNextPage() {
       if (currentLoadedCount >= TOTAL_IMAGES) return;

       int end = Math.min(currentLoadedCount + PAGE_SIZE, TOTAL_IMAGES);
       int startIndex = currentLoadedCount;

       for (int i = startIndex; i < end; i++) {
           currentData.add(fullData.get(i));
       }

       adapter.notifyItemRangeInserted(startIndex, end - startIndex);
       currentLoadedCount = end;
   }
   ```

7. Thêm OnScrollListener để lazy load:
   ```java
   recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
       @Override
       public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
           super.onScrolled(rv, dx, dy);
           LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
           if (lm == null) return;

           int lastVisible = lm.findLastVisibleItemPosition();
           int total = adapter.getItemCount();

           // Khi còn khoảng 3 item nữa là tới cuối thì nạp thêm
           if (lastVisible >= total - 3) {
               loadNextPage();
           }
       }
   });
   ```

8. Cập nhật tvInfo:
   ```java
   tvInfo.setText(
       "IMAGE LAZY LOAD (GOOD)
" +
       "Total images: " + TOTAL_IMAGES + "
" +
       "Page size: " + PAGE_SIZE + "
" +
       "Load strategy: load first page on start, then lazy load more on scroll"
   );
   ```

------------------------------------------------
G. Đăng ký Activity
------------------------------------------------
Trong AndroidManifest.xml:
```xml
<activity android:name=".ImageEagerLoadActivity" />
<activity android:name=".ImageLazyLoadActivity" />
```
Đảm bảo package/class name đúng theo structure hiện tại.

------------------------------------------------
H. Ràng buộc chung
------------------------------------------------
- Không refactor, không xóa hoặc đổi tên các demo cũ.
- Toàn bộ code mới viết bằng Java, dùng RecyclerView chuẩn (androidx).
- Kỹ thuật demo duy nhất: **lazy loading danh sách ảnh với RecyclerView** (eager vs lazy).
- Mỗi ảnh trong list phải **dễ phân biệt bằng mắt**:
  - Có label “Image #x”.
  - Có màu overlay khác nhau (colorFilter) theo vị trí.
```
