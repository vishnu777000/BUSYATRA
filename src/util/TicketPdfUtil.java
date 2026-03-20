package util;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

        Document document = new Document(PageSize.A4, 28, 28, 28, 28);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(path));

        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, new BaseColor(220, 53, 69));
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, new BaseColor(52, 58, 64));
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(33, 37, 41));
        Font mutedFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(108, 117, 125));
        Font chipFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);

        Paragraph title = new Paragraph("BusYatra E-Ticket", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        Paragraph sub = new Paragraph(
                "Issued on " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")),
                mutedFont
        );
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(14);
        document.add(sub);

        PdfPTable ref = new PdfPTable(new float[]{1, 1});
        ref.setWidthPercentage(100);
        ref.setSpacingAfter(8);
        PdfPCell pnr = new PdfPCell(new Phrase("PNR: BY-" + ticketId, chipFont));
        pnr.setPadding(6);
        pnr.setHorizontalAlignment(Element.ALIGN_CENTER);
        pnr.setBorder(Rectangle.NO_BORDER);
        pnr.setBackgroundColor(new BaseColor(35, 46, 62));
        PdfPCell booking = new PdfPCell(new Phrase("Booking ID: " + ticketId, chipFont));
        booking.setPadding(6);
        booking.setHorizontalAlignment(Element.ALIGN_CENTER);
        booking.setBorder(Rectangle.NO_BORDER);
        booking.setBackgroundColor(new BaseColor(63, 81, 181));
        ref.addCell(pnr);
        ref.addCell(booking);
        document.add(ref);

        PdfPTable top = new PdfPTable(new float[]{3, 1});
        top.setWidthPercentage(100);
        top.setSpacingAfter(14);

        PdfPTable table = new PdfPTable(new float[]{1.2f, 1.4f});
        table.setWidthPercentage(100);
        table.getDefaultCell().setBorderColor(new BaseColor(230, 230, 230));

        String boarding = route;
        String dropping = "-";
        if (route != null && route.contains("->")) {
            String[] parts = route.split("->");
            if (parts.length >= 2) {
                boarding = parts[0].trim();
                dropping = parts[1].trim();
            }
        }

        addRow(table, "Passenger Name", passenger, normalFont);
        addRow(table, "Boarding Point", boarding, normalFont);
        addRow(table, "Dropping Point", dropping, normalFont);
        addRow(table, "Route", route, normalFont);
        addRow(table, "Service", bus, normalFont);
        addRow(table, "Seat Number(s)", seats, normalFont);
        addRow(table, "Journey Time", date, normalFont);
        addRow(table, "Ticket Fare", "INR " + amount, normalFont);

        PdfPCell leftCell = new PdfPCell(table);
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);

        String qrPayload =
                "Ticket:" + ticketId + "\\n" +
                "Passenger:" + passenger + "\\n" +
                "Route:" + route + "\\n" +
                "Seats:" + seats + "\\n" +
                "Departure:" + date + "\\n" +
                "Amount:INR " + amount;

        BarcodeQRCode qrCode = new BarcodeQRCode(qrPayload, 140, 140, null);
        Image qrImage = qrCode.getImage();
        qrImage.scaleToFit(120, 120);

        PdfPCell qrCell = new PdfPCell();
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setVerticalAlignment(Element.ALIGN_TOP);
        qrCell.addElement(qrImage);
        Paragraph scan = new Paragraph("Scan for verification", mutedFont);
        scan.setAlignment(Element.ALIGN_CENTER);
        qrCell.addElement(scan);

        top.addCell(leftCell);
        top.addCell(qrCell);
        document.add(top);

        PdfPTable notes = new PdfPTable(1);
        notes.setWidthPercentage(100);
        PdfPCell note = new PdfPCell();
        note.setBorderColor(new BaseColor(235, 235, 235));
        note.setPadding(10);
        Paragraph h = new Paragraph("Passenger Instructions", sectionFont);
        h.setSpacingAfter(6);
        note.addElement(h);
        note.addElement(new Paragraph("1. Carry valid government ID proof for verification.", normalFont));
        note.addElement(new Paragraph("2. Report at boarding point at least 15 minutes before departure.", normalFont));
        note.addElement(new Paragraph("3. Keep this ticket for support, cancellation or refund requests.", normalFont));
        notes.addCell(note);
        document.add(notes);

        Paragraph thanks = new Paragraph("Thank you for booking with BusYatra. Have a safe journey.", mutedFont);
        thanks.setSpacingBefore(12);
        thanks.setAlignment(Element.ALIGN_CENTER);
        document.add(thanks);

        document.close();
        writer.close();
    }

    private static void addRow(PdfPTable table, String key, String value, Font valueFont) {
        Font keyFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(73, 80, 87));
        PdfPCell cell1 = new PdfPCell(new Phrase(key, keyFont));
        PdfPCell cell2 = new PdfPCell(new Phrase(value == null ? "-" : value, valueFont));

        cell1.setPadding(8);
        cell2.setPadding(8);
        cell1.setBorderColor(new BaseColor(235, 235, 235));
        cell2.setBorderColor(new BaseColor(235, 235, 235));

        table.addCell(cell1);
        table.addCell(cell2);
    }
}
