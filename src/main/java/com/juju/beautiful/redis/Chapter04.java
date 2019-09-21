package com.juju.beautiful.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 功能：
 * 1、将客户的商品放到市场销售
 * 2、购买商品
 */
public class Chapter04 {
    public static void main(String[] args) {
        new Chapter04().run();
    }

    public void run() {
        Jedis conn = new Jedis("localhost");
        conn.select(15);

        testListItem(conn, false);
        testPurchaseItem(conn);
        testBenchmarkUpdateToken(conn);
    }

    public void testListItem(Jedis conn, boolean nested) {
        if (!nested) {
            System.out.println("\n----- testListItem -----");
        }

        System.out.println("We need to set up just enough state so that a user can list an item");
        String seller = "userX";
        String item = "itemX";
        conn.sadd("inventory:" + seller, item);
        Set<String> i = conn.smembers("inventory:" + seller);

        System.out.println("The user's inventory has:");
        i.stream().map(member -> "  " + member)
                .forEach(System.out::println);
        System.out.println();

        System.out.println("Listing the item...");
        boolean l = listItem(conn, item, seller, 10);
        System.out.println("Listing the item succeeded? " + l);
        Set<Tuple> r = conn.zrangeWithScores("market:", 0, -1);
        System.out.println("The market contains:");
        r.stream().map(tuple -> "  " + tuple.getElement() + ", " + tuple.getScore())
                .forEach(System.out::println);
    }

    public void testPurchaseItem(Jedis conn) {
        System.out.println("\n----- testPurchaseItem -----");
        testListItem(conn, true);

        System.out.println("We need to set up just enough state so a user can buy an item");
        conn.hset("users:userY", "funds", "125");
        Map<String, String> r = conn.hgetAll("users:userY");
        System.out.println("The user has some money:");
        r.entrySet().stream().map(entry -> "  " + entry.getKey() + ": " + entry.getValue())
                .forEach(System.out::println);
        System.out.println();

        System.out.println("Let's purchase an item");
        boolean p = purchaseItem(conn, "userY", "itemX", "userX", 10);
        System.out.println("Purchasing an item succeeded? " + p);

        r = conn.hgetAll("users:userY");
        System.out.println("Their money is now:");
        r.entrySet().stream()
                .map(entry -> "  " + entry.getKey() + ": " + entry.getValue())
                .forEach(System.out::println);


        String buyer = "userY";
        Set<String> i = conn.smembers("inventory:" + buyer);
        System.out.println("Their inventory is now:");
        i.stream().map(member -> "  " + member)
                .forEach(System.out::println);
    }

    public void testBenchmarkUpdateToken(Jedis conn) {
        System.out.println("\n----- testBenchmarkUpdate -----");
        benchmarkUpdateToken(conn, 5);
    }

    /**
     * 将用户的某个商品，添加到商品市场
     *
     * @param conn
     * @param itemId   商品id
     * @param sellerId 卖家用户id
     * @param price    价格
     * @return 是否添加成功
     */
    public boolean listItem(Jedis conn, String itemId, String sellerId, double price) {
        String inventory = "inventory:" + sellerId;
        String item = itemId + '.' + sellerId;
        long end = System.currentTimeMillis() + 5000;

        // 5s内重试，直至成功or超时
        while (System.currentTimeMillis() < end) {
            // 监视用户的包裹
            conn.watch(inventory);
            // 不再持有要销售的商品，添加失败
            if (!conn.sismember(inventory, itemId)) {
                conn.unwatch();
                return false;
            }

            // 将商品添加到商品市场，从用户的包裹中移除商品
            Transaction trans = conn.multi();
            trans.zadd("market:", price, item);
            trans.srem(inventory, itemId);
            List<Object> results = trans.exec();
            // null response indicates that the transaction was aborted due to the watched key changing.
            if (results == null) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * 从商品市场购买商品
     *
     * @param conn
     * @param buyerId  买家用户id
     * @param itemId   商品id
     * @param sellerId 卖家用户id
     * @param lprice   售价
     * @return 是否购买成功
     */
    public boolean purchaseItem(Jedis conn, String buyerId, String itemId, String sellerId, double lprice) {
        String buyer = "users:" + buyerId;
        String seller = "users:" + sellerId;
        String item = itemId + '.' + sellerId;
        String inventory = "inventory:" + buyerId;
        long end = System.currentTimeMillis() + 10000;

        // 10s内重试，直至成功or超时
        while (System.currentTimeMillis() < end) {
            // 监视商品市场、买家信息
            conn.watch("market:", buyer);

            // 商品价格，买家余额
            double price = conn.zscore("market:", item);
            double funds = Double.parseDouble(conn.hget(buyer, "funds"));
            // 商品价格变化，买家余额不足，购买失败
            if (price != lprice || price > funds) {
                conn.unwatch();
                return false;
            }

            Transaction trans = conn.multi();
            // 卖家余额增加
            trans.hincrBy(seller, "funds", (int) price);
            // 买家余额减少
            trans.hincrBy(buyer, "funds", (int) -price);
            // 商品添加到买家包裹
            trans.sadd(inventory, itemId);
            // 商品从市场移除
            trans.zrem("market:", item);
            List<Object> results = trans.exec();
            // null response indicates that the transaction was aborted due to the watched key changing.
            if (results == null) {
                continue;
            }
            return true;
        }

        return false;
    }

    /**
     * 测试两个更新token函数
     *
     * @param conn
     * @param duration
     */
    public void benchmarkUpdateToken(Jedis conn, int duration) {
        try {
            @SuppressWarnings("rawtypes")
            Class[] args = new Class[]{Jedis.class, String.class, String.class, String.class};
            Method[] methods = new Method[]{
                    this.getClass().getDeclaredMethod("updateToken", args),
                    this.getClass().getDeclaredMethod("updateTokenPipeline", args),
            };
            for (Method method : methods) {
                int count = 0;
                long start = System.currentTimeMillis();
                long end = start + (duration * 1000);
                while (System.currentTimeMillis() < end) {
                    count++;
                    method.invoke(this, conn, "token", "user", "item");
                }
                long delta = System.currentTimeMillis() - start;
                System.out.println(method.getName() + ' ' + count + ' ' + (delta / 1000) + ' ' + (count / (delta / 1000)));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 更新token函数
     * 与redis交互5次
     */
    public void updateToken(Jedis conn, String token, String user, String item) {
        long timestamp = System.currentTimeMillis() / 1000;
        conn.hset("login:", token, user);
        conn.zadd("recent:", timestamp, token);
        if (item != null) {
            conn.zadd("viewed:" + token, timestamp, item);
            conn.zremrangeByRank("viewed:" + token, 0, -26);
            conn.zincrby("viewed:", -1, item);
        }
    }

    /**
     * 非事务型流水线
     * 与redis交互1次
     */
    public void updateTokenPipeline(Jedis conn, String token, String user, String item) {
        long timestamp = System.currentTimeMillis() / 1000;
        Pipeline pipe = conn.pipelined();
        pipe.multi();
        pipe.hset("login:", token, user);
        pipe.zadd("recent:", timestamp, token);
        if (item != null) {
            pipe.zadd("viewed:" + token, timestamp, item);
            pipe.zremrangeByRank("viewed:" + token, 0, -26);
            pipe.zincrby("viewed:", -1, item);
        }
        pipe.exec();
    }
}