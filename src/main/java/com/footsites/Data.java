package com.footsites;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class Data {

    public Data(){
        ensureDataPath();
        ensureDataFile("proxies.txt");
    }

    private void ensureDataPath(){
        String path = getDataPath();
        File dataDir = new File(path);
        if(!dataDir.exists()){
            dataDir.mkdir();
        }
    }

    private static void ensureDataFile(String filename){
        String path = getDataPath();
        File dataFile = new File(path + "\\" + filename);
        try {
            if(dataFile.createNewFile()){
                System.out.println("made file");
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    public static ArrayList<String> getData(String filename){
        ensureDataFile(filename);
        Scanner in = null;
        ArrayList<String> lines = new ArrayList<>();
        try {
            in = new Scanner(new File(Data.getDataPath() + "\\" + filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while(in.hasNext()) {
            lines.add(in.next());
        }
        in.close();
        return lines;
    }

    public static String getDataPath(){
        String platform = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if(platform.indexOf("windows") > -1){
            return System.getenv("AppData") + "\\eatshoeFootsites";
        }
        else{
            return System.getProperty("user.home") + "/Library/Application Support/eatshoeFootsites";
        }
    }
}
