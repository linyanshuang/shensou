package com.waimaoshensou.google.search;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WebsiteInfomation {
	private static final Log logger = LogFactory
			.getLog(WebsiteInfomation.class);
	private static RedisService redisService = new RedisService();

	// 线程池的容量
	private static final int POOL_SIZE = Integer.parseInt(MyProperty
			.getKey("download.pool.size"));

	// 线程睡眠时间
	private static final int SLEEP_SECOND = Integer.parseInt(MyProperty
			.getKey("download.pool.sleep.millisecond"));

	private static final int DOWNLOAD_MODE = Integer.parseInt(MyProperty
			.getKey("download.mode"));

	// 线程池
	private static ExecutorService exe = null;

	public static void main(String[] args) {
		logger.info("下载器初始化成功");
		while (true) {
			try {
				exe = Executors.newFixedThreadPool(POOL_SIZE);
				String url = redisService.getUrlByRedisList();
				if (url != null && !url.isEmpty()) {
					String[] urls = url.split(",");
					if (urls.length < 2) {
						continue;
					}
					url = urls[0];
					String md5key = urls[1];
					Download download = null;
					if (DOWNLOAD_MODE == 0) {// TODO
						// download = new Downloader(url);
					} else if (DOWNLOAD_MODE == 1) {
						download = new JsoupDownloader(url, md5key);
					}
					exe.submit(download);
				}
				Thread.sleep(SLEEP_SECOND);
			} catch (Exception e) {
				logger.error(e);
			}
		}

	}
}
