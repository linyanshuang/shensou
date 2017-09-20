package com.waimaoshensou.google.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;

import com.waimaoshensou.google.MD5Util;

public class GoogleSearch {
	// 线程池的容量
	private static final int POOL_SIZE = Integer.parseInt(MyProperty
			.getKey("search.pool.size"));

	private static final String SEARCH_MODE = MyProperty.getKey("search.mode");
	private static final String SEARCH_MODE_CUSTOM = "0";

	private static final String SPEARTION = "\\|";
	private static final String SPLIT_COMMA = ",";

	// 线程睡眠时间
	private static final int SLEEP_SECOND_MILLISECOND = Integer
			.parseInt(MyProperty.getKey("search.pool.sleep.second.millisecond"));

	private static final String regKey = "search.reg";

	private static RedisService redisService = new RedisService();

	private static Log log = LogFactory.getLog(GoogleSearch.class);
	private static ExecutorService exe = null;
	private static List<String> blackList = new ArrayList<String>();

	public static void main(String[] args) {
		log.info("服务初始化成功！");
		while (true) {
			try {
				if (exe != null) {
					exe.shutdown();
				}
				if (exe == null || exe.isTerminated()) {
					exe = Executors.newFixedThreadPool(POOL_SIZE);
					String keywords = redisService.getKeywordsByRedisList();
					if (keywords == null || keywords.isEmpty()) {
						try {
							Thread.sleep(SLEEP_SECOND_MILLISECOND * 3);
						} catch (InterruptedException e) {
						}
						exe.shutdown();
						// jedis.close();
						continue;
					}
					log.info("从队列从获取到一个新的关键词[" + keywords + "]");

					// 拆分关键词和国家列表
					String[] country = keywords.split(SPEARTION);
					keywords = country[0];
					// 按国家逐个查询
					for (int i = 1; i < country.length; i++) {
						String c = country[i];
						// 检查关键词对应的国家是否已经存在结果
						if (redisService.isExsitKeywords(MD5Util.MD5(keywords
								+ ":" + c))) {
							log.info("关键词[" + keywords + ":" + c
									+ "]信息已经存在,无需重复查询");
							exe.shutdown();
							// jedis.close();
							continue;
						}
						if (SEARCH_MODE_CUSTOM.equals(SEARCH_MODE)) {
							List<String> keylist = redisService.getGoogleKey(1);
							if (keylist == null || keylist.isEmpty()) {
								log.warn("无可用秘钥，重新保存关键词到队列中" + keywords + "|"
										+ c);
								// 将关键词重新保存到队列中
								redisService.saveKeywords2RedisList(keywords
										+ "|" + c);
								exe.shutdown();
								// jedis.close();
								continue;
							}
							String[] regs = MyProperty.getKey(regKey, c).split(
									SPLIT_COMMA);

							GoogleCustomSearch googSearch = new GoogleCustomSearch(
									c, keywords, keylist, regs);
							exe.submit(googSearch);
						} else {
							// 黑名单字段更新
							if (blackList.size() < redisService.getBlackList()
									.size()) {
								blackList = redisService.getBlackList();
							}
							String[] regs = MyProperty.getKey(regKey, c).split(
									SPLIT_COMMA);
							GoogleWebSearch googSearch = new GoogleWebSearch(c,
									keywords, Arrays.asList(regs), blackList);
							exe.submit(googSearch);
						}
					}
					// jedis.close();
					exe.shutdown();
				} else {
					try {
						Thread.sleep(SLEEP_SECOND_MILLISECOND);
					} catch (InterruptedException e) {
					}
				}
			} catch (Exception e) {
				log.error(e);
			}
		}

	}

	public static void testMap(Jedis jedis) {

		Map<String, String> user = new HashMap<String, String>();
		user.put("name", "minxr");
		user.put("pwd", "password");
		jedis.hmset("user", user);
		// 取出user中的name，执行结果:[minxr]-->注意结果是一个泛型的List
		// 第一个参数是存入redis中map对象的key，后面跟的是放入map中的对象的key，后面的key可以跟多个，是可变参数
		List<String> rsmap = jedis.hmget("user", "name");
		System.out.println(rsmap);
		// 删除map中的某个键值
		// jedis.hdel("user","pwd");
		System.out.println(jedis.hmget("user", "pwd")); // 因为删除了，所以返回的是null
		System.out.println(jedis.hlen("user")); // 返回key为user的键中存放的值的个数1
		System.out.println(jedis.exists("user"));// 是否存在key为user的记录 返回true
		System.out.println(jedis.hkeys("user"));// 返回map对象中的所有key [pwd, name]
		System.out.println(jedis.hvals("user"));// 返回map对象中的所有value [minxr,
												// password]
		Iterator<String> iter = jedis.hkeys("user").iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			System.out.println(key + ":" + jedis.hmget("user", key));
		}
	}
}
