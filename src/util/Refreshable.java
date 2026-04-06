package util;

public interface Refreshable {

    default void refreshData() {
        
    }

    default boolean refreshOnFirstShow() {
        return true;
    }

}
