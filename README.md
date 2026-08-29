# FlashSale-Service
## Authentication API

Base URL: `/api/v1/auth`

`identifier` là một field dùng chung: hệ thống tự nhận diện email hoặc số điện thoại quốc tế (ví dụ `+84901234567`). Password được hash bằng BCrypt.

### Register

```http
POST /api/v1/auth/register
Content-Type: application/json

{"identifier":"user@example.com","password":"secret123","displayName":"User"}
```

OTP 6 số được tạo, hash trong database và mock gửi bằng log ứng dụng:
`MOCK OTP [EMAIL] user@example.com => 123456` (hiệu lực 5 phút).

### Verify OTP

```http
POST /api/v1/auth/verify-otp
Content-Type: application/json

{"identifier":"user@example.com","otp":"123456"}
```

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{"identifier":"user@example.com","password":"secret123"}
```

Response trả về opaque Bearer token, được lưu Redis với TTL 24 giờ.

### Logout

```http
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
```

Logout xóa key token trong Redis nên token bị vô hiệu ngay lập tức. Các API bảo vệ khác chỉ cần gửi header `Authorization: Bearer <accessToken>`; `TokenAuthenticationFilter` sẽ xác thực token từ Redis.

## Chạy local

MySQL và Redis được cung cấp bằng Docker Compose, lần lượt public ở `localhost:3306` và `localhost:6379`:

```bash
docker compose up -d
```

Thông tin mặc định:

```text
MySQL database: flashsale
MySQL username: flashsale
MySQL password: flashsale_password
Redis: localhost:6379
```

Chạy project từ thư mục `FlashSale` bằng `./mvnw spring-boot:run`. Có thể override kết nối bằng các biến `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`.

## Flash Sale API

```http
GET /api/v1/flash-sales/current
```

Trả về các sản phẩm thuộc khung giờ đang chạy. Cấu hình được lưu trong các bảng `products`, `flash_sale_slots`, `flash_sale_items`, `inventories`; `remaining_quantity` được khởi tạo theo quota của item.

```http
POST /api/v1/flash-sales/purchase
Authorization: Bearer <accessToken>
Content-Type: application/json

{"itemId":"<flash-sale-item-id>"}
```

Purchase được bảo vệ bằng transaction và các câu lệnh atomic update. Database unique key trên `(customer_id, purchase_date)` bảo đảm mỗi customer chỉ mua một lần mỗi ngày; quota flash sale và inventory không thể giảm dưới 0. Balance được xem là dữ liệu có sẵn của `Customer` và phải được nạp trước khi mua.

## Notification template

OTP được gửi qua `NotificationService`. Service đọc template theo mã từ bảng `notification_templates`, kiểm tra loại `SMS`/`EMAIL`, rồi thay placeholder dạng `{{otp}}` bằng parameter truyền vào. Template được quản lý bằng Liquibase trong `FlashSale/src/main/resources/db/changelog`; có thể thêm hoặc cập nhật template bằng changeset mà không thay đổi luồng authentication.
