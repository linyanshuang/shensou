//package com.waimaoshensou.google.search;
//
//import edu.uci.ics.crawler4j.crawler.CrawlConfig;
//import edu.uci.ics.crawler4j.fetcher.PageFetcher;
//import edu.uci.ics.crawler4j.parser.Parser;
//
//public class PageFetcherFactory {
//	private static final int CONNECTION_TIMEOUT = 5000;
//	private static final int SOCKETT_IMEOUT = 5000;
//
//	private static Parser parser;
//	private static PageFetcher pageFetcher;
//
//	private PageFetcherFactory() {
//
//	}
//
//	public static Parser getParser() {
//		if (parser == null) {
//			CrawlConfig config = null;
//			config = new CrawlConfig();
//			config.setMaxDepthOfCrawling(1);
//			config.setPolitenessDelay(0);
//			config.setConnectionTimeout(CONNECTION_TIMEOUT);
//			config.setSocketTimeout(SOCKETT_IMEOUT);
//			parser = new Parser(config);
//		}
//		return parser;
//	}
//
//	public static PageFetcher getPageFetcher() {
//		if (pageFetcher == null) {
//			CrawlConfig config = null;
//			config = new CrawlConfig();
//			config.setMaxDepthOfCrawling(1);
//			config.setPolitenessDelay(0);
//			config.setConnectionTimeout(CONNECTION_TIMEOUT);
//			config.setSocketTimeout(SOCKETT_IMEOUT);
//			pageFetcher = new PageFetcher(config);
//		}
//		return pageFetcher;
//	}
//
//}
