package com.waimaoshensou.google.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import com.waimaoshensou.util.StringUtils;

public class RedisService {

	private Log log = LogFactory.getLog(RedisService.class);

	/**
	 * 存储关键词对应的URL
	 */
	private static final String KEY_RESULT_KEYWORDS_PRE_FIX = "SHENSOU_RESULT_KEYWORDS:";

	/**
	 * 黑名单KEY
	 */
	private static final String BLACK_LIST = "BLACK_LIST";

	/**
	 * 存储所有未查询URL列表
	 */
	private static final String NEW_URL_LIST_PRE_FIX = "SHENSOU_NEW_URL_LIST";

	/**
	 * 存储关键词列表
	 */
	private static String KEY_SHENSOU_RESULT_LIST_PRE_FIX = "SHENSOU_RESULT_KEYWORDS_LIST";

	/**
	 * 存储总的查询结果
	 */
	private static String KEY_SHENSOU_RESULT_TOTAL_PRE_FIX = "SHENSOU_RESULT_KEYWORDS_TOTAL";
	/**
	 * 存储总的查询结果
	 */
	private static String ALL_USER_TOTAL = "ALL_USER_TOTAL";
	/**
	 * 谷歌秘钥有序Set存储 ，score：查询次数 使用中
	 */
	private static String KEY_GOOGLE_KEY_USED_PRE_FIX = "SHENSOU_GOOGLE_USED_KEY_SET";

	/**
	 * 谷歌秘钥有序Set存储 ，score：查询次数 空闲
	 */
	private static String KEY_GOOGLE_KEY_FREE_PRE_FIX = "SHENSOU_GOOGLE_FREE_KEY_SET";
	/**
	 * 接受关键词查询队列
	 */
	private static final String KEY_NEW_KEYWORDS_LIST_PRE_FIX = "SHENSOU_NEW_KEYWORDS_LIST:";

	public RedisService() {
	}

	/**
	 * 保存关键词查询结果到数据库中
	 * 
	 * @param keywords
	 *            关键词
	 * @param size
	 *            关键词查询结果条数
	 */
	public void save2RedisShensouResultList(String keywords, int size) {
		Jedis jedis = JedisPoolFactory.getJedis();
		try {
			jedis.zadd(KEY_SHENSOU_RESULT_LIST_PRE_FIX, size, keywords);// 保存每个词的查询记录数
			jedis.zincrby(KEY_SHENSOU_RESULT_TOTAL_PRE_FIX, size,
					ALL_USER_TOTAL);// 保存汇总查询记录数
		} catch (Exception e) {
			log.error(e);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 查询所有已知关键词
	 * 
	 * @param keywords
	 *            关键词
	 * @param size
	 *            关键词查询结果条数
	 */
	public Set<String> getRedisShensouResultList(String keywords) {
		Jedis jedis = JedisPoolFactory.getJedis();
		try {
			return jedis.zrange(KEY_SHENSOU_RESULT_LIST_PRE_FIX, 0, -1);
		} catch (Exception e) {
			log.error(e);
		} finally {
			jedis.close();
		}
		return null;
	}

	/**
	 * 判断关键词是否已经存在数据库中
	 * 
	 * @return
	 */
	public boolean isExsitKeywords(String keywords) {
		Jedis jedis = JedisPoolFactory.getJedis();
		try {

			String key = KEY_RESULT_KEYWORDS_PRE_FIX + keywords;
			Long count = jedis.zcard(key);
			if (count != null && count > 0) {
				return true;
			}
		} catch (Exception e) {
			log.error(e);
		} finally {
			jedis.close();
		}
		return false;
	}

	/**
	 * 判断关键词是否已经存在数据库中
	 * 
	 * @return
	 */
	public List<String> getBlackList() {
		Jedis jedis = JedisPoolFactory.getJedis();
		try {
			String black = jedis.get(BLACK_LIST);
			String[] list = black.split(",");
			return Arrays.asList(list);
		} catch (Exception e) {
			log.error(e);
		} finally {
			jedis.close();
		}
		return new ArrayList<String>();
	}

	/**
	 * 获取队列中最新的关键词
	 * 
	 * @return
	 */
	public String getKeywordsByRedisList() {
		Jedis jedis = JedisPoolFactory.getJedis();
		try {
			String kewords = jedis.rpop(KEY_NEW_KEYWORDS_LIST_PRE_FIX);
			return kewords;
		} catch (Exception e) {
			log.error(e);
		} finally {
			jedis.close();
		}
		return null;
	}

	/**
	 * 保存关键词到最新队列里
	 * 
	 * @return
	 */
	public void saveKeywords2RedisList(String keywords) {
		Jedis jedis = JedisPoolFactory.getJedis();
		try {
			jedis.lpush(KEY_NEW_KEYWORDS_LIST_PRE_FIX, keywords);
		} catch (Exception e) {
			log.error(e);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 获取队列中最新的URL
	 * 
	 * @return
	 */
	public String getUrlByRedisList() {
		Jedis jedis = JedisPoolFactory.getJedis();
		try {
			String url = jedis.rpop(NEW_URL_LIST_PRE_FIX);
			return url;
		} catch (Exception e) {
			log.error(e);
		} finally {
			jedis.close();
		}
		return null;
	}

	public void saveResult2Redis(Jedis jedis, String keyword,
			ShensouResult result, int count) {
		// Jedis jedis = jedisPool.getResource();
		try {
			String key = KEY_RESULT_KEYWORDS_PRE_FIX + keyword;
			// Long count = jedis.zcard(key) + URL_RESULT_INIT_SCORE;
			// 保存网址到有序Set[SHENSOU_RESULT_KEYWORDS:关键词] 中
			jedis.zadd(key, count, result.getWebsite());
			// jedis.lpush(NEW_URL_LIST_PRE_FIX, result.getWebsite());

			Map<String, String> resultMap = new HashMap<String, String>();
			resultMap.put("query",
					result.getQuery() == null ? "" : result.getQuery());
			resultMap.put("website",
					result.getWebsite() == null ? "" : result.getWebsite());
			resultMap.put("title",
					result.getTitle() == null ? "" : result.getTitle());
			resultMap.put("desc",
					result.getDesc() == null ? "" : result.getDesc());
			resultMap.put("keywords", result.getKeywords() == null ? ""
					: result.getKeywords());
			resultMap.put("email", result.getEmail()== null ? ""
					: result.getEmail());
			String mkey = KEY_RESULT_KEYWORDS_PRE_FIX + keyword
					+ result.getWebsite();
			// 保存每个网址信息到Hash[SHENSOU_RESULT_KEYWORDS:关键词:url] 中
			jedis.hmset(mkey, resultMap);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			// jedis.close();
		}
	}

	public void saveWebsiteInfo2Redis(ShensouResult result, String md5key) {
		Jedis jedis = JedisPoolFactory.getJedis();
		if (result == null || result.getWebsite().isEmpty()
				|| result.getTitle().isEmpty()) {
			return;
		}
		try {

			String mkey = KEY_RESULT_KEYWORDS_PRE_FIX + md5key
					+ result.getWebsite();
			Map<String, String> resultMap = new HashMap<String, String>();
			if (!StringUtils.isEmpty(result.getTitle())) {
				resultMap.put("title", result.getTitle());
			}
			if (!StringUtils.isEmpty(result.getDesc())) {
				resultMap.put("desc", result.getDesc());
			}
			if (!StringUtils.isEmpty(result.getKeywords())) {
				resultMap.put("keywords", result.getKeywords());
			}
			if (!StringUtils.isEmpty(result.getWebsite())) {
				resultMap.put("website", result.getWebsite());
			}
			if (!StringUtils.isEmpty(result.getEmail())) {
				resultMap.put("email", result.getEmail());
			}
			// 保存每个网址信息到Hash[SHENSOU_RESULT_KEYWORDS:md5key.url] 中
			jedis.hmset(mkey, resultMap);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 保存google秘钥使用情况,每次递减
	 * 
	 * @param jedis
	 * 
	 * @param key
	 * @param searchCount
	 */
	public void save2RedisKeyUesd(String key, int searchCount) {
		Jedis jedis = JedisPoolFactory.getJedis();
		try {
			// 刷新使用次数
			Double count = jedis.zincrby(KEY_GOOGLE_KEY_USED_PRE_FIX,
					-searchCount, key);
			// 从使用队列中移除 FIXME 当前秘钥足够大
			// jedis.zrem(KEY_GOOGLE_KEY_USED_PRE_FIX, key);
			// 将新的剩余次数更新到空闲队列中 FIXME 当前秘钥足够大
			// jedis.zadd(KEY_GOOGLE_KEY_FREE_PRE_FIX, count, key);
		} catch (Exception e) {
			log.error(e);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 初始化谷歌秘钥
	 * 
	 * @param key
	 * @param count
	 */
	public void initGoogleKey(String key, int count) {
		Jedis jedis = JedisPoolFactory.getJedis();
		try {
			jedis.zadd(KEY_GOOGLE_KEY_FREE_PRE_FIX, count, key);
		} catch (Exception e) {
			log.error(e);
		} finally {
			jedis.close();
		}
	}

	/**
	 * 获取N个剩余次数最多的秘钥 ， -1获取所有秘钥
	 * 
	 * @param count
	 *            秘钥个数 -1所有
	 * @return
	 * 
	 */
	public List<String> getGoogleKey(int count) {
		Jedis jedis = JedisPoolFactory.getJedis();
		if (count == 0) {
			return null;
		}

		List<String> keyList = new ArrayList<String>();
		try {
			Set<Tuple> keysTuple = jedis.zrevrangeWithScores(
					KEY_GOOGLE_KEY_FREE_PRE_FIX, 0, count < 0 ? -1 : count - 1);
			Tuple key = null;
			if (keysTuple == null) {
				return null;
			}
			for (Iterator<Tuple> itr = keysTuple.iterator(); itr.hasNext();) {
				key = itr.next();
				// 从空闲队列中移除 FIXME 当前秘钥足够大，无需加锁控制
				// jedis.zrem(KEY_GOOGLE_KEY_FREE_PRE_FIX, key.getElement());
				// 添加到使用队列中 FIXME 当前秘钥足够大，无需加锁控制
				// jedis.zadd(KEY_GOOGLE_KEY_USED_PRE_FIX,
				// key.getScore(),key.getElement());
				keyList.add(key.getElement());
			}
		} catch (Exception e) {
			log.error(e);
		} finally {
			jedis.close();
		}
		return keyList;
	}

}
