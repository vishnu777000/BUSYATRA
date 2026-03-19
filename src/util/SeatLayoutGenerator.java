package util;

import java.util.ArrayList;
import java.util.List;

/**
 * SeatLayoutGenerator
 * Generates bus seat numbers based on rows and seat pattern.
 * Example pattern: 2x2 -> A B | C D
 * Seats generated: 1A,1B,1C,1D,2A,2B...
 */
public class SeatLayoutGenerator {

    /**
     * Generate seat numbers
     *
     * @param rows number of rows
     * @param seatsPerRow number of seats in each row
     * @return list of seat numbers
     */
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

    /**
     * Default layout for buses (10 rows, 4 seats each)
     */
    public static List<String> generateDefaultBusSeats() {

        return generateSeats(10, 4);
    }

}