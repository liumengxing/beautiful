package com.juju.beautiful.redis.redisinaction;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ZParams;

import java.util.*;

public class Chapter01 {
    private static final int ONE_WEEK_IN_SECONDS = 7 * 86400;
    private static final int VOTE_SCORE = 432;
    private static final int ARTICLES_PER_PAGE = 25;

    public static void main(String[] args) {
        new Chapter01().run();
    }

    private void run() {
        Jedis conn = new Jedis("localhost");
        conn.select(15);

        String articleId = postArticle(conn, "username", "A title", "http://www.google.com");
        System.out.println("We posted a new article with id: " + articleId);
        System.out.println("Its HASH looks like:");
        Map<String, String> articleData = conn.hgetAll("article:" + articleId);
        articleData.entrySet().stream().map(entry -> "  " + entry.getKey() + ": " + entry.getValue())
                .forEach(System.out::println);

        System.out.println();

        articleVote(conn, "other_user", "article:" + articleId);
        String votes = conn.hget("article:" + articleId, "votes");
        System.out.println("We voted for the article, it now has votes: " + votes);

        System.out.println("The currently highest-scoring articles are:");
        List<Map<String, String>> articles = getArticles(conn, 1);
        printArticles(articles);

        addGroups(conn, articleId, new String[]{"new-group"});
        System.out.println("We added the article to a new group, other articles include:");
        articles = getGroupArticles(conn, "new-group", 1);
        printArticles(articles);
    }

    /**
     * 发布文章
     * hash，article:articleID 文章信息，poster、title、link、posttime、votes等，此处创建新对象
     *
     * @param conn
     * @param user
     * @param title
     * @param link
     * @return articleID
     */
    private String postArticle(Jedis conn, String user, String title, String link) {
        // string，生成articleID
        String articleId = String.valueOf(conn.incr("article:nextID"));

        // 初始化该文章的投票用户集合
        // 默认自己投了一票
        // 一周后不能投票，设置投票集合的过期时间 = 一周
        String voted = "voted:" + articleId;
        conn.sadd(voted, user);
        conn.expire(voted, ONE_WEEK_IN_SECONDS);

        // 创建文章信息（散列）
        long now = System.currentTimeMillis() / 1000;
        String article = "article:" + articleId;
        HashMap<String, String> articleData = new HashMap<>();
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");
        // 添加到所有的文章信息的散列中
        conn.hmset(article, articleData);

        // 向集合（发布时间、分数）中添加文章的数据
        conn.zadd("score:", now + VOTE_SCORE, article);
        conn.zadd("time:", now, article);

        return articleId;
    }

    /**
     * 投票
     * zset，time:articleID 发布时间
     * zset，score:articleID 分数
     * hash，article:articleID 文章信息，此处更新votes
     * set，voted:articleID某个文章的投票用户id集合
     *
     * @param conn
     * @param user    user:userid，此处只传了username
     * @param article article:articleID
     */
    private void articleVote(Jedis conn, String user, String article) {
        // 文章发布一周内可以投票，超过一周不能投票
        long cutoff = (System.currentTimeMillis() / 1000) - ONE_WEEK_IN_SECONDS;
        if (conn.zscore("time:", article) < cutoff) {
            return;
        }

        String articleId = article.substring(article.indexOf(':') + 1);
        // 将userid记录到文章的投票用户集合中
        if (conn.sadd("voted:" + articleId, user) == 1) {
            // 添加成功，投票成功，更新文章分数、票数
            conn.zincrby("score:", VOTE_SCORE, article);
            conn.hincrBy(article, "votes", 1);
        }
    }


    private List<Map<String, String>> getArticles(Jedis conn, int page) {
        return getArticles(conn, page, "score:");
    }

    /**
     * 获取所有文章信息，读取postArticle中，生成的hash对象
     *
     * @param conn
     * @param page  第page页，每页25篇
     * @param order 此处直接作为key值使用
     * @return
     */
    private List<Map<String, String>> getArticles(Jedis conn, int page, String order) {
        int start = (page - 1) * ARTICLES_PER_PAGE;
        int end = start + ARTICLES_PER_PAGE - 1;

        // 获取第page张的25篇文章id
        Set<String> ids = conn.zrevrange(order, start, end);
        List<Map<String, String>> articles = new ArrayList<>();
        // 逐一读取每个id的文章
        ids.forEach(id -> {
            Map<String, String> articleData = conn.hgetAll(id);
            articleData.put("id", id);
            articles.add(articleData);
        });

        return articles;
    }

    /**
     * 将一篇文章添加到一些分组中
     *
     * @param conn
     * @param articleId 文章id
     * @param toAdd     文章要加入的group的key
     */
    private void addGroups(Jedis conn, String articleId, String[] toAdd) {
        String article = "article:" + articleId;
        Arrays.stream(toAdd)
                .forEach(group -> conn.sadd("group:" + group, article));
    }

    private List<Map<String, String>> getGroupArticles(Jedis conn, String group, int page) {
        return getGroupArticles(conn, group, page, "score:");
    }

    /**
     * 获取某个分组，某一页的文章信息
     *
     * @param conn
     * @param group 分组名称
     * @param page  页数
     * @param order key值的一部分
     * @return
     */
    private List<Map<String, String>> getGroupArticles(Jedis conn, String group, int page, String order) {
        String key = order + group;
        // 先取缓存，不存在时，实时排序，排序结果缓存60s
        if (!conn.exists(key)) {
            ZParams params = new ZParams().aggregate(ZParams.Aggregate.MAX);
            conn.zinterstore(key, params, "group:" + group, order);
            // 缓存60s
            conn.expire(key, 60);
        }
        return getArticles(conn, page, key);
    }

    private void printArticles(List<Map<String, String>> articles) {
        articles.forEach(article -> {
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String, String> entry : article.entrySet()) {
                if ("id".equals(entry.getKey())) {
                    return;
                }
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        });
    }
}
