package com.footsites;

import java.util.ArrayList;

public class Proxies {

    private static ArrayList<String> proxies;
   // private static String currentProxy;
    public Proxies(){
		loadProxies();
		//currentProxy = getProxy();
	}

    public static String getProxy(){
    	if(proxies.size() >0){
			return proxies.get((int) Math.floor(Math.random() * proxies.size()));
		}
		return "";
    }
    
    
    public static String getIP(String currentProxy) {
    	String[] splitCurrentProxy = currentProxy.split(":");
    	for(int i = 0; i < splitCurrentProxy.length; i++) {
    		return splitCurrentProxy[i];
    	}
    	
    	return "";
    	
    }
    public static int getPort(String currentProxy) {
    	String[] splitCurrentProxy = currentProxy.split(":");
    	for(int i = 0; i < splitCurrentProxy.length; i++) {
    		String daPort = splitCurrentProxy[i+1];
    		int foo = Integer.parseInt(daPort);
    		return foo;
    	}
    	
    	return 0;
    	
    }
    
    public static String getUsername(String currentProxy) {
    	String[] splitCurrentProxy = currentProxy.split(":");
    	for(int i = 0; i < splitCurrentProxy.length; i++) {
    		return splitCurrentProxy[i+2];
    	}
    	
    	return "";
    	
    }
    public static String getPassword(String currentProxy) {
    	String[] splitCurrentProxy = currentProxy.split(":");
    	for(int i = 0; i < splitCurrentProxy.length; i++) {
    		return splitCurrentProxy[i+3];
    	}
    	
    	return "";
    	
    }
    

    public static void loadProxies(){
		proxies = Data.getData("proxies.txt");
    }
}