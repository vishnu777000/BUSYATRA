package util;

import java.util.HashSet;
import java.util.Set;

public class RefreshManager {

    private static final Set<Refreshable> listeners = new HashSet<>();

    private RefreshManager(){}

    

    public static void register(Refreshable r){

        if(r != null){
            listeners.add(r);
        }
    }

    

    public static void unregister(Refreshable r){

        listeners.remove(r);
    }

    

    public static void refreshAll(){

        for(Refreshable r : listeners){
            r.refreshData();
        }
    }
}