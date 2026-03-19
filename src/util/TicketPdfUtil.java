package util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.FileOutputStream;

public class TicketPdfUtil {

    public static void generateTicketPDF(
            String path,
            String ticketId,
            String passenger,
            String route,
            String bus,
            String seats,
            String date,
            String amount
    ) throws Exception {

        Document document = new Document();

        PdfWriter.getInstance(document, new FileOutputStream(path));

        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);

        Paragraph title = new Paragraph("BusYatra E-Ticket", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        addRow(table, "Ticket ID", ticketId);
        addRow(table, "Passenger", passenger);
        addRow(table, "Route", route);
        addRow(table, "Bus", bus);
        addRow(table, "Seats", seats);
        addRow(table, "Departure", date);
        addRow(table, "Amount Paid", "₹ " + amount);

        document.add(table);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Thank you for booking with BusYatra!", headerFont));
        document.add(new Paragraph("Have a safe journey.", normalFont));

        document.close();
    }

    private static void addRow(PdfPTable table, String key, String value) {

        PdfPCell cell1 = new PdfPCell(new Phrase(key));
        PdfPCell cell2 = new PdfPCell(new Phrase(value));

        cell1.setPadding(8);
        cell2.setPadding(8);

        table.addCell(cell1);
        table.addCell(cell2);
    }
}