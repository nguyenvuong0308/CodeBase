# Quy trình làm việc của Agent

File này áp dụng cho toàn bộ repository. Mọi thay đổi code phải đi đúng thứ tự dưới đây; không được đánh dấu hoàn thành khi chưa chạy kiểm thử.

## Workflow bắt buộc

### 1. Lập plan và task

- Đọc yêu cầu, kiểm tra code liên quan và xác định phạm vi ảnh hưởng trước khi sửa.
- Tạo plan gồm các task nhỏ, có kết quả kiểm chứng được. Plan tối thiểu phải có các bước riêng: `Code`, `Viết test`, `Auto test`.
- Gán trạng thái ban đầu cho từng task: task đầu tiên là `in_progress`, các task còn lại là `pending`.
- Chỉ được có tối đa một task `in_progress` tại một thời điểm.
- Nếu phát hiện thêm việc trong lúc triển khai, cập nhật plan trước khi tiếp tục.

### 2. Code

- Chuyển task code cần thực hiện sang `in_progress`.
- Chỉ sửa những phần cần thiết cho yêu cầu; giữ nguyên các thay đổi không liên quan của người dùng.
- Tự kiểm tra diff và các nhánh lỗi quan trọng sau khi code.
- Khi phần triển khai đã xong, chuyển task `Code` sang `completed`, sau đó chuyển task `Viết test` sang `in_progress`.
- Hoàn thành code chưa đồng nghĩa với hoàn thành toàn bộ yêu cầu.

### 3. Viết test

- Thêm hoặc cập nhật test cho hành vi mới, bug đã sửa và các trường hợp biên phù hợp.
- Ưu tiên unit test trong `src/test`; chỉ dùng instrumentation test trong `src/androidTest` khi cần Android runtime hoặc thiết bị/emulator.
- Test sửa bug phải có khả năng thất bại khi chưa có bản sửa và thành công sau khi sửa.
- Nếu không thể viết test tự động, phải ghi rõ lý do và cách kiểm chứng thay thế trong phần bàn giao; không được âm thầm bỏ qua.
- Khi test đã được viết xong, chuyển task `Viết test` sang `completed`, sau đó chuyển task `Auto test` sang `in_progress`.

### 4. Auto test

- Agent phải chủ động chạy test, không chờ người dùng yêu cầu lại.
- Chạy test hẹp nhất liên quan đến module/thay đổi trước, sau đó chạy bộ test rộng hơn khi phạm vi hoặc rủi ro yêu cầu.
- Trên Windows/PowerShell, dùng Gradle wrapper của repository, ví dụ:
  - Module không có product flavor: `.\gradlew.bat :core:utilities:test`
  - Module có flavor: `.\gradlew.bat :core:ads:testProdDebugUnitTest`
  - Toàn bộ unit test: `.\gradlew.bat test`
  - Instrumentation test khi có emulator/device phù hợp: `.\gradlew.bat connectedAndroidTest`
- Nếu test thất bại do thay đổi vừa thực hiện, sửa code/test và chạy lại cho đến khi pass.
- Nếu bị chặn bởi môi trường hoặc lỗi có sẵn không liên quan, giữ task `Auto test` ở `in_progress`, ghi lại command, lỗi cụ thể và bằng chứng phân biệt lỗi đó với thay đổi hiện tại.
- Chỉ chuyển task `Auto test` sang `completed` khi các test bắt buộc đã pass.

### 5. Cập nhật trạng thái và bàn giao

- Trạng thái hợp lệ: `pending` -> `in_progress` -> `completed`.
- Cập nhật trạng thái ngay sau mỗi bước, không dồn cập nhật vào cuối.
- Chỉ đánh dấu toàn bộ task/yêu cầu là `completed` khi:
  - Code đã hoàn tất.
  - Test cần thiết đã được thêm hoặc đã nêu rõ lý do hợp lệ khi không thể thêm.
  - Auto test bắt buộc đã pass.
  - Không còn task con `pending` hoặc `in_progress`.
- Phần bàn giao cuối phải nêu ngắn gọn: nội dung đã đổi, test đã thêm, command test đã chạy và kết quả. Nếu còn blocker, task chưa được báo là hoàn thành.

## Luồng trạng thái chuẩn

1. `Lập plan/task: in_progress`
2. `Lập plan/task: completed` -> `Code: in_progress`
3. `Code: completed` -> `Viết test: in_progress`
4. `Viết test: completed` -> `Auto test: in_progress`
5. `Auto test: completed` -> `Task tổng: completed`
