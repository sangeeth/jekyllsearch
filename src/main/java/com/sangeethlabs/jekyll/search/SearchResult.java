package com.sangeethlabs.jekyll.search;

import java.io.Serializable;
import java.util.List;

public class SearchResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String queryString;
    
    private List<Post> posts;

    public SearchResult() {
        super();
    }
    
    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }
    
}
