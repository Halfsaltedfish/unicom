package me.kuku.bean;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "user")
public class User {
    private int max;
    private List<Key> key;
    private HtmlCon htmlCon;

    public HtmlCon getHtmlCon() {
        return htmlCon;
    }

    public void setHtmlCon(HtmlCon htmlCon) {
        this.htmlCon = htmlCon;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public List<Key> getKey() {
        return key;
    }

    public void setKey(List<Key> key) {
        this.key = key;
    }
}
