package util;

import java.util.*;

public final class BookingContext {

    private BookingContext(){}

    

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
    public static String couponCode = "";

    

    private static final Set<String> selectedSeats =
            Collections.synchronizedSet(new HashSet<>());
    private static final List<Integer> recentTicketIds =
            Collections.synchronizedList(new ArrayList<>());

    private static final int MAX_SEATS = 6;

    

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

    

    public static void removeSeat(String seatNo){

        if(seatNo == null) return;

        synchronized (selectedSeats){
            selectedSeats.remove(seatNo);
        }

        recalc();
    }

    

    public static void toggleSeat(String seatNo){

        if(isSeatSelected(seatNo)){
            removeSeat(seatNo);
        } else {
            addSeat(seatNo);
        }
    }

    

    public static boolean isSeatSelected(String seatNo){
        return selectedSeats.contains(seatNo);
    }

    

    public static void clearSeats(){

        synchronized (selectedSeats){
            selectedSeats.clear();
        }

        recalc();
    }

    

    public static Set<String> getSelectedSeats(){
        return Collections.unmodifiableSet(selectedSeats);
    }

    public static Set<String> copySelectedSeats(){
        return new HashSet<>(selectedSeats);
    }

    public static void setRecentTicketIds(Collection<Integer> ticketIds) {
        synchronized (recentTicketIds) {
            recentTicketIds.clear();
            if (ticketIds != null) {
                for (Integer id : ticketIds) {
                    if (id != null && id > 0 && !recentTicketIds.contains(id)) {
                        recentTicketIds.add(id);
                    }
                }
            }
            ticketId = recentTicketIds.isEmpty()
                    ? -1
                    : recentTicketIds.get(recentTicketIds.size() - 1);
        }
    }

    public static void setActiveTicketId(int id) {
        ticketId = id;
        setRecentTicketIds(id > 0 ? Collections.singletonList(id) : Collections.emptyList());
    }

    public static List<Integer> getRecentTicketIds() {
        synchronized (recentTicketIds) {
            return new ArrayList<>(recentTicketIds);
        }
    }

    public static int getRecentTicketCount() {
        synchronized (recentTicketIds) {
            return recentTicketIds.size();
        }
    }

    public static int getPrimaryTicketId() {
        synchronized (recentTicketIds) {
            if (!recentTicketIds.isEmpty()) {
                return recentTicketIds.get(recentTicketIds.size() - 1);
            }
        }
        return ticketId;
    }

    public static String getTicketIdListString() {
        synchronized (recentTicketIds) {
            if (recentTicketIds.isEmpty()) return "-";
            List<String> ids = new ArrayList<>();
            for (Integer id : recentTicketIds) {
                ids.add(String.valueOf(id));
            }
            return String.join(", ", ids);
        }
    }

    public static void clearRecentTicketIds() {
        synchronized (recentTicketIds) {
            recentTicketIds.clear();
        }
        ticketId = -1;
    }

    

    public static String getSeatListString(){

        if(selectedSeats.isEmpty()) return "-";

        List<String> sorted = new ArrayList<>(selectedSeats);

        sorted.sort(Comparator.comparingInt(s ->
                Integer.parseInt(s.replaceAll("\\D",""))
        ));

        return String.join(", ", sorted);
    }

    

    public static int seatCount(){
        return selectedSeats.size();
    }

    

    public static double getBaseAmount(){
        return seatCount() * farePerSeat;
    }

    public static double getEffectiveDiscount() {
        return Math.min(Math.max(discount, 0), getBaseAmount());
    }

    public static double getFinalAmount(){
        return Math.max(getBaseAmount() - getEffectiveDiscount(),0);
    }

    public static double getFinalPayable(){
        return getFinalAmount();
    }

    

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

    

    public static void recalc(){
        
    }

    public static void applyCoupon(String code, double discountAmount) {
        couponCode = code == null ? "" : code.trim().toUpperCase();
        discount = Math.max(0, discountAmount);
    }

    public static void clearCoupon() {
        couponCode = "";
        discount = 0;
    }

    

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
        couponCode = "";

        clearRecentTicketIds();
        clearSeats();
    }

    public static void clearAfterPreview(){
        clearRecentTicketIds();
    }

    

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
