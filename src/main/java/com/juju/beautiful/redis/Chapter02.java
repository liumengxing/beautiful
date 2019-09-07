package com.juju.beautiful.redis;

import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class Chapter02 {
    public static final void main(String[] args) throws InterruptedException {
        new Chapter02().run();
    }

    public void run() throws InterruptedException {
        Jedis conn = new Jedis("localhost");
        conn.select(15);

        testLoginCookies(conn);
        testShopppingCartCookies(conn);
        testCacheRows(conn);
        testCacheRequest(conn);
    }

    public void testLoginCookies(Jedis conn) throws InterruptedException {
        System.out.println("\n----- testLoginCookies -----");
        String token = UUID.randomUUID().toString();

        updateToken(conn, token, "username", "itemX");
        System.out.println("We just logged-in/updated token: " + token);
        System.out.println("For user: 'username'");
        System.out.println();

        System.out.println("What username do we get when we look-up that token?");
        String r = checkToken(conn, token);
        System.out.println(r);
        System.out.println();
        assert r != null;

        System.out.println("Let's drop the maximum number of cookies to 0 to clean them out");
        System.out.println("We will start a thread to do the cleaning, while we stop it later");

        CleanSessionsThread thread = new CleanSessionsThread(0);
        thread.start();
        Thread.sleep(1000);
        thread.quit();
        Thread.sleep(2000);
        if (thread.isAlive()) {
            throw new RuntimeException("The clean sessions thread is still alive?!?");
        }

        long s = conn.hlen("login:");
        System.out.println("The current number of sessions still available is: " + s);
        assert s == 0;
    }

    public void testShopppingCartCookies(Jedis conn) throws InterruptedException {
        System.out.println("\n----- testShopppingCartCookies -----");
        String token = UUID.randomUUID().toString();

        System.out.println("We'll refresh our session...");
        updateToken(conn, token, "username", "itemX");
        System.out.println("And add an item to the shopping cart");
        addToCart(conn, token, "itemY", 3);
        Map<String, String> r = conn.hgetAll("cart:" + token);
        System.out.println("Our shopping cart currently has:");
        for (Map.Entry<String, String> entry : r.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        System.out.println();

        assert r.size() >= 1;

        System.out.println("Let's clean out our sessions and carts");
        CleanFullSessionsThread thread = new CleanFullSessionsThread(0);
        thread.start();
        Thread.sleep(1000);
        thread.quit();
        Thread.sleep(2000);
        if (thread.isAlive()) {
            throw new RuntimeException("The clean sessions thread is still alive?!?");
        }

        r = conn.hgetAll("cart:" + token);
        System.out.println("Our shopping cart now contains:");
        for (Map.Entry<String, String> entry : r.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        assert r.size() == 0;
    }

    public void testCacheRows(Jedis conn) throws InterruptedException {
        System.out.println("\n----- testCacheRows -----");
        System.out.println("First, let's schedule caching of itemX every 5 seconds");
        scheduleRowCache(conn, "itemX", 5);
        System.out.println("Our schedule looks like:");
        Set<Tuple> s = conn.zrangeWithScores("schedule:", 0, -1);
        for (Tuple tuple : s) {
            System.out.println("  " + tuple.getElement() + ", " + tuple.getScore());
        }
        assert s.size() != 0;

        System.out.println("We'll start a caching thread that will cache the data...");

        CacheRowsThread thread = new CacheRowsThread();
        thread.start();

        Thread.sleep(1000);
        System.out.println("Our cached data looks like:");
        String r = conn.get("inv:itemX");
        System.out.println(r);
        assert r != null;
        System.out.println();

        System.out.println("We'll check again in 5 seconds...");
        Thread.sleep(5000);
        System.out.println("Notice that the data has changed...");
        String r2 = conn.get("inv:itemX");
        System.out.println(r2);
        System.out.println();
        assert r2 != null;
        assert !r.equals(r2);

        System.out.println("Let's force un-caching");
        scheduleRowCache(conn, "itemX", -1);
        Thread.sleep(1000);
        r = conn.get("inv:itemX");
        System.out.println("The cache was cleared? " + (r == null));
        assert r == null;

        thread.quit();
        Thread.sleep(2000);
        if (thread.isAlive()) {
            throw new RuntimeException("The database caching thread is still alive?!?");
        }
    }

    public void testCacheRequest(Jedis conn) {
        System.out.println("\n----- testCacheRequest -----");
        String token = UUID.randomUUID().toString();

        // 回调函数，定义request的请求结果
        Callback callback = request -> "content for " + request;

        updateToken(conn, token, "username", "itemX");
        String url = "http://test.com/?item=itemX";
        System.out.println("We are going to cache a simple request against " + url);
        String result = cacheRequest(conn, url, callback);
        System.out.println("We got initial content:\n" + result);
        System.out.println();

        assert result != null;

        System.out.println("To test that we've cached the request, we'll pass a bad callback");
        String result2 = cacheRequest(conn, url, null);
        System.out.println("We ended up getting the same response!\n" + result2);

        assert result.equals(result2);

        assert !canCache(conn, "http://test.com/");
        assert !canCache(conn, "http://test.com/?item=itemX&_=1234536");
    }

    /**
     * 检查token对应的用户
     * hash，令牌与用户的映射关系
     * 给定token，直接hget即可
     *
     * @param conn
     * @param token cookie的token令牌
     * @return 用户id
     */
    public String checkToken(Jedis conn, String token) {
        return conn.hget("login:", token);
    }

    /**
     * 操作后更新token
     * hash，令牌与用户的映射关系
     * zset，所有令牌，分值=最后一次使用时间
     * zset，某个令牌访问的商品，分值=时间戳
     *
     * @param conn
     * @param token 令牌
     * @param user  用户
     * @param item  被访问的商品
     */
    public void updateToken(Jedis conn, String token, String user, String item) {
        // 获取当前时间戳
        long timestamp = System.currentTimeMillis() / 1000;
        // 维持token与当前用户的映射关系
        conn.hset("login:", token, user);
        // 有序结合，记录token的最后一次使用时间
        conn.zadd("recent:", timestamp, token);
        if (item != null) {
            // 记录令牌访问了一个商品
            conn.zadd("viewed:" + token, timestamp, item);
            // 移除旧纪录，只保留最近的25个浏览记录
            conn.zremrangeByRank("viewed:" + token, 0, -26);
            conn.zincrby("viewed:", -1, item);
        }
    }

    /**
     * 购物车功能
     * hash，每个session，及其购物车
     *
     * @param conn
     * @param session
     * @param item    商品
     * @param count   购买数量
     */
    public void addToCart(Jedis conn, String session, String item, int count) {
        if (count <= 0) {
            // 购买数量不大于0，从购物车中清除数据
            conn.hdel("cart:" + session, item);
        } else {
            // 将商品、购买数量添加到购物车
            conn.hset("cart:" + session, item, String.valueOf(count));
        }
    }

    /**
     * 缓存数据行时，负责调度和终止缓存
     *
     * @param conn
     * @param rowId 缓存数据行id
     * @param delay 分数
     */
    public void scheduleRowCache(Jedis conn, String rowId, int delay) {
        // 调度开始后，delay时间后，停止调度
        conn.zadd("delay:", delay, rowId);
        // 调度开始时间
        conn.zadd("schedule:", System.currentTimeMillis() / 1000, rowId);
    }

    /**
     * 缓存常用的，不常变化的请求结果
     *
     * @param conn
     * @param request  请求url
     * @param callback 不能缓存时，通过回调函数返回请求结果
     * @return
     */
    public String cacheRequest(Jedis conn, String request, Callback callback) {
        // 不能缓存请求结果时，使用回调函数的生成结果
        if (!canCache(conn, request)) {
            return callback != null ? callback.call(request) : null;
        }

        // key=request的哈希码，value=请求结果
        String pageKey = "cache:" + hashRequest(request);
        String content = conn.get(pageKey);

        // 没有缓存请求的返回结果时，返回回调函数的执行结果，并将结果缓存起来，设置过期时间300s
        if (content == null && callback != null) {
            content = callback.call(request);
            conn.setex(pageKey, 300, content);
        }

        return content;
    }

    public boolean canCache(Jedis conn, String request) {
        try {
            URL url = new URL(request);
            HashMap<String, String> params = new HashMap<String, String>();
            if (url.getQuery() != null) {
                for (String param : url.getQuery().split("&")) {
                    String[] pair = param.split("=", 2);
                    params.put(pair[0], pair.length == 2 ? pair[1] : null);
                }
            }

            // 页面读取itemid
            String itemId = extractItemId(params);
            if (itemId == null || isDynamic(params)) {
                return false;
            }

            // 被浏览次数，确定是否要缓存
            Long rank = conn.zrank("viewed:", itemId);
            return rank != null && rank < 10000;
        } catch (MalformedURLException mue) {
            return false;
        }
    }

    private boolean isDynamic(Map<String, String> params) {
        return params.containsKey("_");
    }

    private String extractItemId(Map<String, String> params) {
        return params.get("item");
    }

    private String hashRequest(String request) {
        return String.valueOf(request.hashCode());
    }

    public interface Callback {
        String call(String request);
    }

    /**
     * 清理session
     * {@link CleanFullSessionsThread}
     */
    public class CleanSessionsThread extends Thread {
        private Jedis conn;
        private int limit;
        private boolean quit;

        CleanSessionsThread(int limit) {
            this.conn = new Jedis("localhost");
            this.conn.select(15);
            this.limit = limit;
        }

        void quit() {
            quit = true;
        }

        @Override
        public void run() {
            while (!quit) {
                long size = conn.zcard("recent:");
                if (size <= limit) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }

                long endIndex = Math.min(size - limit, 100);
                Set<String> tokenSet = conn.zrange("recent:", 0, endIndex - 1);
                String[] tokens = tokenSet.toArray(new String[0]);

                ArrayList<String> sessionKeys = new ArrayList<>();
                for (String token : tokens) {
                    sessionKeys.add("viewed:" + token);
                }

                conn.del(sessionKeys.toArray(new String[0]));
                conn.hdel("login:", tokens);
                conn.zrem("recent:", tokens);
            }
        }
    }

    /**
     * 清理session
     */
    public class CleanFullSessionsThread extends Thread {
        private Jedis conn;
        /**
         * redis允许保存的session个数
         */
        private int limit;
        private boolean quit;

        CleanFullSessionsThread(int limit) {
            this.conn = new Jedis("localhost");
            this.conn.select(15);
            this.limit = limit;
        }

        void quit() {
            quit = true;
        }

        @Override
        public void run() {
            while (!quit) {
                // 当前已经存储的session个数
                long size = conn.zcard("recent:");
                // 当前不需要清理时，休眠1s
                if (size <= limit) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }

                // 需要清理的session个数，每次最多清理100个
                long endIndex = Math.min(size - limit, 100);
                // 最早的session集合
                Set<String> sessionSet = conn.zrange("recent:", 0, endIndex - 1);
                String[] sessions = sessionSet.toArray(new String[0]);

                ArrayList<String> sessionKeys = new ArrayList<>();
                for (String sess : sessions) {
                    sessionKeys.add("viewed:" + sess);
                    sessionKeys.add("cart:" + sess);
                }

                // 删除session相关的各种信息
                // 购物车
                conn.del(sessionKeys.toArray(new String[0]));
                // 在令牌与用户的映射关系hash中，删除某个session
                conn.hdel("login:", sessions);
                // 删除令牌对应的浏览记录的zset
                conn.zrem("recent:", sessions);
            }
        }
    }

    /**
     * 缓存数据行
     * 调度函数：{@link #scheduleRowCache}
     */
    public class CacheRowsThread extends Thread {
        private Jedis conn;
        private boolean quit;

        CacheRowsThread() {
            this.conn = new Jedis("localhost");
            this.conn.select(15);
        }

        void quit() {
            quit = true;
        }

        @Override
        public void run() {
            Gson gson = new Gson();
            while (!quit) {
                // 获取下一个需要被缓存的数据行的调度时间
                Set<Tuple> range = conn.zrangeWithScores("schedule:", 0, 0);
                Tuple next = range.size() > 0 ? range.iterator().next() : null;
                long now = System.currentTimeMillis() / 1000;
                // 没有读取到，休眠50ms继续
                if (next == null || next.getScore() > now) {
                    try {
                        sleep(50);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }

                // 停止调度判断
                String rowId = next.getElement();
                double delay = conn.zscore("delay:", rowId);
                // 停止调度的时候，移除两个zset，移除数据行信息
                if (delay <= 0) {
                    conn.zrem("delay:", rowId);
                    conn.zrem("schedule:", rowId);
                    conn.del("inv:" + rowId);
                    continue;
                }

                // 从数据库读取inventory的最新信息
                Inventory row = Inventory.get(rowId);
                // 下一次缓存inventory的时间
                conn.zadd("schedule:", now + delay, rowId);
                // 将inventory信息写入缓存
                conn.set("inv:" + rowId, gson.toJson(row));
            }
        }
    }

    public static class Inventory {
        private String id;
        private String data;
        private long time;

        private Inventory(String id) {
            this.id = id;
            this.data = "data to cache...";
            this.time = System.currentTimeMillis() / 1000;
        }

        public static Inventory get(String id) {
            return new Inventory(id);
        }
    }
}
