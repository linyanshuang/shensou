package com.waimaoshensou.google.search;

import java.util.concurrent.ExecutionException;

public class Main {

	public static void main(String[] args) throws InterruptedException,
			ExecutionException {
		String pix = args != null && args.length > 0 ? args[0] : "search";
		if ("download".equals(pix)) {
			WebsiteInfomation.main(args);
		} else {
			GoogleSearch.main(args);
		}

	}
} 	
