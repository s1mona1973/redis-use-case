import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RedisDemo - Giới thiệu các chức năng cơ bản của Redis
 */
public class RedisDemo implements AutoCloseable {
    private final JedisPool jedisPool;

    public RedisDemo() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);

        this.jedisPool = new JedisPool(poolConfig, "localhost", 6379);
    }

    @Override
    public void close() {
        jedisPool.close();
    }

    /**
     * STRINGS - Cơ bản nhất trong Redis
     * Dùng để lưu trữ giá trị đơn giản như chuỗi, số, JSON
     */
    public void demoStrings() {
        System.out.println("\n================================================================");
        System.out.println("||                    1. REDIS STRINGS                         ||");
        System.out.println("================================================================");
        System.out.println("Strings là kiểu dữ liệu cơ bản nhất trong Redis");
        System.out.println("Dùng để lưu text, số, JSON, hoặc dữ liệu nhị phân (tối đa 512MB)");

        try (Jedis jedis = jedisPool.getResource()) {
            System.out.println("\n1.1. Lưu và lấy giá trị cơ bản");
            System.out.println("Command: SET greeting \"Xin chào Redis!\"");
            jedis.set("greeting", "Xin chào Redis!");

            System.out.println("Command: GET greeting");
            String value = jedis.get("greeting");
            System.out.println("Kết quả: " + value);

            System.out.println("\n1.2. Lưu với thời gian hết hạn (seconds)");
            System.out.println("Command: SETEX session:123 60 \"user_data\"");
            jedis.setex("session:123", 60, "user_data");

            System.out.println("Command: TTL session:123");
            long ttl = jedis.ttl("session:123");
            System.out.println("Kết quả: " + ttl + " giây");

            System.out.println("\n1.3. Tăng/giảm giá trị số");
            System.out.println("Command: SET counter 10");
            jedis.set("counter", "10");

            System.out.println("Command: INCR counter");
            long newCount = jedis.incr("counter");
            System.out.println("Kết quả: " + newCount);

            System.out.println("Command: INCRBY counter 5");
            newCount = jedis.incrBy("counter", 5);
            System.out.println("Kết quả: " + newCount);

            System.out.println("Command: DECR counter");
            newCount = jedis.decr("counter");
            System.out.println("Kết quả: " + newCount);

            System.out.println("\n1.4. Xóa key");
            System.out.println("Command: DEL greeting");
            jedis.del("greeting");

            System.out.println("Command: GET greeting");
            System.out.println("Kết quả: " + jedis.get("greeting") + " (null)");
        }
    }

    /**
     * LISTS - Danh sách có thứ tự
     * Dùng cho hàng đợi, stack, lưu danh sách theo thứ tự
     */
    public void demoLists() {
        System.out.println("\n================================================================");
        System.out.println("||                    2. REDIS LISTS                          ||");
        System.out.println("================================================================");
        System.out.println("Lists là danh sách các chuỗi được sắp xếp theo thứ tự chèn");
        System.out.println("Thường dùng cho: queues, stacks, và các danh sách có thứ tự");

        try (Jedis jedis = jedisPool.getResource()) {
            String listKey = "tasks";

            // Xóa list cũ nếu tồn tại
            System.out.println("\n2.1. Xóa danh sách cũ nếu tồn tại");
            System.out.println("Command: DEL tasks");
            jedis.del(listKey);

            System.out.println("\n2.2. Thêm phần tử vào đầu và cuối");
            System.out.println("Command: LPUSH tasks \"Task 1\"");
            jedis.lpush(listKey, "Task 1");
            System.out.println("Command: LPUSH tasks \"Task 2\"");
            jedis.lpush(listKey, "Task 2");
            System.out.println("Command: RPUSH tasks \"Task 3\"");
            jedis.rpush(listKey, "Task 3");

            System.out.println("\n2.3. Lấy toàn bộ danh sách");
            System.out.println("Command: LRANGE tasks 0 -1");
            List<String> allTasks = jedis.lrange(listKey, 0, -1);
            System.out.println("Kết quả: " + allTasks);

            System.out.println("\n2.4. Lấy theo chỉ số (index)");
            System.out.println("Command: LINDEX tasks 1");
            String task = jedis.lindex(listKey, 1);
            System.out.println("Kết quả: " + task);

            System.out.println("\n2.5. Lấy và xóa phần tử đầu (dequeue)");
            System.out.println("Command: LPOP tasks");
            String firstTask = jedis.lpop(listKey);
            System.out.println("Kết quả (phần tử được lấy ra): " + firstTask);

            System.out.println("Command: LRANGE tasks 0 -1");
            System.out.println("Kết quả (danh sách còn lại): " + jedis.lrange(listKey, 0, -1));

            System.out.println("\n2.6. Lấy độ dài danh sách");
            System.out.println("Command: LLEN tasks");
            long length = jedis.llen(listKey);
            System.out.println("Kết quả: " + length);
        }
    }

    /**
     * SETS - Tập hợp không có thứ tự, không trùng lặp
     * Dùng để lưu các tập hợp, thực hiện các phép toán tập hợp
     */
    public void demoSets() {
        System.out.println("\n================================================================");
        System.out.println("||                    3. REDIS SETS                           ||");
        System.out.println("================================================================");
        System.out.println("Sets là tập hợp các chuỗi không có thứ tự, không trùng lặp");
        System.out.println("Thường dùng cho: lưu trữ các mối quan hệ, thẻ (tags), phép toán tập hợp");

        try (Jedis jedis = jedisPool.getResource()) {
            System.out.println("\n3.1. Xóa các set cũ nếu tồn tại");
            System.out.println("Command: DEL team1 team2");
            jedis.del("team1", "team2");

            System.out.println("\n3.2. Thêm các thành viên vào set");
            System.out.println("Command: SADD team1 Hoa Minh Tuan Linh");
            jedis.sadd("team1", "Hoa", "Minh", "Tuan", "Linh");

            System.out.println("Command: SADD team2 Hoa Nam Linh Hai");
            jedis.sadd("team2", "Hoa", "Nam", "Linh", "Hai");

            System.out.println("\n3.3. Lấy tất cả các thành viên");
            System.out.println("Command: SMEMBERS team1");
            Set<String> team1Members = jedis.smembers("team1");
            System.out.println("Kết quả: " + team1Members);

            System.out.println("\n3.4. Kiểm tra thành viên có tồn tại");
            System.out.println("Command: SISMEMBER team1 Minh");
            boolean isMember = jedis.sismember("team1", "Minh");
            System.out.println("Kết quả: " + isMember);

            System.out.println("\n3.5. Phép giao (intersection)");
            System.out.println("Command: SINTER team1 team2");
            Set<String> bothTeams = jedis.sinter("team1", "team2");
            System.out.println("Kết quả (thành viên thuộc cả hai team): " + bothTeams);

            System.out.println("\n3.6. Phép hiệu (difference)");
            System.out.println("Command: SDIFF team1 team2");
            Set<String> onlyTeam1 = jedis.sdiff("team1", "team2");
            System.out.println("Kết quả (thành viên chỉ thuộc team1): " + onlyTeam1);

            System.out.println("\n3.7. Lấy số lượng thành viên");
            System.out.println("Command: SCARD team1");
            long count = jedis.scard("team1");
            System.out.println("Kết quả: " + count);
        }
    }

    /**
     * HASHES - Lưu trữ cặp key-value trong một key
     * Giống như một đối tượng/map nhỏ, phù hợp để lưu các đối tượng
     */
    public void demoHashes() {
        System.out.println("\n================================================================");
        System.out.println("||                    4. REDIS HASHES                         ||");
        System.out.println("================================================================");
        System.out.println("Hashes là cấu trúc lưu trữ field-value, tương tự như Map/Object");
        System.out.println("Thường dùng cho: lưu trữ đối tượng, cấu trúc dữ liệu phức tạp");

        try (Jedis jedis = jedisPool.getResource()) {
            String userKey = "user:1001";

            System.out.println("\n4.1. Xóa hash cũ nếu tồn tại");
            System.out.println("Command: DEL " + userKey);
            jedis.del(userKey);

            System.out.println("\n4.2. Lưu từng trường của user");
            System.out.println("Command: HSET " + userKey + " name \"Nguyen Van A\"");
            jedis.hset(userKey, "name", "Nguyen Van A");

            System.out.println("Command: HSET " + userKey + " email \"nguyenvana@example.com\"");
            jedis.hset(userKey, "email", "nguyenvana@example.com");

            System.out.println("Command: HSET " + userKey + " age 30");
            jedis.hset(userKey, "age", "30");

            System.out.println("\n4.3. Lưu nhiều trường cùng lúc");
            System.out.println("Command: HSET " + userKey + " phone 0901234567 address \"Ho Chi Minh City\"");
            Map<String, String> fields = new HashMap<>();
            fields.put("phone", "0901234567");
            fields.put("address", "Ho Chi Minh City");
            jedis.hset(userKey, fields);

            System.out.println("\n4.4. Lấy một trường");
            System.out.println("Command: HGET " + userKey + " name");
            String name = jedis.hget(userKey, "name");
            System.out.println("Kết quả: " + name);

            System.out.println("\n4.5. Lấy tất cả các trường");
            System.out.println("Command: HGETALL " + userKey);
            Map<String, String> userData = jedis.hgetAll(userKey);
            System.out.println("Kết quả: " + userData);

            System.out.println("\n4.6. Kiểm tra trường tồn tại");
            System.out.println("Command: HEXISTS " + userKey + " phone");
            boolean hasPhone = jedis.hexists(userKey, "phone");
            System.out.println("Kết quả: " + hasPhone);

            System.out.println("\n4.7. Xóa một trường");
            System.out.println("Command: HDEL " + userKey + " age");
            jedis.hdel(userKey, "age");

            System.out.println("Command: HGETALL " + userKey);
            System.out.println("Kết quả (sau khi xóa age): " + jedis.hgetAll(userKey));

            System.out.println("\n4.8. Tăng giá trị số");
            System.out.println("Command: HSET " + userKey + " visits 10");
            jedis.hset(userKey, "visits", "10");

            System.out.println("Command: HINCRBY " + userKey + " visits 5");
            long newVisits = jedis.hincrBy(userKey, "visits", 5);
            System.out.println("Kết quả (số lần truy cập mới): " + newVisits);
        }
    }

    /**
     * SORTED SETS - Tập hợp có thứ tự theo điểm số
     * Dùng cho bảng xếp hạng, dữ liệu ưu tiên theo điểm số
     */
    public void demoSortedSets() {
        System.out.println("\n=== DEMO REDIS SORTED SETS ===");

        try (Jedis jedis = jedisPool.getResource()) {
            String leaderboardKey = "game:scores";

            // Xóa sorted set cũ nếu tồn tại
            jedis.del(leaderboardKey);

            // Thêm điểm người chơi
            jedis.zadd(leaderboardKey, 100, "Minh");
            jedis.zadd(leaderboardKey, 85, "Hoa");
            jedis.zadd(leaderboardKey, 95, "Nam");
            jedis.zadd(leaderboardKey, 120, "Linh");

            // Lấy người chơi có điểm cao nhất
            List<String> topPlayers = jedis.zrevrange(leaderboardKey, 0, 2);
            System.out.println("3 người chơi điểm cao nhất: " + topPlayers);

            // Lấy điểm của một người chơi
            Double score = jedis.zscore(leaderboardKey, "Nam");
            System.out.println("Điểm của Nam: " + score);

            // Lấy thứ hạng (rank) của người chơi (0-based)
            Long rank = jedis.zrevrank(leaderboardKey, "Nam");
            System.out.println("Thứ hạng của Nam: " + (rank + 1));

            // Tăng điểm
            Double newScore = jedis.zincrby(leaderboardKey, 15, "Hoa");
            System.out.println("Điểm mới của Hoa: " + newScore);

            // Lấy số lượng người chơi
            long count = jedis.zcard(leaderboardKey);
            System.out.println("Tổng số người chơi: " + count);

            // Lấy người chơi trong khoảng điểm
            // Sử dụng API mới nhất của Jedis
            // Vì các phiên bản khác nhau của Jedis có thể có API khác nhau,
            // nên chúng ta sẽ dùng cách đơn giản hơn
            List<String> playersInRange = jedis.zrangeByScore(leaderboardKey, 90, 110);
            System.out.println("Người chơi có điểm từ 90-110: " + playersInRange);
        }
    }

    /**
     * Demo các chức năng hữu ích khác
     */
    public void demoOtherFeatures() {
        System.out.println("\n================================================================");
        System.out.println("||                  6. TÍNH NĂNG KHÁC                         ||");
        System.out.println("================================================================");
        System.out.println("Redis cung cấp nhiều tính năng hữu ích khác cho ứng dụng");

        try (Jedis jedis = jedisPool.getResource()) {
            System.out.println("\n6.1. Đặt nhiều key cùng lúc");
            System.out.println("Command: MSET key1 value1 key2 value2 key3 value3");
            jedis.mset("key1", "value1", "key2", "value2", "key3", "value3");

            System.out.println("\n6.2. Lấy nhiều key cùng lúc");
            System.out.println("Command: MGET key1 key2 key3");
            List<String> values = jedis.mget("key1", "key2", "key3");
            System.out.println("Kết quả: " + values);

            System.out.println("\n6.3. Kiểm tra key tồn tại");
            System.out.println("Command: EXISTS key1");
            boolean exists = jedis.exists("key1");
            System.out.println("Kết quả: " + exists);

            System.out.println("\n6.4. Lấy tất cả key theo pattern");
            System.out.println("Command: KEYS key*");
            Set<String> keys = jedis.keys("key*");
            System.out.println("Kết quả (các key bắt đầu bằng 'key'): " + keys);
            System.out.println("Lưu ý: Trong môi trường production, tránh dùng KEYS vì có thể ảnh hưởng hiệu suất");

            System.out.println("\n6.5. Đặt thời gian hết hạn cho key");
            System.out.println("Command: EXPIRE key1 60");
            jedis.expire("key1", 60);

            System.out.println("Command: TTL key1");
            long ttl = jedis.ttl("key1");
            System.out.println("Kết quả (thời gian sống còn lại của key1): " + ttl + " giây");

            System.out.println("\n6.6. Các tính năng khác");
            System.out.println("• RENAME: Đổi tên key");
            System.out.println("• PERSIST: Xóa thời gian hết hạn của key");
            System.out.println("• TYPE: Kiểm tra kiểu dữ liệu của key");
            System.out.println("• SCAN: Lấy key theo pattern, an toàn hơn KEYS");
            System.out.println("• MULTI/EXEC: Thực hiện giao dịch (transaction)");
            System.out.println("• PUBLISH/SUBSCRIBE: Nhắn tin theo cơ chế pub/sub");

            System.out.println("\n6.7. Xóa tất cả key demo");
            System.out.println("Command: DEL key1 key2 key3 greeting counter session:123");
            jedis.del("key1", "key2", "key3", "greeting", "counter", "session:123");

            System.out.println("Command: DEL tasks team1 team2 user:1001 game:scores");
            jedis.del("tasks", "team1", "team2", "user:1001", "game:scores");
        }
    }

    /**
     * Demo ứng dụng thực tế: Cache
     */
    public void demoCache() {
        System.out.println("\n================================================================");
        System.out.println("||                 7. ỨNG DỤNG THỰC TẾ: CACHE                 ||");
        System.out.println("================================================================");
        System.out.println("Redis thường được sử dụng làm cache để tăng tốc ứng dụng");
        System.out.println("Cache pattern: Kiểm tra cache → Nếu miss → Lấy từ DB → Lưu vào cache");

        String productId = "product:12345";
        String productData = "{\"id\":\"12345\",\"name\":\"Smartphone XYZ\",\"price\":5990000,\"inStock\":true}";

        try (Jedis jedis = jedisPool.getResource()) {
            System.out.println("\n7.1. Mô phỏng cache miss và database load");
            System.out.println("Command: GET " + productId);
            String cachedData = jedis.get(productId);

            if (cachedData == null) {
                System.out.println("Kết quả: null (Cache miss!)");
                System.out.println("\n→ Mô phỏng tải dữ liệu từ database (tốn thời gian)...");

                // Mô phỏng truy vấn database (chậm)
                try {
                    Thread.sleep(500); // Giả lập truy vấn database mất 500ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println("→ Đã tải dữ liệu từ database sau 500ms");

                System.out.println("\n7.2. Lưu vào cache với thời hạn 5 phút");
                System.out.println("Command: SETEX " + productId + " 300 '" + productData + "'");
                jedis.setex(productId, 300, productData);
                System.out.println("→ Đã lưu vào cache");
            }

            System.out.println("\n7.3. Kiểm tra cache hit (lần sau)");
            System.out.println("Command: GET " + productId);
            String cachedProduct = jedis.get(productId);
            System.out.println("Kết quả: " + cachedProduct);
            System.out.println("→ Cache hit! Trả về dữ liệu ngay lập tức");

            System.out.println("\n7.4. Kiểm tra thời gian hết hạn");
            System.out.println("Command: TTL " + productId);
            long expiry = jedis.ttl(productId);
            System.out.println("Kết quả (thời gian còn lại): " + expiry + " giây");

            System.out.println("\n7.5. Cache invalidation khi dữ liệu thay đổi");
            System.out.println("→ Mô phỏng cập nhật sản phẩm trong database");
            System.out.println("Command: DEL " + productId);
            jedis.del(productId);
            System.out.println("→ Đã xóa cache để buộc tải lại từ database khi cần");

            System.out.println("\nCommand: GET " + productId);
            String result = jedis.get(productId);
            System.out.println("Kết quả: " + (result == null ? "null (Cache miss)" : result));

            System.out.println("\n7.6. Các pattern cache phổ biến khác");
            System.out.println("• Cache-Aside: Pattern được mô tả ở trên");
            System.out.println("• Write-Through: Ghi vào cache & DB đồng thời");
            System.out.println("• Write-Behind: Ghi vào cache trước, update DB sau");
            System.out.println("• Read-Through: Cache tự động tải từ DB khi miss");
        }
    }

    public static void main(String[] args) {
        try (RedisDemo demo = new RedisDemo()) {
            System.out.println("================================================================");
            System.out.println("||               REDIS COMMAND TUTORIAL                       ||");
            System.out.println("================================================================");
            System.out.println("Bài hướng dẫn này giới thiệu các kiểu dữ liệu và lệnh Redis cơ bản");
            System.out.println("----------------------------------------------------------------");

            // Demo cấu trúc dữ liệu cơ bản
            demo.demoStrings();
            demo.demoLists();
            demo.demoSets();
            demo.demoHashes();
            demo.demoSortedSets();

            // Demo các tính năng khác
            demo.demoOtherFeatures();

            // Demo ứng dụng thực tế
            demo.demoCache();

            System.out.println("\n================================================================");
            System.out.println("||                    KẾT THÚC DEMO                          ||");
            System.out.println("================================================================");
            System.out.println("Chú ý: Redis là một data structure server với nhiều tính năng hơn");
            System.out.println("những gì được trình bày trong demo này, bao gồm pub/sub, streams,");
            System.out.println("transactions, scripting, và nhiều cấu trúc dữ liệu nâng cao khác.");
        }
    }
}