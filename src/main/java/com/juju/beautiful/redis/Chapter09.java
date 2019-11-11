package com.juju.beautiful.redis;

import org.javatuples.Pair;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ZParams;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.IntStream;
import java.util.zip.CRC32;

public class Chapter09 {
    private static final String[] COUNTRIES = (
            "ABW AFG AGO AIA ALA ALB AND ARE ARG ARM ASM ATA ATF ATG AUS AUT AZE BDI BEL BEN BES BFA BGD BGR BHR BHS BIH BLM BLR BLZ BMU BOL BRA BRB BRN BTN " +
                    "BVT BWA CAF CAN CCK CHE CHL CHN CIV CMR COD COG COK COL COM CPV CRI CUB CUW CXR CYM CYP CZE DEU DJI DMA DNK DOM DZA ECU EGY ERI ESH ESP EST ETH " +
                    "FIN FJI FLK FRA FRO FSM GAB GBR GEO GGY GHA GIB GIN GLP GMB GNB GNQ GRC GRD GRL GTM GUF GUM GUY HKG HMD HND HRV HTI HUN IDN IMN IND IOT IRL IRN " +
                    "IRQ ISL ISR ITA JAM JEY JOR JPN KAZ KEN KGZ KHM KIR KNA KOR KWT LAO LBN LBR LBY LCA LIE LKA LSO LTU LUX LVA MAC MAF MAR MCO MDA MDG MDV MEX MHL " +
                    "MKD MLI MLT MMR MNE MNG MNP MOZ MRT MSR MTQ MUS MWI MYS MYT NAM NCL NER NFK NGA NIC NIU NLD NOR NPL NRU NZL OMN PAK PAN PCN PER PHL PLW PNG POL " +
                    "PRI PRK PRT PRY PSE PYF QAT REU ROU RUS RWA SAU SDN SEN SGP SGS SHN SJM SLB SLE SLV SMR SOM SPM SRB SSD STP SUR SVK SVN SWE SWZ SXM SYC SYR TCA " +
                    "TCD TGO THA TJK TKL TKM TLS TON TTO TUN TUR TUV TWN TZA UGA UKR UMI URY USA UZB VAT VCT VEN VGB VIR VNM VUT WLF WSM YEM ZAF ZMB ZWE"
    ).split(" ");

    private static final Map<String, String[]> STATES = new HashMap<>();

    static {
        STATES.put("CAN", "AB BC MB NB NL NS NT NU ON PE QC SK YT".split(" "));
        STATES.put("USA", ("AA AE AK AL AP AR AS AZ CA CO CT DC DE FL FM GA GU HI IA ID IL IN KS KY LA MA MD ME MH MI MN MO MP " +
                "MS MT NC ND NE NH NJ NM NV NY OH OK OR PA PR PW RI SC SD TN TX UT VA VI VT WA WI WV WY"
        ).split(" "));
    }

    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:00:00");

    static {
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        new Chapter09().run();
    }

    public void run() {
        Jedis conn = new Jedis("localhost");
        conn.select(15);
        conn.flushDB();

        testLongZiplistPerformance(conn);
        testShardKey();
        testShardedHash(conn);
        testShardedSadd(conn);
        testUniqueVisitors(conn);
        testUserLocation(conn);
    }

    public void testLongZiplistPerformance(Jedis conn) {
        longZiplistPerformance(conn, "test", 5, 10, 10);
        assert conn.llen("test") == 5;
    }

    public void testShardKey() {
        String base = "test";
        assert "test:0".equals(shardKey(base, "1", 2, 2));
        assert "test:1".equals(shardKey(base, "125", 1000, 100));

        for (int i = 0; i < 50; i++) {
            String key = shardKey(base, "hello:" + i, 1000, 100);
            String[] parts = key.split(":");
            assert Integer.parseInt(parts[parts.length - 1]) < 20;

            key = shardKey(base, String.valueOf(i), 1000, 100);
            parts = key.split(":");
            assert Integer.parseInt(parts[parts.length - 1]) < 10;
        }
    }

    public void testShardedHash(Jedis conn) {
        IntStream.range(0, 50).forEach(i -> {
            String istr = String.valueOf(i);
            shardHset(conn, "test", "keyname:" + i, istr, 1000, 100);
            assert istr.equals(shardHget(conn, "test", "keyname:" + i, 1000, 100));
            shardHset(conn, "test2", istr, istr, 1000, 100);
            assert istr.equals(shardHget(conn, "test2", istr, 1000, 100));
        });
    }

    public void testShardedSadd(Jedis conn) {
        IntStream.range(0, 50).forEach(i -> shardSadd(conn, "testx", String.valueOf(i), 50, 50));
        assert conn.scard("testx:0") + conn.scard("testx:1") == 50;
    }

    public void testUniqueVisitors(Jedis conn) {
        DAILY_EXPECTED = 10000;
        IntStream.range(0, 179).forEach(i -> countVisit(conn, UUID.randomUUID().toString()));
        assert "179".equals(conn.get("unique:" + ISO_FORMAT.format(new Date())));

        conn.flushDB();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        conn.set("unique:" + ISO_FORMAT.format(yesterday.getTime()), "1000");
        IntStream.range(0, 183).forEach(i -> countVisit(conn, UUID.randomUUID().toString()));
        assert "183".equals(conn.get("unique:" + ISO_FORMAT.format(new Date())));
    }

    public void testUserLocation(Jedis conn) {
        int i = 0;
        for (String country : COUNTRIES) {
            if (STATES.containsKey(country)) {
                for (String state : STATES.get(country)) {
                    setLocation(conn, i, country, state);
                    i++;
                }
            } else {
                setLocation(conn, i, country, "");
                i++;
            }
        }

        Pair<Map<String, Long>, Map<String, Map<String, Long>>> _aggs = aggregateLocation(conn);
        long[] userIds = new long[i + 1];
        IntStream.rangeClosed(0, i).forEach(j -> userIds[j] = j);
        Pair<Map<String, Long>, Map<String, Map<String, Long>>> aggs = aggregateLocationList(conn, userIds);
        assert _aggs.equals(aggs);

        Map<String, Long> countries = aggs.getValue0();
        Map<String, Map<String, Long>> states = aggs.getValue1();
        aggs.getValue0().keySet().forEach(country -> {
            if (STATES.containsKey(country)) {
                assert STATES.get(country).length == countries.get(country);
                Arrays.stream(STATES.get(country)).forEach(state -> {
                    assert states.get(country).get(state) == 1;
                });
            } else {
                assert countries.get(country) == 1;
            }
        });
    }

    /**
     * ziplist的性能测试
     * 随着数据量的增加，从列表左侧取一个数据，再加到右侧
     *
     * @param conn
     * @param key    测试的列表的key
     * @param length 要为列表中添加的元素个数
     * @param passes 从左侧取出，右侧加入的元素个数
     * @param psize  每个数据的字节数
     * @return
     */
    public double longZiplistPerformance(Jedis conn, String key, int length, int passes, int psize) {
        // 构造列表，加入length个元素
        conn.del(key);
        IntStream.range(0, length)
                .forEach(i -> conn.rpush(key, String.valueOf(i)));

        Pipeline pipeline = conn.pipelined();
        // 操作开始的时间
        long time = System.currentTimeMillis();
        // passes个元素执行右侧出，左侧入操作
        IntStream.range(0, passes).forEach(p -> {
            IntStream.range(0, psize).forEach(pi -> pipeline.rpoplpush(key, key));
            pipeline.sync();
        });

        // 总字节数 / 总时间
        // 单位时间，移动元素字节数
        return (passes * psize) / (System.currentTimeMillis() - time);
    }

    /**
     * 将key-value添加到散列中，根据key值计算分片后的key值
     *
     * @param base          原散列名称
     * @param key           即将加入散列的key值
     * @param totalElements 预计元素总数，变化后整个散列重新分片
     * @param shardSize     分片数量，变化后整个散列重新分片
     * @return 分片后的元素key值：（原散列：分片ID）
     */
    public String shardKey(String base, String key, long totalElements, int shardSize) {
        long shardId;
        if (isDigit(key)) {
            // key值是数字，直接用来计算分片id
            shardId = Integer.parseInt(key, 10) / shardSize;
        } else {
            // 计算key的散列值与分片数量之间的模数，得到分片id
            // CRC32比MD5和SHA1计算速度更快，返回整数
            CRC32 crc = new CRC32();
            crc.update(key.getBytes());
            long shards = 2 * totalElements / shardSize;
            shardId = Math.abs(((int) crc.getValue()) % shards);
        }
        return base + ':' + shardId;
    }

    /**
     * 将key-value加入分片散列中
     */
    public Long shardHset(Jedis conn, String base, String key, String value, long totalElements, int shardSize) {
        // 计算分片的key值
        String shard = shardKey(base, key, totalElements, shardSize);
        // 写入分片散列
        return conn.hset(shard, key, value);
    }

    /**
     * 获取分片散列中key对应的value
     */
    public String shardHget(Jedis conn, String base, String key, int totalElements, int shardSize) {
        // 计算分片的key值
        String shard = shardKey(base, key, totalElements, shardSize);
        // 从分片散列中获取key的value
        return conn.hget(shard, key);
    }

    /**
     * 将member加入分片集合中
     */
    public Long shardSadd(Jedis conn, String base, String member, long totalElements, int shardSize) {
        // 计算分片集合的key
        String shard = shardKey(base, "x" + member, totalElements, shardSize);
        // 将member加入某个分片集合
        return conn.sadd(shard, member);
    }

    private int SHARD_SIZE = 512;

    /**
     * 记录唯一访客数量
     *
     * @param conn
     * @param sessionId 用来获取用户的UUID
     */
    public void countVisit(Jedis conn, String sessionId) {
        Calendar today = Calendar.getInstance();
        // 用当天日期，作为唯一访客计数器的key
        String key = "unique:" + ISO_FORMAT.format(today.getTime());
        // 预计访客数量
        long expected = getExpected(conn, key, today);
        // 根据UUID，计算出56位的id
        // 将id加入分片集合（存储所有访客的id）中，更新唯一访客数量
        // 如果id还没有计入到唯一访客的分片集合中时，计数器+1
        long id = Long.parseLong(sessionId.replace("-", "").substring(0, 15), 16);
        if (shardSadd(conn, key, String.valueOf(id), expected, SHARD_SIZE) != 0) {
            conn.incr(key);

        }
    }

    private long DAILY_EXPECTED = 1000000;
    private Map<String, Long> EXPECTED = new HashMap<>();

    /**
     * 根据预定的简单算法，通过昨天的访客数量，计算今天预计的访客数量
     */
    public long getExpected(Jedis conn, String key, Calendar today) {
        if (!EXPECTED.containsKey(key)) {
            String exkey = key + ":expected";
            String expectedStr = conn.get(exkey);

            long expected;
            if (expectedStr == null) {
                Calendar yesterday = (Calendar) today.clone();
                yesterday.add(Calendar.DATE, -1);
                expectedStr = conn.get("unique:" + ISO_FORMAT.format(yesterday.getTime()));
                expected = expectedStr != null ? Long.parseLong(expectedStr) : DAILY_EXPECTED;
                expected = (long) Math.pow(2, (long) (Math.ceil(Math.log(expected * 1.5) / Math.log(2))));
                if (conn.setnx(exkey, String.valueOf(expected)) == 0) {
                    expectedStr = conn.get(exkey);
                    expected = Integer.parseInt(expectedStr);
                }
            } else {
                expected = Long.parseLong(expectedStr);
            }
            EXPECTED.put(key, expected);
        }
        return EXPECTED.get(key);
    }

    private long USERS_PER_SHARD = (long) Math.pow(2, 20);

    public void setLocation(Jedis conn, long userId, String country, String state) {
        String code = getCode(country, state);

        long shardId = userId / USERS_PER_SHARD;
        int position = (int) (userId % USERS_PER_SHARD);
        int offset = position * 2;

        Pipeline pipe = conn.pipelined();
        pipe.setrange("location:" + shardId, offset, code);

        String tkey = UUID.randomUUID().toString();
        pipe.zadd(tkey, userId, "max");
        pipe.zunionstore("location:max", new ZParams().aggregate(ZParams.Aggregate.MAX), tkey, "location:max");
        pipe.del(tkey);
        pipe.sync();
    }

    public Pair<Map<String, Long>, Map<String, Map<String, Long>>> aggregateLocation(Jedis conn) {
        Map<String, Long> countries = new HashMap<>();
        Map<String, Map<String, Long>> states = new HashMap<>();

        long maxBlock = conn.zscore("location:max", "max").longValue();
        byte[] buffer = new byte[(int) Math.pow(2, 17)];
        for (int shardId = 0; shardId <= maxBlock; shardId++) {
            try (InputStream in = new RedisInputStream(conn, "location:" + shardId)) {
                int read;
                while ((read = in.read(buffer, 0, buffer.length)) != -1) {
                    for (int offset = 0; offset < read - 1; offset += 2) {
                        String code = new String(buffer, offset, 2);
                        updateAggregates(countries, states, code);
                    }
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
        return new Pair<>(countries, states);
    }

    public Pair<Map<String, Long>, Map<String, Map<String, Long>>> aggregateLocationList(Jedis conn, long[] userIds) {
        Map<String, Long> countries = new HashMap<>();
        Map<String, Map<String, Long>> states = new HashMap<>();

        Pipeline pipe = conn.pipelined();
        IntStream.range(0, userIds.length).forEach(i -> {
            long userId = userIds[i];
            long shardId = userId / USERS_PER_SHARD;
            int position = (int) (userId % USERS_PER_SHARD);
            int offset = position * 2;
            pipe.substr("location:" + shardId, offset, offset + 1);
            if ((i + 1) % 1000 == 0) {
                updateAggregates(countries, states, pipe.syncAndReturnAll().toString());
            }
        });

        updateAggregates(countries, states, pipe.syncAndReturnAll().toString());
        return new Pair<>(countries, states);
    }

    public void updateAggregates(Map<String, Long> countries, Map<String, Map<String, Long>> states, String code) {
        if (code.length() != 2) {
            return;
        }

        int countryIdx = (int) code.charAt(0) - 1;
        int stateIdx = (int) code.charAt(1) - 1;
        if (countryIdx < 0 || countryIdx >= COUNTRIES.length) {
            return;
        }

        String country = COUNTRIES[countryIdx];
        Long countryAgg = countries.get(country);
        if (countryAgg == null) {
            countryAgg = 0L;
        }
        countries.put(country, countryAgg + 1);

        if (!STATES.containsKey(country)) {
            return;
        }
        if (stateIdx < 0 || stateIdx >= STATES.get(country).length) {
            return;
        }

        String state = STATES.get(country)[stateIdx];
        Map<String, Long> stateAggs = states.get(country);
        if (stateAggs == null) {
            stateAggs = new HashMap<>();
            states.put(country, stateAggs);
        }
        Long stateAgg = stateAggs.get(state);
        if (stateAgg == null) {
            stateAgg = 0L;
        }
        stateAggs.put(state, stateAgg + 1);
    }

    public String getCode(String country, String state) {
        int cindex = bisectLeft(COUNTRIES, country);
        if (cindex > COUNTRIES.length || !country.equals(COUNTRIES[cindex])) {
            cindex = -1;
        }
        cindex++;

        int sindex = -1;
        if (state != null && STATES.containsKey(country)) {
            String[] states = STATES.get(country);
            sindex = bisectLeft(states, state);
            if (sindex > states.length || !state.equals(states[sindex])) {
                sindex--;
            }
        }
        sindex++;

        return new String(new char[]{(char) cindex, (char) sindex});
    }

    private int bisectLeft(String[] values, String key) {
        int index = Arrays.binarySearch(values, key);
        return index < 0 ? Math.abs(index) - 1 : index;
    }

    private boolean isDigit(String string) {
        for (char c : string.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public class RedisInputStream extends InputStream {
        private Jedis conn;
        private String key;
        private int pos;

        public RedisInputStream(Jedis conn, String key) {
            this.conn = conn;
            this.key = key;
        }

        @Override
        public int available() {
            long len = conn.strlen(key);
            return (int) (len - pos);
        }

        @Override
        public int read() {
            byte[] block = conn.substr(key.getBytes(), pos, pos);
            if (block == null || block.length == 0) {
                return -1;
            }
            pos++;
            return (block[0] & 0xff);
        }

        @Override
        public int read(byte[] buf, int off, int len) {
            byte[] block = conn.substr(key.getBytes(), pos, pos + (len - off - 1));
            if (block == null || block.length == 0) {
                return -1;
            }
            System.arraycopy(block, 0, buf, off, block.length);
            pos += block.length;
            return block.length;
        }

        @Override
        public void close() {
        }
    }
}