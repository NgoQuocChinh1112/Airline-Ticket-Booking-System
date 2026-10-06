# Airline-Ticket-Booking-System

Hệ thống Backend đặt vé máy bay được xây dựng bằng Java / Spring Boot, tuân thủ kiến trúc phân tầng (Layered Architecture), hỗ trợ cơ chế giữ ghế tạm thời (Seat Holding) và xử lý tranh chấp ghế đồng thời (Concurrency) qua Redis Distributed Lock.

1. Kiến trúc Hệ thống (Layered Architecture)

Hệ thống được thiết kế tuân thủ nghiêm ngặt nguyên tắc phân tách trách nhiệm (Separation of Concerns):
<img width="1822" height="882" alt="Biểu đồ không có tiêu đề" src="https://github.com/user-attachments/assets/bd57fc04-44b6-4985-9e04-0284391c1adb" />



  a. API / Presentation Layer:
     - Tiếp nhận các yêu cầu REST HTTP và trả về định dạng JSON.
     - Sử dụng Data Transfer Objects (DTOs) để trao đổi dữ liệu, ngăn chặn việc lộ các Entity trực tiếp ra bên ngoài.
  b. Security & Cross-Cutting Layer:
     - Sử dụng Spring Security kết hợp với `JwtAuthFilter` can thiệp ở tầng Middleware/Filter để xử lý xác thực tập trung qua JWT Token.
     - GlobalExceptionHandler xử lý ngoại lệ tập trung và chuẩn hóa cấu trúc lỗi trả về cho Client.
  c. Business Logic Layer:
     - Chứa toàn bộ quy tắc nghiệp vụ (tìm kiếm chuyến bay, giữ ghế, tạo đơn đặt vé, xác nhận thanh toán).
     - Tầng nghiệp vụ hoàn toàn không import các thư viện Web Framework hay các driver Database trực tiếp.
     - RedisLockService: Quản lý khóa phân tán (Distributed Lock) ngăn ngừa hiện tượng Race Condition khi nhiều người dùng cùng chọn 1 ghế.
     - SeatReleaseScheduler: Tiến trình ngầm (`@Scheduled`) tự động hoàn trả các ghế hết hạn giữ chỗ về trạng thái `AVAILABLE`.
  d. Data Access / Persistence Layer:
     - Truy xuất dữ liệu thông qua Spring Data JPA (ORM).


2. Đặc tả API (API Specifications)

Hệ thống cung cấp đầy đủ các phương thức HTTP (GET, POST, DELETE):
  a. Authentication API (Public)
    - POST /api/auth/register: Đăng ký tài khoản người dùng mới.
    - POST /api/auth/login: Đăng nhập và nhận chuỗi JWT Token.
  b. Flight & Seat API
  - GET /api/flights/search *(Public)*: Tìm kiếm chuyến bay theo địa điểm và thời gian.
  - GET /api/flights/{id}/seats *(Yêu cầu Token)*: Xem sơ đồ ghế và trạng thái chi tiết của từng ghế.
  - POST /api/bookings/hold-seat *(Yêu cầu Token)*: Giữ ghế tạm thời (áp dụng Redis Distributed Lock).
  c. Booking API (Yêu cầu Token)
  - POST /api/bookings: Tạo đơn đặt vé mới (Trạng thái `PENDING`).
  - POST /api/bookings/{id}/confirm: Xác nhận thanh toán đơn hàng (Chuyển trạng thái sang `CONFIRMED` và ghế sang `BOOKED`).
  - DELETE /api/bookings/{id}: Hủy đơn đặt vé (Giải phóng ghế về trạng thái `AVAILABLE`).

Tài liệu API tương tác trực tiếp (OpenAPI/Swagger UI) khả dụng tại:
http://localhost:3000/swagger-ui.html

3. Bảo mật & Xác thực

  - Cơ chế xác thực: Chuẩn **JWT (JSON Web Token)**.
  - Xử lý tập trung: Token được kiểm tra và giải mã tại `JwtAuthFilter` (Filter chain của Spring Security) trước khi Request tới Controller. Không viết mã xác thực lặp lại trong từng endpoint.
  - Phân quyền truy cập:
    - Endpoint công khai: `/api/auth/**`, `/api/flights/search`, Swagger UI.
    - Endpoint yêu cầu xác thực: Tất cả các API còn lại thuộc `/api/flights/{id}/seats`, `/api/bookings/**`.

4. Hướng dẫn Khởi chạy với Docker

- Bắt đầu dịch vụ Redis & Database Container
Mở terminal tại thư mục gốc của dự án và chạy:

```bash
docker-compose up -d
```

- Biên dịch và Chạy Ứng dụng Backend

```bash
./mvnw clean install
./mvnw spring-boot:run
```

---

5. Kịch bản Kiểm thử tải (Load Testing on Kaggle CPU)

Để kiểm thử khả năng xử lý tranh chấp chọn ghế đồng thời (Race Condition) bằng Python script hoặc Locust trên môi trường Kaggle CPU, sử dụng kịch bản bắn 50 requests đồng thời vào cùng 1 ID ghế:

```python
import concurrent.futures
import requests

URL = "http://<YOUR_BACKEND_IP>:8080/api/bookings/hold-seat"
HEADERS = {
    "Authorization": "Bearer <YOUR_JWT_TOKEN>",
    "Content-Type": "application/json"
}
PAYLOAD = {
    "flightId": "<FLIGHT_UUID>",
    "seatId": "<SEAT_UUID>"
}

def send_hold_request(user_id):
    response = requests.post(URL, json=PAYLOAD, headers=HEADERS)
    return response.status_code, response.json()

# Giả lập 50 luồng truy cập đồng thời
with concurrent.futures.ThreadPoolExecutor(max_workers=50) as executor:
    results = list(executor.map(send_hold_request, range(50)))

# Kết quả: Chỉ 1 request giữ ghế thành công (HTTP 200/201), 49 request còn lại bị từ chối (HTTP 409 Conflict)
for status, body in results:
    print(f"Status: {status} | Response: {body}")
```
