package util;

/**
 * FareCalculator
 * Handles fare calculation logic for BusYatra
 */
public class FareCalculator {

    private FareCalculator(){}

    /* ================= BASE ================= */

    public static double baseFare(double routeFare, int seats){

        return routeFare * seats;
    }

    /* ================= SURGE ================= */

    public static double applySurge(double amount, boolean peakHour){

        if(peakHour){
            return amount * 1.20;   // 20% surge
        }

        return amount;
    }

    /* ================= WEEKEND ================= */

    public static double applyWeekendCharge(double amount, boolean weekend){

        if(weekend){
            return amount * 1.10;   // 10% increase
        }

        return amount;
    }

    /* ================= DISCOUNT ================= */

    public static double applyDiscount(double amount, double discount){

        double finalAmount = amount - discount;

        return Math.max(finalAmount,0);
    }

    /* ================= FINAL CALC ================= */

    public static double calculateFinalFare(
            double routeFare,
            int seats,
            double discount,
            boolean weekend,
            boolean peakHour
    ){

        double amount = baseFare(routeFare,seats);

        amount = applySurge(amount,peakHour);

        amount = applyWeekendCharge(amount,weekend);

        amount = applyDiscount(amount,discount);

        return Math.round(amount);
    }
}