package com.sangeethlabs.jekyll.search;

import java.util.Date;

public class Post {
    private String url;
    private String title;
    private Date date;
    private String excerpt;
    public Post() {
        super();
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
    public String getExcerpt() {
        return excerpt;
    }
    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }
}
