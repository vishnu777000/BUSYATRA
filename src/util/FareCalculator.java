package util;





public class FareCalculator {

    private static final double DEFAULT_MULTIPLIER = 1.0;
    private static final double NON_AC_SEATER_MULTIPLIER = 0.95;
    private static final double AC_SEATER_MULTIPLIER = 1.10;
    private static final double NON_AC_SLEEPER_MULTIPLIER = 1.08;
    private static final double AC_SLEEPER_MULTIPLIER = 1.28;

    private FareCalculator(){}

    

    public static double baseFare(double routeFare, int seats){

        return routeFare * seats;
    }

    

    public static double applySurge(double amount, boolean peakHour){

        if(peakHour){
            return amount * 1.20;   
        }

        return amount;
    }

    

    public static double applyWeekendCharge(double amount, boolean weekend){

        if(weekend){
            return amount * 1.10;   
        }

        return amount;
    }

    

    public static double applyDiscount(double amount, double discount){

        double finalAmount = amount - discount;

        return Math.max(finalAmount,0);
    }

    public static double multiplierForBusType(String busType) {
        String normalized = normalize(busType);
        if (normalized.isEmpty()) {
            return DEFAULT_MULTIPLIER;
        }

        boolean nonAc = normalized.contains("NONAC") || normalized.contains("NON A C");
        boolean ac = normalized.contains("AC");
        boolean sleeper = normalized.contains("SLEEPER");
        boolean seater = normalized.contains("SEATER");

        if (nonAc && sleeper) return NON_AC_SLEEPER_MULTIPLIER;
        if (ac && sleeper) return AC_SLEEPER_MULTIPLIER;
        if (nonAc && seater) return NON_AC_SEATER_MULTIPLIER;
        if (ac && seater) return AC_SEATER_MULTIPLIER;
        if (nonAc) return NON_AC_SEATER_MULTIPLIER;
        if (ac) return AC_SEATER_MULTIPLIER;
        if (sleeper) return NON_AC_SLEEPER_MULTIPLIER;
        if (seater) return NON_AC_SEATER_MULTIPLIER;
        return DEFAULT_MULTIPLIER;
    }

    public static double resolveBusMultiplier(String busType, double storedMultiplier) {
        if (busType != null && !busType.isBlank()) {
            return multiplierForBusType(busType);
        }
        if (storedMultiplier > 0) {
            return storedMultiplier;
        }
        return multiplierForBusType(busType);
    }

    public static double calculateSegmentFare(int distanceKm, double routeRatePerKm, String busType, double storedMultiplier) {
        if (distanceKm <= 0 || routeRatePerKm <= 0) {
            return 0;
        }

        double multiplier = resolveBusMultiplier(busType, storedMultiplier);
        double fare = distanceKm * routeRatePerKm * multiplier;
        return Math.max(0, Math.round(fare * 100.0) / 100.0);
    }

    

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

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .trim()
                .toUpperCase()
                .replace("-", "")
                .replace("_", "")
                .replaceAll("\\s+", " ");
    }
}
