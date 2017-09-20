package com.waimaoshensou.google;

import java.util.List;

public class Result {
	public Cursor cursor;
	public String query;
	public List<Results> results;

	public Cursor getCursor() {
		return cursor;
	}

	public void setCursor(Cursor cursor) {
		this.cursor = cursor;
	}

	public List<Results> getResults() {
		return results;
	}

	public void setResults(List<Results> results) {
		this.results = results;
	}

	public final class Cursor {
		String currentPageIndex;
		String estimatedResultCount;
		String moreResultsUrl;
		String resultCount;
		boolean isExactTotalResults;
		List<String> pages;

		public String getCurrentPageIndex() {
			return currentPageIndex;
		}

		public void setCurrentPageIndex(String currentPageIndex) {
			this.currentPageIndex = currentPageIndex;
		}

		public String getEstimatedResultCount() {
			return estimatedResultCount;
		}

		public void setEstimatedResultCount(String estimatedResultCount) {
			this.estimatedResultCount = estimatedResultCount;
		}

		public String getMoreResultsUrl() {
			return moreResultsUrl;
		}

		public void setMoreResultsUrl(String moreResultsUrl) {
			this.moreResultsUrl = moreResultsUrl;
		}

		public String getResultCount() {
			return resultCount;
		}

		public void setResultCount(String resultCount) {
			this.resultCount = resultCount;
		}

		public boolean isExactTotalResults() {
			return isExactTotalResults;
		}

		public void setExactTotalResults(boolean isExactTotalResults) {
			this.isExactTotalResults = isExactTotalResults;
		}

		public List<String> getPages() {
			return pages;
		}

		public void setPages(List<String> pages) {
			this.pages = pages;
		}

	}

	public final class Results {
		public String getContentNoFormatting() {
			return contentNoFormatting;
		}

		public void setContentNoFormatting(String contentNoFormatting) {
			this.contentNoFormatting = contentNoFormatting;
		}

		public String getTitleNoFormatting() {
			return titleNoFormatting;
		}

		public void setTitleNoFormatting(String titleNoFormatting) {
			this.titleNoFormatting = titleNoFormatting;
		}

		public String getVisibleUrl() {
			return visibleUrl;
		}

		public void setVisibleUrl(String visibleUrl) {
			this.visibleUrl = visibleUrl;
		}

		String contentNoFormatting;
		String titleNoFormatting;
		String visibleUrl;

	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

}
