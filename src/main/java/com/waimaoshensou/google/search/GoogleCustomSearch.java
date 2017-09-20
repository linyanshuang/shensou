package com.waimaoshensou.google.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;

import com.google.api.client.googleapis.batch.BatchCallback;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.json.GoogleJsonErrorContainer;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.model.Query;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;
import com.waimaoshensou.google.Utils;

public class GoogleCustomSearch implements Runnable {
	// private static String API_KEY =
	// "AIzaSyATcsLuaw0OwwxxyHeyTunemrXcXYsmGos";//lovewenshu1 付费账户
	// private String key = "AIzaSyCX8vRsVmQx5TpMyBJpR5M6iOWQen9KWlY";// goog1
	private String key = "AIzaSyCVAXiUzRYsML1Pv6RwSG1gunmMikTzQqY";
	private final String CSE_ID = "012080660999116631289:zlpj9ypbnii";
	// private final String CSE_ID = "004024656803731530703:3fzyfrgq8um";
	private final String Daily_Limit_Exceeded = "Daily Limit Exceeded.";
	private List<String> keyList = new ArrayList<String>();

	private Log log = LogFactory.getLog(GoogleCustomSearch.class);

	private final long NUM = 10;

	private final long COUNT_100 = 100;// 100;
	private final long RELATED_LIMIT = Long.parseLong(MyProperty
			.getKey("related.limit"));// 1000;
	private final long LIST_COUNT_MAX = Long.parseLong(MyProperty
			.getKey("list.count.max"));// 5000;

	// 线程池的容量
	private final int POOL_SIZE = Integer.parseInt(MyProperty
			.getKey("download.pool.size"));

	// 线程池
	private ExecutorService exe = null;

	private static Customsearch customSearch = new Customsearch(
			new NetHttpTransport(), new JacksonFactory(), null);

	private static int changeKeyIdx = 0;

	private int searchCount = 0;

	private static boolean changeNextKey = false;

	private List<Result> resultList;

	private Map<String, Integer> regSearchResultCount = new HashMap<String, Integer>();

	private boolean isRuning = true;

	private Set<String> set = new HashSet<String>();

	private RedisService redisService = new RedisService();

	private Jedis jedis = JedisPoolFactory.getJedis();;
	// 规则集合
	private String[] regs;

	private String country;

	@Override
	public void run() {
		// 清空结果集
		exe = Executors.newFixedThreadPool(POOL_SIZE);
		resultList = new ArrayList<Result>();
		search();
	}

	/**
	 * 构造函数
	 * 
	 * @param keyword
	 */
	GoogleCustomSearch(String country, String keyword, List<String> keys,
			String[] regs) {
		this.country = country;
		this.keywords = keyword;
		this.keyList = keys;
		this.key = keys.get(0);
		this.regs = regs;
	}

	private void search() {
		checkKey();
		// 获取常规规则结果
		nomalResultList(keywords, regs);
		// 删除重复项
		List<String> sites = removeDuplicate();
		log.info("关键词[" + keywords + "]常规结果:" + sites.size());
		if (sites.size() < RELATED_LIMIT) { // related 结果倍增
			relatedResultSiteList(sites);
			removeDuplicate();
			log.info("关键词[" + keywords + "]related 倍增结果:" + resultList.size());
		} else {
			log.info("关键词[" + keywords + "]超过最小related个数[" + RELATED_LIMIT
					+ "]限制，结束");
		}
		// 查询结束
		isRuning = false;
		if (resultList.size() > 0) {
			redisService.save2RedisShensouResultList(
					keywords.replaceAll(" ", "+") + ":" + country,
					resultList.size());
			redisService.save2RedisKeyUesd(key, searchCount);
		}
		jedis.close();

		set.clear();
		set = null;
	}

	/**
	 * 删除重复项
	 * 
	 * @param list
	 * @return
	 */
	private List<String> removeDuplicate() {
		Set<String> set = new HashSet<String>();
		List<String> newList = new ArrayList<String>();
		List<Result> listResultList = new ArrayList<Result>();
		int duplicate = 0;
		for (Iterator<Result> iter = resultList.iterator(); iter.hasNext();) {
			Result result = (Result) iter.next();
			String link = result.getDisplayLink();
			if (set.add(link.trim())) {
				newList.add(link.trim());
				listResultList.add(result);
			} else {
				duplicate++;
			}
		}
		if (duplicate > 0) {
			log.info("删除重复项:" + duplicate + "条记录");
			// 覆盖原来重复的列表
			resultList = listResultList;
		} else {
			log.info("无重复项");
		}
		return newList;
	}

	/**
	 * 获取所有关键词结果
	 * 
	 * @param keyword
	 * @return
	 */
	private void nomalResultList(String keyword, String[] regs) {
		List<String> keywords = new ArrayList<String>();
		for (String reg : regs) {
			keywords.add(keyword + " " + reg.trim());
		}
		// 获取所有规则的首页结果
		log.info("=============开始 nomalResultList  getAllRegFirstPageResultList=============");
		List<Search> firstSearchList = getAllRegFirstPageResultList(keywords);
		log.info("=============结束 nomalResultList getAllRegFirstPageResultList=============");
		// 获取所有规则的剩余页结果
		log.info("=============开始 nomalResultList AllRegKeywords=============");
		AllRegKeywords(firstSearchList);
		log.info("=============结束 nomalResultList AllRegKeywords=============");
	}

	/**
	 * related 结果倍增
	 * 
	 * @param list
	 * @return
	 */
	private void relatedResultSiteList(List<String> sites) {
		List<String> keywords = new ArrayList<String>();
		for (String site : sites) {
			keywords.add("related:" + site);
		}
		// 获取所有规则的首页结果
		log.info("=============开始 relatedResultSiteList  getAllRegFirstPageResultList=============");
		List<Search> firstSearchList = getAllRegFirstPageResultList(keywords);
		log.info("=============结束 relatedResultSiteList  getAllRegFirstPageResultList=============");
		// 获取所有规则的剩余页结果
		log.info("=============开始 relatedResultSiteList  AllRegKeywords=============");
		AllRegKeywords(firstSearchList);
		log.info("=============结束relatedResultSiteList  AllRegKeywords=============");
	}

	/**
	 * 获取网站信息
	 * 
	 * @param site
	 * @param query
	 * @return
	 */
	private void getWebInfo(String site) {
		if (!set.add(site)) {
			return;
		}
		try {// TODO XX
				// Downloader downloader = new Downloader(site);
				// exe.submit(downloader);
		} catch (Exception e) {
		}
	}

	/**
	 * 批量获取网站信息
	 * 
	 * @param list
	 * @param query
	 * @return
	 */
	private void getWebInfos(List<Result> list, String query) {
		for (Result re : list) {

			// 先保存URL
			ShensouResult ssresult = new ShensouResult();
			ssresult.setWebsite(re.getDisplayLink());
			ssresult.setQuery(query);
			// TODO
			// redisService.saveResult2Redis(jedis, keywords.replaceAll(" ",
			// "+") + ":" + country,
			// ssresult);

			// 暂时不获取网站详细信息 TODO
			// getWebInfo(re.getDisplayLink());
		}

	}

	/**
	 * 首先查询所有规则的的首页结果
	 * 
	 * @param keyword
	 * @return
	 */
	private List<Search> getAllRegFirstPageResultList(List<String> keywords) {
		final List<Search> searchList = new ArrayList<Search>();
		try {
			BatchRequest batch = customSearch.batch();
			for (String keyword : keywords) {
				// 超过最大限制条数，退出查询
				if (resultList.size() > LIST_COUNT_MAX) {
					log.info("达到最大查询条数=============结束=============");
					log.info("达到最大查询条数=============结束=============");
					log.info("达到最大查询条数=============结束=============");
					log.info("达到最大查询条数=============结束=============");
					log.info("达到最大查询条数=============结束=============");
					break;
				}
				Customsearch.Cse.List list = customSearch.cse().list(keyword);
				list.setKey(key);
				list.setCx(CSE_ID);
				list.setNum(NUM);
				list.setStart((long) 1);
				list.setSafe("high");
				list.buildHttpRequest();
				list.setUserIp(Utils.getRandomIp());// 不限制用户请求数
				batch.queue(list.buildHttpRequest(), Search.class,
						GoogleJsonErrorContainer.class,
						new BatchCallback<Search, GoogleJsonErrorContainer>() {
							public void onSuccess(Search search,
									HttpHeaders responseHeaders) {
								if (search != null && search.getItems() != null
										&& search.getItems().size() > 0) {
									String query = search.getQueries()
											.get("request").get(0)
											.getSearchTerms();
									getWebInfos(search.getItems(), query);
									resultList.addAll(search.getItems());
									searchList.add(search);
									calcRegQueryCount(search);
									searchCount++;
									printSearchinfo(search);
								}
							}

							public void onFailure(GoogleJsonErrorContainer e,
									HttpHeaders responseHeaders) {
								// 当日查询量不足
								if (e.getError().getMessage()
										.contains(Daily_Limit_Exceeded)) {
									changeNextKey();
									throw new RuntimeException(
											Daily_Limit_Exceeded);
								} else {
									log.error(e.getError().getMessage());
								}
								// searchCount++;
							}
						});
			}
			if (batch.size() > 0) {
				batch.execute();
			}

		} catch (Exception e) {
			if (e.getMessage() != null
					&& e.getMessage().contains(Daily_Limit_Exceeded)) {
				// 查询结束
				isRuning = false;
				// search();
			} else {
				log.error(e.getMessage());
			}
		}
		return searchList;
	}

	/**
	 * 计算每个规则获取的记录数
	 * 
	 * @param serach
	 */
	private void calcRegQueryCount(Search serach) {
		String query = serach.getQueries().get("request").get(0)
				.getSearchTerms();
		if (regSearchResultCount.containsKey(query)) {
			regSearchResultCount.put(query, regSearchResultCount.get(query)
					+ serach.getItems().size());
		} else {
			regSearchResultCount.put(query, serach.getItems().size());
		}

	}

	private void printSearchinfo(Search search) {
		String query = search.getQueries().get("request").get(0)
				.getSearchTerms();
		log.info("keyword:" + query + " added:" + search.getItems().size());
	}

	protected void changeNextKey() {
		changeNextKey = true;
		log.info("Daily Limit Exceeded.");
	}

	private void checkKey() {
		if (changeNextKey) {
			changeKeyIdx++;
			key = keyList.get(changeKeyIdx % keyList.size());
			log.info("changed Next Key=" + key);
		}
		changeNextKey = false;
	}

	/**
	 * 
	 * @param searchList
	 * @return
	 */
	private void AllRegKeywords(List<Search> searchList) {
		Customsearch customsearch = new Customsearch(new NetHttpTransport(),
				new JacksonFactory(), null);
		try {
			// 遍历所有规则的首页查询结果
			for (Search search : searchList) {
				// 超过最大限制条数，退出查询
				if (resultList.size() > LIST_COUNT_MAX) {
					log.info("达到最大查询条数=============结束=============");
					log.info("达到最大查询条数=============结束=============");
					log.info("达到最大查询条数=============结束=============");
					log.info("达到最大查询条数=============结束=============");
					log.info("达到最大查询条数=============结束=============");
					break;
				}
				if (search == null || search.getItems() == null
						|| search.getItems().size() == 0) {
					continue;
				}
				Long totalResult = search.getSearchInformation()
						.getTotalResults();
				Query query = search.getQueries().get("request").get(0);
				getWebInfos(search.getItems(), query.getSearchTerms());
				if (totalResult > NUM) {
					BatchRequest batch = customsearch.batch();
					for (long start = 1 + NUM; start <= totalResult; start += NUM) {
						Customsearch.Cse.List list = customsearch.cse().list(
								query.getSearchTerms());
						list.setKey(key);
						list.setCx(CSE_ID);
						list.setNum(NUM);
						list.setStart(start);
						list.buildHttpRequest();
						list.setSafe("high");
						list.setUserIp(Utils.getRandomIp());
						if (start > COUNT_100) {
							break;
						}
						batch.queue(
								list.buildHttpRequest(),
								Search.class,
								GoogleJsonErrorContainer.class,
								new BatchCallback<Search, GoogleJsonErrorContainer>() {
									public void onSuccess(Search search,
											HttpHeaders responseHeaders) {
										if (search != null
												&& search.getItems() != null
												&& search.getItems().size() > 0) {
											String query = search.getQueries()
													.get("request").get(0)
													.getSearchTerms();
											getWebInfos(search.getItems(),
													query);
											resultList.addAll(search.getItems());
											calcRegQueryCount(search);
											searchCount++;
											printSearchinfo(search);
										}
									}

									public void onFailure(
											GoogleJsonErrorContainer e,
											HttpHeaders responseHeaders) {
										// 当日查询量不足
										if (e.getError().getMessage()
												.contains(Daily_Limit_Exceeded)) {
											changeNextKey();
											throw new RuntimeException(
													Daily_Limit_Exceeded);
											// 查询结束
										} else {
											log.error(e.getError().getMessage());
										}
										// searchCount++;
									}
								});
					}
					if (batch.size() > 0) {
						batch.execute();
					}
				}
			}
		} catch (Exception e) {
			if (e.getMessage().contains(Daily_Limit_Exceeded)) {
				isRuning = false;
				// search();
			} else {
				log.error(e.getMessage());
			}
		}
	}

	private String keywords = "";

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public List<String> getKeyList() {
		return keyList;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public boolean isRuning() {
		return isRuning;
	}

	public int getSearchCount() {
		return searchCount;
	}

	public void setSearchCount(int searchCount) {
		this.searchCount = searchCount;
	}

	public Map<String, Integer> getRegSearchResultCount() {
		return regSearchResultCount;
	}

	public void setRegSearchResultCount(
			Map<String, Integer> regSearchResultCount) {
		this.regSearchResultCount = regSearchResultCount;
	}

	public List<Result> getResultList() {
		return resultList;
	}

	public void setResultList(List<Result> resultList) {
		this.resultList = resultList;
	}

}