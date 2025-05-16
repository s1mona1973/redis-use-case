import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.SetParams;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RedisDistributedLock - Triển khai khóa phân tán với Redis
 */
public class RedisDistributedLock implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(RedisDistributedLock.class.getName());
    private static final String LOCK_PREFIX = "lock:";

    private final JedisPool jedisPool;

    // Khởi tạo với cấu hình mặc định
    public RedisDistributedLock() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setMaxWait(Duration.ofSeconds(30));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);

        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379);
    }

    // Lấy khóa
    public boolean acquireLock(String lockKey, String requestId, int expirationSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = LOCK_PREFIX + lockKey;
            SetParams params = new SetParams().nx().ex(expirationSeconds);
            String result = jedis.set(key, requestId, params);
            return "OK".equals(result);
        } catch (JedisException e) {
            LOGGER.log(Level.WARNING, "Lỗi khi lấy khóa: {0}", e.getMessage());
            return false;
        }
    }

    // Lấy khóa với retry
    public boolean acquireLockWithRetry(String lockKey, String requestId, int expirationSeconds)
            throws InterruptedException {
        int retryCount = 0;
        int maxRetries = 20;
        int retryDelayMs = 100;

        while (retryCount < maxRetries) {
            if (acquireLock(lockKey, requestId, expirationSeconds)) {
                return true;
            }

            retryCount++;

            if (retryCount >= maxRetries) {
                break;
            }

            int jitter = ThreadLocalRandom.current().nextInt(retryDelayMs);
            Thread.sleep(retryDelayMs + jitter);
        }

        return false;
    }

    // Giải phóng khóa
    public boolean releaseLock(String lockKey, String requestId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = LOCK_PREFIX + lockKey;
            String script =
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end";

            Object result = jedis.eval(script, 1, key, requestId);
            return result != null && ((Long) result) == 1L;
        } catch (JedisException e) {
            LOGGER.log(Level.WARNING, "Lỗi khi giải phóng khóa: {0}", e.getMessage());
            return false;
        }
    }

    // Đóng kết nối Redis
    @Override
    public void close() {
        jedisPool.close();
    }

    /**
     * Demo minh họa cách sử dụng Redis distributed lock
     */
    public void demo() throws InterruptedException {
        System.out.println("=== DEMO REDIS DISTRIBUTED LOCK ===");

        // Demo với hai thread cùng tăng một biến
        final AtomicInteger counter = new AtomicInteger(0);
        final int iterations = 1000;
        final int numThreads = 2;

        System.out.println("Mỗi thread sẽ tăng biến đếm " + iterations + " lần");
        System.out.println("Giá trị mong đợi sau khi kết thúc: " + (iterations * numThreads));

        // Chạy demo không có khóa
        System.out.println("\n--- Không sử dụng khóa ---");
        demoWithoutLock(counter, iterations, numThreads);

        // Đặt lại biến đếm về 0
        counter.set(0);

        // Chạy demo có khóa
        System.out.println("\n--- Sử dụng khóa Redis ---");
        demoWithLock(counter, iterations, numThreads);
    }

    /**
     * Demo không sử dụng khóa - mô phỏng race condition
     */
    private void demoWithoutLock(AtomicInteger counter, int iterations, int numThreads) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        // Race condition: Đọc giá trị -> xử lý -> ghi giá trị
                        int current = counter.get();
                        Thread.yield();
                        counter.set(current + 1);
                    }
                } finally {
                    latch.countDown();
                }
            });

            thread.start();
        }

        latch.await(2, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis();

        System.out.println("Kết quả: " + counter.get() + " (mong đợi: " + (iterations * numThreads) + ")");
        System.out.println("Thời gian thực thi: " + (endTime - startTime) + "ms");
    }

    /**
     * Demo sử dụng khóa Redis
     */
    private void demoWithLock(AtomicInteger counter, int iterations, int numThreads) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final String threadId = "thread-" + i;

            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        String requestId = threadId + "-" + UUID.randomUUID().toString();
                        boolean locked = false;

                        try {
                            // Lấy khóa trước khi truy cập biến đếm
                            locked = acquireLockWithRetry("counter", requestId, 5);

                            if (locked) {
                                // Critical section - được bảo vệ bởi khóa
                                int current = counter.get();
//                                Thread.sleep(1); // Mô phỏng xử lý
                                counter.set(current + 1);
                            }
                        } finally {
                            // Đảm bảo luôn giải phóng khóa
                            if (locked) {
                                releaseLock("counter", requestId);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });

            thread.start();
        }

        latch.await(5, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis();

        System.out.println("Kết quả: " + counter.get() + " (mong đợi: " + (iterations * numThreads) + ")");
        System.out.println("Thời gian thực thi: " + (endTime - startTime) + "ms");
    }

    /**
     * Phương thức main để chạy demo
     */
    public static void main(String[] args) {
        try (RedisDistributedLock lockManager = new RedisDistributedLock()) {
            lockManager.demo();
        } catch (Exception e) {
            System.err.println("Lỗi: " + e.getMessage());
        }
    }
}