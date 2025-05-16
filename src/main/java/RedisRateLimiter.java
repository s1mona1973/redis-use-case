import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

/**
 * RedisRateLimiter - Triển khai cơ chế giới hạn tốc độ truy cập với Redis
 * 
 * Class này cung cấp phương thức để giới hạn số lượng request từ một client
 * trong một khoảng thời gian (cửa sổ thời gian) nhất định, sử dụng thuật toán
 * "Fixed Window Counter" đơn giản dựa trên Redis.
 */
public class RedisRateLimiter {
    private final JedisPool jedisPool;
    private static final String RATE_LIMITER_PREFIX = "ratelimit:";
    
    /**
     * Khởi tạo RedisRateLimiter với các cấu hình mặc định
     */
    public RedisRateLimiter() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        
        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379);
    }
    
    /**
     * Khởi tạo RedisRateLimiter với cấu hình tùy chỉnh
     * 
     * @param host Redis server host
     * @param port Redis server port
     */
    public RedisRateLimiter(String host, int port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        
        this.jedisPool = new JedisPool(poolConfig, host, port);
    }
    
    /**
     * Kiểm tra và đếm request từ client sử dụng thuật toán "Fixed Window Counter"
     * 
     * @param clientId ID của client đang gửi request
     * @param maxRequests Số lượng request tối đa được phép trong cửa sổ thời gian
     * @param windowSeconds Độ rộng của cửa sổ thời gian (giây)
     * @return true nếu request được chấp nhận, false nếu vượt quá giới hạn
     */
    public boolean allowRequest(String clientId, int maxRequests, int windowSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = RATE_LIMITER_PREFIX + clientId;
            
            // Kiểm tra xem key đã tồn tại chưa, nếu chưa tạo mới
            if (!jedis.exists(key)) {
                jedis.setex(key, windowSeconds, "1");
                return true;
            }
            
            // Lấy số lượng request hiện tại
            long currentRequests = Long.parseLong(jedis.get(key));
            
            // Kiểm tra giới hạn
            if (currentRequests < maxRequests) {
                // Tăng bộ đếm lên 1
                jedis.incr(key);
                return true;
            } else {
                return false;
            }
        } catch (JedisException e) {
            System.err.println("Lỗi khi kiểm tra giới hạn tốc độ: " + e.getMessage());
            // Trong trường hợp lỗi, cho phép request để tránh chặn toàn bộ hệ thống
            return true;
        }
    }
    
    /**
     * Lấy số lượng request còn lại được phép từ một client
     * 
     * @param clientId ID của client
     * @param maxRequests Số lượng request tối đa được phép trong cửa sổ thời gian
     * @return Số lượng request còn lại được phép
     */
    public int getRemainingRequests(String clientId, int maxRequests) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = RATE_LIMITER_PREFIX + clientId;
            
            // Nếu key không tồn tại, nghĩa là client chưa gửi request nào
            if (!jedis.exists(key)) {
                return maxRequests;
            }
            
            // Lấy số lượng request hiện tại và tính số còn lại
            long currentRequests = Long.parseLong(jedis.get(key));
            int remaining = (int) (maxRequests - currentRequests);
            
            // Đảm bảo giá trị không âm
            return Math.max(0, remaining);
        } catch (JedisException e) {
            System.err.println("Lỗi khi lấy số request còn lại: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Lấy thời gian còn lại (giây) cho cửa sổ thời gian hiện tại
     * 
     * @param clientId ID của client
     * @return Thời gian còn lại (giây), hoặc 0 nếu không có giới hạn nào đang được áp dụng
     */
    public long getRemainingWindowTime(String clientId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = RATE_LIMITER_PREFIX + clientId;
            
            // Nếu key không tồn tại, không có giới hạn nào đang được áp dụng
            if (!jedis.exists(key)) {
                return 0;
            }
            
            return jedis.ttl(key);
        } catch (JedisException e) {
            System.err.println("Lỗi khi lấy thời gian còn lại: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Đặt lại bộ đếm cho client, cho phép client gửi request lại từ đầu
     * 
     * @param clientId ID của client
     * @return true nếu reset thành công, false nếu thất bại
     */
    public boolean resetLimit(String clientId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = RATE_LIMITER_PREFIX + clientId;
            long result = jedis.del(key);
            return result > 0;
        } catch (JedisException e) {
            System.err.println("Lỗi khi reset giới hạn: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Đóng kết nối đến Redis pool
     */
    public void close() {
        jedisPool.close();
    }
    
    /**
     * Demo minh họa cách sử dụng RedisRateLimiter
     */
    public void demo() {
        System.out.println("=== DEMO REDIS RATE LIMITER ===");
        
        // Thiết lập tham số
        String clientId = "client1";
        int maxRequests = 10;  // Tối đa 10 request
        int windowSeconds = 10;  // Trong khoảng thời gian 10 giây
        
        System.out.println("Thiết lập giới hạn: " + maxRequests + " request trong " 
                + windowSeconds + " giây cho client " + clientId);
        
        // Mô phỏng một trường hợp thực tế: request API
        System.out.println("\nMô phỏng một API có giới hạn tốc độ:");
        
        for (int i = 1; i <= 23; i++) {
            System.out.println("\n--- Lần gọi API #" + i + " ---");
            boolean isAllowed = callRateLimitedAPI(clientId, maxRequests, windowSeconds);
            
            if (!isAllowed) {
                System.out.println("Đợi 1 giây trước khi thử lại...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Thử lại
                System.out.println("\nThử lại sau khi đợi:");
                callRateLimitedAPI(clientId, maxRequests, windowSeconds);
            }
        }
    }
    
    /**
     * Mô phỏng việc gọi một API có giới hạn tốc độ
     * 
     * @param clientId ID của client
     * @param maxRequests Số lượng request tối đa
     * @param windowSeconds Độ rộng cửa sổ thời gian
     * @return true nếu gọi API thành công, false nếu vượt quá giới hạn
     */
    private boolean callRateLimitedAPI(String clientId, int maxRequests, int windowSeconds) {
        // Kiểm tra giới hạn tốc độ
        boolean isAllowed = allowRequest(clientId, maxRequests, windowSeconds);
        int remaining = getRemainingRequests(clientId, maxRequests);
        
        if (isAllowed) {
            System.out.println("API call: Thành công");
            System.out.println("Số request còn lại: " + remaining);
            System.out.println("Dữ liệu API: { \"status\": \"success\", \"data\": { ... } }");
            return true;
        } else {
            System.out.println("API call: Thất bại - Vượt quá giới hạn tốc độ");
            System.out.println("Vui lòng đợi " + getRemainingWindowTime(clientId) + " giây");
            System.out.println("Phản hồi: { \"error\": \"rate_limit_exceeded\", \"message\": \"Too many requests\" }");
            return false;
        }
    }
    
    /**
     * Phương thức main để chạy demo
     */
    public static void main(String[] args) {
        RedisRateLimiter rateLimiter = new RedisRateLimiter();
        
        try {
            rateLimiter.demo();
        } finally {
            rateLimiter.close();
        }
    }
} 