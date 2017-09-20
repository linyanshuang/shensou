package com.waimaoshensou.google.search;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JsoupDownloader implements Download {

	private String org_url = "";
	private String url = "";
	private String md5key = "";
	private String regex = "[a-zA-Z0-9_-]+@\\w+\\.[a-z]+(\\.[a-z]+)?";
	private final Log logger = LogFactory.getLog(JsoupDownloader.class);
	private RedisService redisService = new RedisService();

	private static final int SOCKETT_IMEOUT = Integer.parseInt(MyProperty
			.getKey("download.sockect.timeout"));;

	public JsoupDownloader(String url, String md5key) {
		this.org_url = url;
		this.md5key = md5key;
		this.url = (url.startsWith("http://") || url.startsWith("https://")) ? url
				: "http://" + url;
	}

	@Override
	public void run() {
		try {
			Document dom = Jsoup.connect(url).followRedirects(true)
					.timeout(SOCKETT_IMEOUT).get();
			Elements titles = dom.getElementsByTag("title");
			// 获取首页邮箱
			String idxEmail = getEmail(dom.text()).toString();
			StringBuffer email = new StringBuffer();
			// 联系方式页面邮箱
			Elements links = dom.select("a:contains(contact)");
			String contactUrl = links.attr("abs:href");
			if (contactUrl.contains("http")) {
				try {
					Document domContact = Jsoup.connect(contactUrl)
							.followRedirects(true).timeout(SOCKETT_IMEOUT)
							.get();
					email.append(getEmail(domContact.text()));
				} catch (IOException e) {
				}
			}
			if (!email.toString().contains(idxEmail.toLowerCase())) {
				email.append(idxEmail);
			}
			logger.info(email);
			if ((titles != null && titles.size() > 0)
					|| (email != null && email.length() > 0)) {
				ShensouResult result = new ShensouResult();
				result.setWebsite(org_url);
				result.setTitle(titles.get(0).text());
				result.setEmail(email.toString());

				Elements metas = dom.getElementsByTag("meta");
				if (titles != null && titles.size() > 0) {
					Map<String, String> metaTags = new HashMap<String, String>();
					for (Element mete : metas) {
						metaTags.put(mete.attr("name"), mete.attr("content"));
					}
					result.setMetaTags(metaTags);
					logger.info(org_url + " Keywords: " + result.getKeywords());
					save(result);
				}
			}

		} catch (Exception e) {
			logger.error(url, e);
		}
	}

	private StringBuffer getEmail(String text) {
		StringBuffer email = new StringBuffer();
		Pattern p = Pattern.compile(regex);
		String[] texts = text.split(" ");
		for (String line : texts) {
			Matcher m = p.matcher(line);
			if (m.find()) {
				String em = m.group().toLowerCase();
				if (!email.toString().contains(em)) {
					email.append(em);
					email.append(" ");
				}
			}
		}
		logger.info(email);
		return email;
	}

	private void save(ShensouResult result) {
		redisService.saveWebsiteInfo2Redis(result, md5key);
	}

	public static void main(String[] args) {
		JsoupDownloader a = new JsoupDownloader("http://www.kingstown.co.uk/",
				"");
		a.run();
	}
}
