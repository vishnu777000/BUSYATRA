package dao;

import java.util.ArrayList;
import java.util.List;

public class StopDAO {

    public List<String> getStopsByRoute(int routeId) {

        List<String> stops = new ArrayList<>();
        List<String[]> rows = new RouteDAO().getStopsByRoute(routeId);

        for (String[] row : rows) {
            if (row != null && row.length > 0 && row[0] != null) {
                stops.add(row[0]);
            }
        }

        return stops;
    }
}
