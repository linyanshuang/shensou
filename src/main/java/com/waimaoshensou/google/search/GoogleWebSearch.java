package com.waimaoshensou.google.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;

import com.waimaoshensou.google.MD5Util;
import com.waimaoshensou.google.Result;

public class GoogleWebSearch implements Runnable {
	private static Log log = LogFactory.getLog(GoogleWebSearch.class);
	private static String SPLIT_COMMA = ",";

	// 线程池的容量
	private static final int POOL_SIZE = Integer.parseInt(MyProperty
			.getKey("web.search.pool.size"));
	private static final String REG_KEY = "search.reg";
	private static final long RELATED_LIMIT = Long.parseLong(MyProperty
			.getKey("related.limit"));// 1000;
	private static final long LIST_COUNT_MAX = Long.parseLong(MyProperty
			.getKey("list.count.max"));// 3000;
	private static final long WEB_SEARCH_SLEEP = Long.parseLong(MyProperty
			.getKey("web.search.sleep"));// 3000;

	private String keywords;
	private String country;
	private int count = 0;
	private ExecutorService exe;
	private List<String> regs;
	private RedisService redisService = new RedisService();
	private Jedis jedis = JedisPoolFactory.getJedis();
	private List<String> blackList;

	GoogleWebSearch(String country, String keywords, List<String> regs,
			List<String> blackList) {
		this.country = country;
		this.keywords = keywords;
		if (keywords.contains(" ")) {// 如果包含多词用双引号精准查询
			this.regs = new ArrayList<String>(regs);
			String first = "\"" + keywords + "\" site:" + country;
			this.regs.add(0, first);

		} else {
			this.regs = regs;
		}
		this.exe = Executors.newFixedThreadPool(POOL_SIZE);
		this.blackList = blackList;

	}

	@Override
	public void run() {
		search();
		regs = null;
	}

	private synchronized void search() {
		List<ShensouResult> resultList = new ArrayList<ShensouResult>();
		List<ShensouResult> list = getResultListByRegs(keywords, regs,
				resultList);
		// 删除重复项
		list = removeDuplicate(list);
		log.info("关键词[" + keywords + "]常规结果:" + list.size());
		List<ShensouResult> rrlist = relatedResultSiteList(list);
		if (list.size() < RELATED_LIMIT) { // related 结果倍增
			rrlist = removeDuplicate(rrlist);
			log.info("关键词[" + keywords + "]related 倍增结果:" + rrlist.size());
		} else {
			log.info("关键词[" + keywords + "]超过最小related个数[" + RELATED_LIMIT
					+ "]限制，结束");
		}
		if (rrlist.size() > 0) {
			redisService.save2RedisShensouResultList(
					MD5Util.MD5(keywords + ":" + country), rrlist.size());
		}
		jedis.close();
		list = null;
		resultList = null;
		rrlist = null;

	}

	private List<ShensouResult> getResultListByRegs(String keywords,
			List<String> regs, List<ShensouResult> resultList) {
		for (String reg : regs) {
			if (resultList.size() > LIST_COUNT_MAX) {
				log.info("达到最大查询条数=============结束=============");
				log.info("达到最大查询条数=============结束=============");
				log.info("达到最大查询条数=============结束=============");
				log.info("达到最大查询条数=============结束=============");
				log.info("达到最大查询条数=============结束=============");
				break;
			}
			String newKeywords = "";
			if (reg.contains("\"" + keywords + "\"")) {
				newKeywords = reg;
			} else {
				newKeywords = keywords + " " + reg;
			}
			List<Result> results = new ArrayList<Result>();
			List<Future<Result>> FutureList = new ArrayList<Future<Result>>();
			// 查询第一页内容
			log.info("start search query:" + newKeywords);
			WebSearch search = new WebSearch(newKeywords, 0);
			Future<Result> future = exe.submit(search);
			Result result;
			try {
				result = future.get();
				if (result == null) {
					continue;
				}
				results.add(result);
				save2redis(result);
				if (result.getCursor() == null
						|| result.getCursor().getPages() == null) {
					continue;
				}

				int pageSize = result.getCursor().getPages().size();
				// 查询第2-N页(n <= 5)
				for (int i = 1; i < pageSize; i++) {
					Future<Result> f = exe
							.submit(new WebSearch(newKeywords, i));
					FutureList.add(f);
					Thread.sleep(WEB_SEARCH_SLEEP);
				}
			} catch (InterruptedException e) {
				log.error(e.getMessage());
			} catch (ExecutionException e) {
				log.error(e.getMessage());
			}
			for (Future<Result> f : FutureList) {
				try {
					Result re = f.get();
					if (re == null) {
						continue;
					}
					results.add(re);
					save2redis(re);
				} catch (InterruptedException e) {
					log.error(e.getMessage());
				} catch (ExecutionException e) {
					log.error(e.getMessage());
				}
			}
			List<ShensouResult> ss = changeResultList2ShensouResultList(results);
			results = null;
			resultList.addAll(ss);
		}
		return resultList;
	}

	private static List<ShensouResult> changeResultList2ShensouResultList(
			List<Result> list) {
		// 构造神搜结果
		List<ShensouResult> ss = new ArrayList<ShensouResult>();
		for (Result r1 : list) {
			if (r1 == null) {
				continue;
			}
			for (Result.Results r2 : r1.getResults()) {
				ShensouResult e = new ShensouResult();
				e.setDesc(r2.getContentNoFormatting());
				e.setTitle(r2.getTitleNoFormatting());
				e.setWebsite(r2.getVisibleUrl());
				ss.add(e);
			}
		}
		return ss;
	}

	public static void main(String[] args) throws InterruptedException,
			IOException, ExecutionException {
		String[] regs = MyProperty.getKey(REG_KEY, "uk").split(SPLIT_COMMA);
		String keywords = "cat scratching post";
		GoogleWebSearch googSearch = new GoogleWebSearch("uk", keywords,
				Arrays.asList(regs), new ArrayList<String>());
		Executors.newFixedThreadPool(1).submit(googSearch);
	}

	/**
	 * related 结果倍增
	 * 
	 * @param list
	 * @return
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private List<ShensouResult> relatedResultSiteList(List<ShensouResult> list) {
		List<String> regs = new ArrayList<String>();
		for (ShensouResult ss : list) {
			regs.add("related:" + ss.getWebsite());
		}
		List<ShensouResult> listResult = getResultListByRegs("", regs, list);
		regs = null;
		return listResult;
	}

	/**
	 * 删除重复项
	 * 
	 * @param list
	 * @return
	 */
	private List<ShensouResult> removeDuplicate(List<ShensouResult> resultList) {
		Set<String> set = new HashSet<String>();
		List<ShensouResult> listResultList = new ArrayList<ShensouResult>();
		int duplicate = 0;
		int black = 0;
		for (ShensouResult result : resultList) {
			String link = result.getWebsite();
			if (isBlack(link)) {
				black++;
				continue;
			}
			if (set.add(link.trim())) {
				listResultList.add(result);
			} else {
				duplicate++;
			}

		}
		if (duplicate > 0) {
			log.info("删除重复项:" + duplicate + "条记录,黑名单：" + black + "条记录");
		} else {
			log.info("无重复项");
		}
		return listResultList;
	}

	private boolean isBlack(String url) {
		for (String black : blackList) {
			if (url.contains(black)) {
				return true;
			}
		}
		return false;
	}

	private void save2redis(Result result) {
		for (Result.Results r : result.getResults()) {
			// 非空以及黑名单判断
			if ("".equals(r.getVisibleUrl()) || null == r.getVisibleUrl()
					|| isBlack(r.getVisibleUrl())) {
				continue;
			}
			ShensouResult ssresult = new ShensouResult();
			ssresult.setWebsite(r.getVisibleUrl());
			ssresult.setQuery(result.getQuery());
			ssresult.setDesc(r.getContentNoFormatting());
			ssresult.setTitle(r.getTitleNoFormatting());

			redisService.saveResult2Redis(jedis,
					MD5Util.MD5(keywords + ":" + country), ssresult, count++);
		}
	}
}
