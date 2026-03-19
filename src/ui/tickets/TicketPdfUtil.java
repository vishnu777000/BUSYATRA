package ui.tickets;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TicketPdfUtil {

    public static void generateTicketPDF(
            String filePath,
            String ticketId,
            String passenger,
            String route,
            String bus,
            String seats,
            String journeyDate,
            String amount
    ) throws Exception {

        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        /* ================= FONTS ================= */
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD);
        Font subTitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 11);
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC);
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 9);

        /* ================= HEADER ================= */
        Paragraph title = new Paragraph("BusYatra – E-Ticket", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph sub = new Paragraph(
                "Safe • Reliable • Smart Travel",
                subTitleFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(10);
        doc.add(sub);

        doc.add(line());

        /* ================= TICKET META ================= */
        PdfPTable meta = new PdfPTable(2);
        meta.setWidthPercentage(100);
        meta.setSpacingBefore(10);
        meta.setWidths(new float[]{50, 50});

        addMeta(meta, "Ticket ID", ticketId);
        addMeta(meta, "Booking Time",
                LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));

        doc.add(meta);

        doc.add(Chunk.NEWLINE);

        /* ================= MAIN DETAILS ================= */
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{35, 65});

        addRow(table, "Passenger Name", passenger, labelFont, valueFont);
        addRow(table, "Route", route, labelFont, valueFont);
        addRow(table, "Bus Details", bus, labelFont, valueFont);
        addRow(table, "Seat Numbers", seats, labelFont, valueFont);
        addRow(table, "Journey Date", journeyDate, labelFont, valueFont);
        addRow(table, "Payment Mode", "Wallet", labelFont, valueFont);
        addRow(table, "Amount Paid", "₹ " + amount, labelFont, valueFont);

        doc.add(table);

        doc.add(Chunk.NEWLINE);
        doc.add(line());

        /* ================= IMPORTANT NOTES ================= */
        Paragraph noteTitle = new Paragraph("Important Instructions", labelFont);
        noteTitle.setSpacingBefore(10);
        doc.add(noteTitle);

        Paragraph note = new Paragraph(
                "• Carry a valid government ID proof.\n"
                        + "• Reporting time is at least 15 minutes before departure.\n"
                        + "• Ticket is non-transferable and non-modifiable.\n"
                        + "• Refund rules apply as per company policy.",
                valueFont);
        note.setSpacingBefore(6);
        doc.add(note);

        /* ================= FOOTER ================= */
        doc.add(Chunk.NEWLINE);
        doc.add(line());

        Paragraph footer = new Paragraph(
                "© 2026 BusYatra | www.busyatra.com | Support: support@busyatra.com\n"
                        + "This is a system generated ticket and does not require a signature.",
                footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(8);
        doc.add(footer);

        doc.close();
    }

    /* ================= HELPERS ================= */

    private static void addRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont
    ) {
        PdfPCell left = new PdfPCell(new Phrase(label, labelFont));
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(6);

        PdfPCell right = new PdfPCell(new Phrase(value, valueFont));
        right.setBorder(Rectangle.NO_BORDER);
        right.setPadding(6);

        table.addCell(left);
        table.addCell(right);
    }

    private static void addMeta(
            PdfPTable table,
            String label,
            String value
    ) {
        Font f = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(label + ": " + value, f));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);

        table.addCell(cell);
    }

    private static LineSeparator line() {
        LineSeparator ls = new LineSeparator();
        ls.setLineColor(new BaseColor(200, 200, 200));
        ls.setLineWidth(1f);
        return ls;
    }
}
