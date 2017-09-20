package com.waimaoshensou.google.search;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MyProperty {

	private static Properties prop;

	private static Log log = LogFactory.getLog(MyProperty.class);

	private static String DEFAULT_VALUE = "";

	private static String FILE = "/shensou.properties";

	public static void main(String[] args) {
		System.out.println(getKey("search.reg", "cn", "com"));
	}

	public static String getKey(String key) {
		getProperty();
		return prop.getProperty(key, DEFAULT_VALUE);
	}

	public static String getKey(String key, String... strs) {
		String val = getKey(key);
		for (int i = 0; i < strs.length; i++) {
			String reg = "{" + i + "}";
			val = val.replace(reg, strs[i]);
		}
		return val;
	}

	public static void getProperty() {
		if (prop != null) {
			return;
		}
		try {
			Properties _prop = new Properties();
			_prop.load(MyProperty.class.getResourceAsStream(FILE));
			prop = _prop;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

}