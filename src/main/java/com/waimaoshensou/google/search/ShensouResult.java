package com.waimaoshensou.google.search;

import java.io.Serializable;
import java.util.Map;

public class ShensouResult implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private Map<String, String> metaTags;
	/**
	 * 查询条件
	 */
	private String query;
	/**
	 * 网站首页
	 */
	private String website;
	/**
	 * 网站标题
	 */
	private String title;
	/**
	 * 网站关键词
	 */
	private String keywords;
	/**
	 * 网站描述
	 */
	private String desc;
	
	private String email;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getKeywords() {
		if (metaTags != null) {
			keywords = metaTags.get("keywords");
			if(keywords == null){
				keywords = "";
			}
		}
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getDesc() {
		if (metaTags != null) {
			desc = metaTags.get("description");
			if(desc == null){
				desc="";
			}
		}
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Override
	public boolean equals(Object obj) {
		return website.equals(((ShensouResult) obj).getWebsite());
	}

	@Override
	public String toString() {
		return website;
	}

	public Map<String, String> getMetaTags() {
		return metaTags;
	}

	public void setMetaTags(Map<String, String> metaTags) {
		this.metaTags = metaTags;
	}
}
