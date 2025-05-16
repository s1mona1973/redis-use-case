import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RedisSessionManager - Quản lý phiên người dùng với Redis
 * Tập trung vào cơ chế tự động hết hạn (expiration)
 */
public class RedisSessionManager implements AutoCloseable {
    private final JedisPool jedisPool;
    private static final String SESSION_PREFIX = "session:";

    // Khởi tạo với cấu hình mặc định
    public RedisSessionManager() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);

        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379);
    }

    // Tạo phiên mới
    public String createSession(String userId, Map<String, String> userData, int expirationSeconds) {
        String sessionId = UUID.randomUUID().toString();

        try (Jedis jedis = jedisPool.getResource()) {
            // Tạo dữ liệu phiên
            if (userData == null) {
                userData = new HashMap<>();
            }
            userData.put("userId", userId);

            // Lưu vào Redis
            String sessionKey = SESSION_PREFIX + sessionId;
            jedis.hmset(sessionKey, userData);

            // Thiết lập thời gian hết hạn
            jedis.expire(sessionKey, expirationSeconds);

            return sessionId;
        } catch (JedisException e) {
            System.err.println("Lỗi khi tạo phiên: " + e.getMessage());
            return null;
        }
    }

    // Lấy thông tin phiên
    public Map<String, String> getSession(String sessionId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String sessionKey = SESSION_PREFIX + sessionId;

            // Nếu phiên không tồn tại hoặc đã hết hạn, Redis sẽ tự động trả về null
            if (!jedis.exists(sessionKey)) {
                return null;
            }

            return jedis.hgetAll(sessionKey);
        } catch (JedisException e) {
            System.err.println("Lỗi khi lấy phiên: " + e.getMessage());
            return null;
        }
    }

    // Đóng kết nối Redis
    @Override
    public void close() {
        jedisPool.close();
    }

    /**
     * Demo minh họa cơ chế tự động hết hạn phiên
     */
    public void demo() {
        System.out.println("=== DEMO REDIS SESSION EXPIRATION ===");

        // Tạo dữ liệu mẫu
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "user123");
        userData.put("role", "admin");

        // Thời gian hết hạn ngắn - 5 giây
        int expirationSeconds = 5;

        // Tạo phiên
        String sessionId = createSession("user123", userData, expirationSeconds);
        if (sessionId == null) {
            System.out.println("Không thể tạo phiên. Vui lòng kiểm tra kết nối Redis!");
            return;
        }

        System.out.println("Đã tạo phiên: " + sessionId);
        System.out.println("Thời gian hết hạn: " + expirationSeconds + " giây");

        // Lấy phiên ngay sau khi tạo
        Map<String, String> session = getSession(sessionId);
        System.out.println("\n[Ngay lập tức] Phiên có tồn tại: " + (session != null));
        if (session != null) {
            System.out.println("Dữ liệu: " + session);
        }

        // Kiểm tra sau 2 giây
        try {
            System.out.println("\nĐợi 2 giây...");
            Thread.sleep(2000);

            session = getSession(sessionId);
            System.out.println("[Sau 2 giây] Phiên có tồn tại: " + (session != null));
            if (session != null) {
                System.out.println("Dữ liệu: " + session);
            }

            // Kiểm tra sau khi hết hạn (thêm 3 giây nữa = tổng 5 giây)
            System.out.println("\nĐợi thêm 3 giây (tổng 5 giây)...");
            Thread.sleep(3000);

            session = getSession(sessionId);
            System.out.println("[Sau 5 giây] Phiên có tồn tại: " + (session != null));

            // Đợi thêm 1 giây để chắc chắn hết hạn
            System.out.println("\nĐợi thêm 1 giây để chắc chắn...");
            Thread.sleep(1000);

            session = getSession(sessionId);
            System.out.println("[Sau 6 giây] Phiên có tồn tại: " + (session != null));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Quá trình chờ bị gián đoạn");
        }
    }

    public static void main(String[] args) {
        try (RedisSessionManager manager = new RedisSessionManager()) {
            manager.demo();
        }
    }
}