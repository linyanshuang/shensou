package com.waimaoshensou.google.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.fetcher.PageFetchResult;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.parser.ParseData;
import edu.uci.ics.crawler4j.parser.Parser;
import edu.uci.ics.crawler4j.url.WebURL;

/**
 * This class is a demonstration of how crawler4j can be used to download a
 * single page and extract its title and text.
 */
public class Downloader implements Download {
	// private static final Logger logger =
	// LoggerFactory.getLogger(Downloader.class);
	private static final Log logger = LogFactory.getLog(Downloader.class);
	private Parser parser;
	private PageFetcher pageFetcher;
	private String url;
	private String org_url;

	private RedisService redisService = new RedisService();

	public Downloader(String url) {
		this.org_url = url;
		this.url = (url.startsWith("http://") || url.startsWith("https://")) ? url
				: "http://" + url;
		this.parser = PageFetcherFactory.getParser();
		this.pageFetcher = PageFetcherFactory.getPageFetcher();
	}

	private void processUrl(String url) {
		ShensouResult result = null;
		// logger.info("Processing: {}" + url);
		Page page = download(url);
		if (page != null) {
			ParseData parseData = page.getParseData();
			if (parseData != null && parseData instanceof HtmlParseData) {
				result = new ShensouResult();
				HtmlParseData htmlParseData = (HtmlParseData) parseData;
				result.setWebsite(this.org_url);
				result.setTitle(htmlParseData.getTitle());
				result.setMetaTags(htmlParseData.getMetaTags());
				save(result);
				logger.info(this.url + "=" + htmlParseData.getTitle());
			}
		} else {
			// logger.warn("Couldn't fetch the content of the page.");
		}
		// logger.info("==============");
	}

	private void save(ShensouResult result) {
		redisService.saveWebsiteInfo2Redis(result,"");
	}

	private Page download(String url) {
		WebURL curURL = new WebURL();
		curURL.setURL(url);
		curURL.setDepth((short) 0);
		PageFetchResult fetchResult = null;
		try {
			fetchResult = pageFetcher.fetchPage(curURL);
			if (fetchResult.getStatusCode() == HttpStatus.SC_OK) {
				Page page = new Page(curURL);
				fetchResult.fetchContent(page);
				parser.parse(page, curURL.getURL());
				return page;
			} else if (fetchResult.getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY) {
				processUrl(fetchResult.getMovedToUrl());
			} else if (fetchResult.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
				processUrl(fetchResult.getMovedToUrl());
			}
		} catch (Exception e) {
			logger.error(
					"Error occurred while fetching url: " + curURL.getURL()
							+ e.getMessage(), e);
		} finally {
			if (fetchResult != null) {
				fetchResult.discardContentIfNotConsumed();
			}
		}
		return null;
	}

	@Override
	public void run() {
		processUrl(url);
	}
}
