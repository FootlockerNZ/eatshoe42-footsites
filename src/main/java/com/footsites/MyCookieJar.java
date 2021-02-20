package com.footsites;
import java.util.List;
import okhttp3.*;
import java.util.ArrayList;


public class MyCookieJar implements CookieJar {

    private List<Cookie> cookies = new ArrayList<>();
    
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
       this.cookies.addAll(cookies);
    }

    public List<Cookie> loadForRequest(HttpUrl url) {
        if (cookies != null)
            return cookies;
        return new ArrayList<Cookie>();

    } 
}