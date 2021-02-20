package com.footsites;


public class Main {
	private static int maxTasks = 1;
	
	static Settings settings = new Settings();
	public static void main(String[] args) {

		startTasks();
	}
	
	public static void startTasks (){
		Profile testProfile = new Profile("test","Bob","Tim","eatshoe42@gmail.com","2148391571","1 Infinite Loop","apt 1","95014","Cupertino","CA","4242424242424242","01","2026","444");
		for(int i = 0; i <maxTasks; i ++){
			new Task(Integer.toString(i), settings.getDelay(), testProfile, "0301103","L","footlocker").start();
		}
	}

}

