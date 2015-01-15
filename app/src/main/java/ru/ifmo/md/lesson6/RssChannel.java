package ru.ifmo.md.lesson6;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lightning95 on 12/20/14.
 */

public class RssChannel {
    private String url;
    private String title;
    private String description;
    private List<RssPost> posts;

    public RssChannel() {
        posts = new ArrayList<RssPost>();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<RssPost> getPosts() {
        return posts;
    }

    public void addPost(RssPost post) {
        posts.add(post);
    }
}
