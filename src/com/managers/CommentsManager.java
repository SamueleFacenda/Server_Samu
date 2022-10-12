package com.managers;

import java.io.*;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * There is the "comments" directory, here there is a file for each comment,
 * the name of a file is made by the user who wrote the comment and the timestamp
 * of the comment.
 * The file contains only the comment.
 * There are statics method for add comment for user, get all comments for user,
 * get all comments for user in a specific time interval.
 */
public class CommentsManager {
    public static String addComment(String user, String comment, Timestamp ts){
        File f = new File("comments/"+user+"_"+ts.toString()+".comm");
        if(f.exists()){
            return "Comment already exists";
        }else{
            try{
                f.createNewFile();
                try(PrintWriter pw = new PrintWriter(f)){
                    pw.println(comment);
                }catch(Exception e){
                    e.printStackTrace();
                    return "Error in writing comment";
                }
                return f.getName();
            }catch(Exception e){
                return "Error: "+e.getMessage();
            }
        }
    }
    //return String array with all comments for user
    public static String[] getComments(String user){
        File f = new File("comments");
        String[] files = f.list();
        List<String> l = new LinkedList<>();
        StringBuilder sb;
        for(String s : files){
            if(s.startsWith(user)){
                try(Scanner sc = new Scanner(new File("comments/"+s))){
                    sb = new StringBuilder();
                    while(sc.hasNextLine()){
                        sb.append(sc.nextLine());
                    }
                    l.add(sb.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return (String[]) l.toArray();
    }

    public static String getComment(String user, Timestamp ts){
        File f = new File("comments/"+user+"_"+ts.toString()+".comm");
        return getString(f);
    }
    public static String getComment(String fileName){
        File f = new File("comments/"+fileName);
        return getString(f);
    }

    private static String getString(File f) {
        if(f.exists()){
            try(Scanner sc = new Scanner(f)){
                StringBuilder sb = new StringBuilder();
                while(sc.hasNextLine()){
                    sb.append(sc.nextLine());
                }
                return sb.toString();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }else{
            return "Comment not found";
        }
    }
}
