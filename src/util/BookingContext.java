package util;

import java.util.*;

public final class BookingContext {

    private BookingContext(){}

    /* ================= CORE ================= */

    public static int scheduleId = -1;
    public static int ticketId = -1;

    public static int routeId = -1;

    public static String fromStop = "";
    public static String toStop = "";

    public static int fromOrder = -1;
    public static int toOrder = -1;

    public static String journeyDate = "";

    public static String operatorName = "";
    public static int totalSeats = 0;

    public static String passengerName = "";
    public static String passengerPhone = "";
    public static String passengerEmail = "";

    public static double farePerSeat = 0;
    public static double discount = 0;

    /* ================= SEATS ================= */

    private static final Set<String> selectedSeats =
            Collections.synchronizedSet(new HashSet<>());

    private static final int MAX_SEATS = 6;

    /* ================= ADD ================= */

    public static boolean addSeat(String seatNo){

        if(!isValidSeat(seatNo)) return false;

        synchronized (selectedSeats){

            if(selectedSeats.size() >= MAX_SEATS)
                return false;

            boolean added = selectedSeats.add(seatNo);

            recalc();
            return added;
        }
    }

    /* ================= REMOVE ================= */

    public static void removeSeat(String seatNo){

        if(seatNo == null) return;

        synchronized (selectedSeats){
            selectedSeats.remove(seatNo);
        }

        recalc();
    }

    /* ================= TOGGLE ================= */

    public static void toggleSeat(String seatNo){

        if(isSeatSelected(seatNo)){
            removeSeat(seatNo);
        } else {
            addSeat(seatNo);
        }
    }

    /* ================= CHECK ================= */

    public static boolean isSeatSelected(String seatNo){
        return selectedSeats.contains(seatNo);
    }

    /* ================= CLEAR ================= */

    public static void clearSeats(){

        synchronized (selectedSeats){
            selectedSeats.clear();
        }

        recalc();
    }

    /* ================= GET ================= */

    public static Set<String> getSelectedSeats(){
        return Collections.unmodifiableSet(selectedSeats);
    }

    public static Set<String> copySelectedSeats(){
        return new HashSet<>(selectedSeats);
    }

    /* ================= SORTED STRING ================= */

    public static String getSeatListString(){

        if(selectedSeats.isEmpty()) return "-";

        List<String> sorted = new ArrayList<>(selectedSeats);

        sorted.sort(Comparator.comparingInt(s ->
                Integer.parseInt(s.replaceAll("\\D",""))
        ));

        return String.join(", ", sorted);
    }

    /* ================= COUNT ================= */

    public static int seatCount(){
        return selectedSeats.size();
    }

    /* ================= AMOUNT ================= */

    public static double getBaseAmount(){
        return seatCount() * farePerSeat;
    }

    public static double getFinalAmount(){
        return Math.max(getBaseAmount() - discount,0);
    }

    public static double getFinalPayable(){
        return getFinalAmount();
    }

    /* ================= VALIDATION ================= */

    public static boolean isReadyForPayment(){

        return scheduleId > 0
                && routeId > 0
                && fromOrder >= 0
                && toOrder >= 0
                && !selectedSeats.isEmpty()
                && farePerSeat > 0
                && notBlank(journeyDate)
                && notBlank(passengerName);
    }

    private static boolean notBlank(String s){
        return s != null && !s.isBlank();
    }

    private static boolean isValidSeat(String seatNo){
        return seatNo != null && seatNo.matches("S\\d+");
    }

    /* ================= RECALC ================= */

    public static void recalc(){
        // future logic (surge pricing, coupons)
    }

    /* ================= RESET ================= */

    public static void clear(){

        scheduleId = -1;
        ticketId = -1;

        routeId = -1;

        fromStop = "";
        toStop = "";

        fromOrder = -1;
        toOrder = -1;

        journeyDate = "";

        operatorName = "";
        totalSeats = 0;

        passengerName = "";
        passengerPhone = "";
        passengerEmail = "";

        farePerSeat = 0;
        discount = 0;

        clearSeats();
    }

    public static void clearAfterPreview(){
        ticketId = -1;
    }

    /* ================= DEBUG ================= */

    public static void debugPrint(){

        System.out.println("\n==== BOOKING CONTEXT ====");

        System.out.println("Schedule : " + scheduleId);
        System.out.println("Route    : " + routeId);

        System.out.println("From     : " + fromStop + " (" + fromOrder + ")");
        System.out.println("To       : " + toStop + " (" + toOrder + ")");

        System.out.println("Date     : " + journeyDate);
        System.out.println("Passenger: " + passengerName);

        System.out.println("Seats    : " + getSeatListString());

        System.out.println("Fare/Seat: " + farePerSeat);
        System.out.println("Total    : " + getFinalAmount());

        System.out.println("=========================\n");
    }
}