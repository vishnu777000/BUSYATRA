package util;

import java.util.ArrayList;
import java.util.List;







public class SeatLayoutGenerator {

    






    public static List<String> generateSeats(int rows, int seatsPerRow) {

        List<String> seats = new ArrayList<>();

        char[] seatLetters = {'A','B','C','D','E','F'};

        for (int i = 1; i <= rows; i++) {

            for (int j = 0; j < seatsPerRow; j++) {

                String seat = i + String.valueOf(seatLetters[j]);

                seats.add(seat);
            }
        }

        return seats;
    }

    


    public static List<String> generateDefaultBusSeats() {

        return generateSeats(10, 4);
    }

}