package util;

public class Session {
    public static int userId;
    public static String username;
    public static String role;
    public static String userEmail;

    public static void clear(){
        userId=0;
        username=null;
        role=null;
        userEmail=null;
    }
}
