# Redis Use Cases Demo - Java

Dự án này triển khai 4 use case phổ biến của Redis bằng Java, giúp bạn hiểu rõ về cách sử dụng Redis trong các kịch bản thực tế. Mỗi ví dụ có thể chạy độc lập và đều có đầy đủ chú thích bằng tiếng Việt.

## Các use case được triển khai

1. **Redis Cache** - Cơ chế cache đơn giản cho ứng dụng
2. **Redis Session Manager** - Quản lý phiên người dùng với cơ chế tự động hết hạn
3. **Redis Rate Limiter** - Giới hạn tốc độ truy cập API
4. **Redis Distributed Lock** - Khóa phân tán cho các hệ thống phân tán

## Yêu cầu hệ thống

- Java JDK 11 trở lên
- Maven
- Redis Server (local hoặc remote)

## Cài đặt và chạy

### Cài đặt Redis

#### Trên macOS (sử dụng Homebrew):
```bash
brew install redis
brew services start redis
```

#### Trên Ubuntu/Debian:
```bash
sudo apt update
sudo apt install redis-server
sudo systemctl start redis-server
```

### Cài đặt dự án

1. Clone dự án:
```bash
git clone https://github.com/yourusername/redis-use-case.git
cd redis-use-case
```

2. Biên dịch dự án bằng Maven:
```bash
mvn clean compile
```

### Chạy các ví dụ

Mỗi file Java đều có phương thức `main()` để chạy demo độc lập:

#### Chạy Redis Cache demo:
```bash
mvn exec:java -Dexec.mainClass="RedisCache"
```

#### Chạy Redis Session Manager demo:
```bash
mvn exec:java -Dexec.mainClass="RedisSessionManager"
```

#### Chạy Redis Rate Limiter demo:
```bash
mvn exec:java -Dexec.mainClass="RedisRateLimiter"
```

#### Chạy Redis Distributed Lock demo:
```bash
mvn exec:java -Dexec.mainClass="RedisDistributedLock"
```

## Chi tiết triển khai

### 1. RedisCache.java

Triển khai cơ chế cache đơn giản với Redis. Các tính năng:
- Lưu trữ dữ liệu với thời gian hết hạn
- Lấy dữ liệu từ cache
- Xóa dữ liệu khỏi cache
- Demo so sánh thời gian truy xuất có và không có cache

### 2. RedisSessionManager.java

Quản lý phiên người dùng với Redis. Các tính năng:
- Tạo phiên mới với thời gian hết hạn
- Lấy, cập nhật và xóa thông tin phiên
- Gia hạn thời gian phiên
- Kiểm tra tính hợp lệ của phiên
- Demo cơ chế tự động hết hạn

### 3. RedisRateLimiter.java

Giới hạn tốc độ truy cập với Redis. Các tính năng:
- Triển khai thuật toán "Fixed Window Counter"
- Giới hạn số lượng request trong một khoảng thời gian
- Lấy thông tin số request còn lại và thời gian còn lại
- Demo kịch bản giới hạn tốc độ truy cập API

### 4. RedisDistributedLock.java

Khóa phân tán với Redis. Các tính năng:
- Lấy và giải phóng khóa an toàn
- Đảm bảo chỉ owner mới có thể giải phóng khóa (sử dụng Lua script)
- Tự động giải phóng khóa sau thời gian chờ
- Demo làm việc với dữ liệu dùng chung giữa các thread

## Lưu ý quan trọng

- Đảm bảo Redis server đang chạy trước khi thực thi các ví dụ
- Các ví dụ mặc định kết nối đến Redis server tại `localhost:6379`
- Bạn có thể thay đổi cấu hình kết nối trong constructor của mỗi class

## Tài liệu tham khảo

- [Redis Documentation](https://redis.io/documentation)
- [Jedis Wiki](https://github.com/redis/jedis/wiki)

## Đóng góp

Nếu có bất kỳ câu hỏi hoặc đề xuất cải tiến, vui lòng tạo issue hoặc pull request. 