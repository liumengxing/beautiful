package com.juju.beautiful.redis;

import redis.clients.jedis.Jedis;

/**
 * 工作电脑上，redis测试
 */
public class WindowsRedis {
    private static final String REDIS_ADDR = "172.20.181.100";

    public static void main(String[] args) {
        // 登陆
        Jedis conn = new Jedis(REDIS_ADDR, 63791);
        conn.auth("123456");
        conn.select(0);

        // string
        String juju = conn.get("juju");
        String juju2 = conn.get("juju2");

        conn.incr("date.year");
        conn.incrBy("date.year", 2);
        System.out.println(conn.get("date.year"));

        conn.decr("date.year");
        conn.decrBy("date.year", 2);
        System.out.println(conn.get("date.year"));

    }
}
