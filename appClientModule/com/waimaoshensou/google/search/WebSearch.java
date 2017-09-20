package com.waimaoshensou.google.search;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.alibaba.fastjson.JSON;
import com.waimaoshensou.google.Result;
import com.waimaoshensou.google.Utils;

public class WebSearch implements Callable<Result> {
	private final String GOOGLE_TRAFFIC = "but your computer or network may be sending automated queries";
	private final String Unauthorized = "Unauthorized access to internal API";
	private final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.94 Safari/537.36";
	private static final int SOCKECT_TIMEOUT = Integer.parseInt(MyProperty
			.getKey("web.search.sockect.timeout"));
	private static Log log = LogFactory.getLog(WebSearch.class);
	private static String cse_tok = null;
	private String query;
	private int page;
	private String tokenURL = MyProperty.getKey("web.search.gettoken.url");
	private final String URL = MyProperty
			.getKey("web.search.url");

	WebSearch(String query, int page) {
		this.query = query;
		this.page = page;
		if(cse_tok == null){
			refreshtoken();
		}
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 1; i++) {
			//Executors.newFixedThreadPool(1).submit(new WebSearch("mp3", 1));
			new WebSearch("mp3", 1).call();
			System.out.println(i);
			Thread.sleep(100);
		}
	}

	/**
	 * 
	 * @param keywords
	 *            关键词
	 * @param page
	 *            默认是0
	 * @return
	 */
	private Result getResult(String query, int page) {
		Document dom;
		Result result = null;
		try {
			dom = Jsoup.connect(URL).timeout(SOCKECT_TIMEOUT)
					.ignoreContentType(true).userAgent(USER_AGENT)
					.data("start", String.valueOf(page * 20))
					.data("cse_tok",cse_tok)
					.data("userIp", Utils.getRandomIp()).data("q", query)
					.ignoreHttpErrors(true).data("prettyPrint", "false").get();
			if (dom.text().contains(GOOGLE_TRAFFIC)) {// 谷歌限流
				log.warn(GOOGLE_TRAFFIC);
			}else if (dom.text().contains(Unauthorized)) {// 谷歌403
				log.warn(Unauthorized);
				log.info("refresh token and recall getResult method");
				refreshtoken();//refresh token
				return getResult(query, page);
			}else {
				result = JSON.parseObject(dom.text(), Result.class);
				result.setQuery(query);
			}
		} catch (Exception e) {
			log.error(e);
		}
		return result;
	}
	
	private void refreshtoken(){
		Document dom = null;
		try {
			log.info("refresh token...");
			dom = (Document) Jsoup.connect(tokenURL).timeout(SOCKECT_TIMEOUT).get();
			if(dom.text().contains("\"cse_token\":\"")){
				cse_tok = dom.text().split("\"cse_token\":\"")[1].split("\"")[0];
				log.info("refresh token ok,cse_tok = "+cse_tok);
			}
		} catch (Exception e) {
			log.error(e);
		}
	}

	@Override
	public Result call() throws Exception {
		return getResult(query, page);
	}
}