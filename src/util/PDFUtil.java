package util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

public class PDFUtil {

    private static final BaseColor BUSYATRA_BLUE = new BaseColor(33,150,243);

    

    private static byte[] generateQRBytes(String text, int size) throws Exception {

        if(text == null || text.isEmpty())
            text = "BusYatra Ticket";

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bm = writer.encode(text, BarcodeFormat.QR_CODE, size, size);

        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

        for(int x=0;x<size;x++){
            for(int y=0;y<size;y++){
                img.setRGB(x,y,bm.get(x,y)?0x000000:0xFFFFFF);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img,"png",baos);

        return baos.toByteArray();
    }

    

    private static PdfPCell cell(String text, Font font, int align, boolean border){

        if(text==null) text="";

        PdfPCell c = new PdfPCell(new Phrase(text,font));

        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(8f);

        if(!border)
            c.setBorder(Rectangle.NO_BORDER);

        return c;
    }

    

    private static PdfPCell sectionTitle(String text){

        Font f = new Font(Font.FontFamily.HELVETICA,12,Font.BOLD,BaseColor.WHITE);

        PdfPCell c = new PdfPCell(new Phrase(text,f));
        c.setBackgroundColor(BUSYATRA_BLUE);
        c.setPadding(8f);
        c.setBorder(Rectangle.NO_BORDER);

        return c;
    }

    

    public static void createPremiumTicketPDF(String filePath, TicketPDFData data){

        try(FileOutputStream fos = new FileOutputStream(filePath)) {

            Document doc = new Document(PageSize.A4,36,36,36,36);
            PdfWriter writer = PdfWriter.getInstance(doc,fos);

            doc.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA,20,Font.BOLD,BUSYATRA_BLUE);
            Font small = new Font(Font.FontFamily.HELVETICA,10,Font.NORMAL,BaseColor.DARK_GRAY);
            Font bold = new Font(Font.FontFamily.HELVETICA,11,Font.BOLD);
            Font normal = new Font(Font.FontFamily.HELVETICA,11);

            

            PdfContentByte canvas = writer.getDirectContentUnder();

            Font wmFont = new Font(Font.FontFamily.HELVETICA,60,Font.BOLD,new BaseColor(230,230,230));
            Phrase watermark = new Phrase("BUSYATRA",wmFont);

            ColumnText.showTextAligned(canvas,
                    Element.ALIGN_CENTER,
                    watermark,
                    300,
                    420,
                    45);

            

            Paragraph app = new Paragraph("BusYatra",titleFont);
            app.setAlignment(Element.ALIGN_CENTER);
            doc.add(app);

            Paragraph tag = new Paragraph("E-Ticket / Booking Confirmation",bold);
            tag.setAlignment(Element.ALIGN_CENTER);
            doc.add(tag);

            Paragraph meta = new Paragraph(
                    "Booking ID/PNR: "+data.getTicketId()+
                            " | Status: "+safe(data.getStatus())+
                            " | Booking Time: "+safe(data.getBookingTime()),
                    small);

            meta.setAlignment(Element.ALIGN_CENTER);
            doc.add(meta);

            doc.add(new Paragraph(" "));

            

            PdfPTable top = new PdfPTable(new float[]{70,30});
            top.setWidthPercentage(100);

            PdfPCell leftTop = new PdfPCell();
            leftTop.setBorder(Rectangle.BOX);
            leftTop.setPadding(10f);

            Paragraph p1 = new Paragraph("Passenger Details",bold);
            p1.setSpacingAfter(6f);

            leftTop.addElement(p1);
            leftTop.addElement(new Paragraph("Name: "+safe(data.getPassengerName()),normal));
            leftTop.addElement(new Paragraph("Mobile: "+safe(data.getMobile()),normal));
            leftTop.addElement(new Paragraph("Email: "+safe(data.getEmail()),normal));

            top.addCell(leftTop);

            

            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.BOX);
            qrCell.setPadding(10f);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            byte[] qrBytes = generateQRBytes(data.getQrText(),180);
            Image qr = Image.getInstance(qrBytes);

            qr.scaleToFit(150,150);
            qr.setAlignment(Image.ALIGN_CENTER);

            Paragraph qrTitle = new Paragraph("Scan QR at Boarding",bold);
            qrTitle.setAlignment(Element.ALIGN_CENTER);

            qrCell.addElement(qrTitle);
            qrCell.addElement(qr);
            qrCell.addElement(new Paragraph("Show this ticket to conductor",small));

            top.addCell(qrCell);

            doc.add(top);
            doc.add(new Paragraph(" "));

            

            PdfPTable trip = new PdfPTable(2);
            trip.setWidthPercentage(100);

            trip.addCell(sectionTitle("Trip Details"));

            PdfPCell dummy = new PdfPCell(new Phrase(""));
            dummy.setBorder(Rectangle.NO_BORDER);
            dummy.setBackgroundColor(BUSYATRA_BLUE);
            trip.addCell(dummy);

            trip.addCell(cell("From",bold,Element.ALIGN_LEFT,true));
            trip.addCell(cell(data.getSource(),normal,Element.ALIGN_LEFT,true));

            trip.addCell(cell("To",bold,Element.ALIGN_LEFT,true));
            trip.addCell(cell(data.getDestination(),normal,Element.ALIGN_LEFT,true));

            trip.addCell(cell("Travel Date",bold,Element.ALIGN_LEFT,true));
            trip.addCell(cell(data.getTravelDate(),normal,Element.ALIGN_LEFT,true));

            trip.addCell(cell("Departure Time",bold,Element.ALIGN_LEFT,true));
            trip.addCell(cell(data.getDepartureTime(),normal,Element.ALIGN_LEFT,true));

            trip.addCell(cell("Arrival Time",bold,Element.ALIGN_LEFT,true));
            trip.addCell(cell(data.getArrivalTime(),normal,Element.ALIGN_LEFT,true));

            trip.addCell(cell("Operator",bold,Element.ALIGN_LEFT,true));
            trip.addCell(cell(data.getOperator(),normal,Element.ALIGN_LEFT,true));

            trip.addCell(cell("Bus Type",bold,Element.ALIGN_LEFT,true));
            trip.addCell(cell(data.getBusType(),normal,Element.ALIGN_LEFT,true));

            doc.add(trip);

            doc.close();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static String safe(String v){
        return v==null?"":v;
    }
}