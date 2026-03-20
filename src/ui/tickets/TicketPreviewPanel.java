package ui.tickets;

import config.UIConfig;
import dao.BookingDAO;
import dao.TicketDAO;
import ui.common.MainFrame;
import util.BookingContext;
import util.Refreshable;
import util.Session;
import util.TicketPdfUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class TicketPreviewPanel extends JPanel implements Refreshable {

    private final MainFrame frame;

    private JLabel pdfLabel;
    private JLabel metaLabel;
    private JLabel statusLabel;
    private JScrollPane previewScroll;
    private File generatedFile;
    private String fallbackText = "";
    private Image renderedPreview;

    public TicketPreviewPanel(MainFrame frame) {

        this.frame = frame;

        setLayout(new BorderLayout(18,18));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        add(header(),BorderLayout.NORTH);
        add(center(),BorderLayout.CENTER);
        add(actions(),BorderLayout.SOUTH);
    }

    /* ================= HEADER ================= */

    private JComponent header(){

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Booking Successful",SwingConstants.CENTER);
        title.setFont(UIConfig.FONT_TITLE);

        panel.add(title,BorderLayout.CENTER);

        return panel;
    }

    /* ================= CENTER ================= */

    private JComponent center(){

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel card = new JPanel(new BorderLayout());
        UIConfig.styleCard(card);
        card.setPreferredSize(new Dimension(980, 560));

        pdfLabel = new JLabel("Generating ticket preview...",SwingConstants.CENTER);
        pdfLabel.setHorizontalAlignment(SwingConstants.LEFT);
        pdfLabel.setVerticalAlignment(SwingConstants.TOP);
        pdfLabel.setOpaque(true);
        pdfLabel.setBackground(Color.WHITE);
        pdfLabel.setForeground(UIConfig.TEXT);
        pdfLabel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        pdfLabel.setPreferredSize(new Dimension(920, 420));

        metaLabel = new JLabel(" ", SwingConstants.LEFT);
        metaLabel.setFont(UIConfig.FONT_NORMAL);
        metaLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 8, 4));

        statusLabel = new JLabel("Preparing preview...", SwingConstants.LEFT);
        statusLabel.setFont(UIConfig.FONT_SMALL);
        statusLabel.setForeground(UIConfig.TEXT_LIGHT);

        previewScroll = new JScrollPane(pdfLabel);
        previewScroll.setBorder(null);
        previewScroll.getViewport().setBackground(Color.WHITE);
        previewScroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (renderedPreview != null && pdfLabel.getIcon() != null) {
                    pdfLabel.setIcon(new ImageIcon(scaleToViewport(renderedPreview)));
                }
            }
        });

        card.add(metaLabel, BorderLayout.NORTH);
        card.add(previewScroll, BorderLayout.CENTER);
        card.add(statusLabel, BorderLayout.SOUTH);

        wrapper.add(card);

        return wrapper;
    }

    /* ================= ACTIONS ================= */

    private JComponent actions(){

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JButton back = new JButton("Back to Dashboard");
        UIConfig.secondaryBtn(back);

        JButton download = new JButton("Download Ticket");
        UIConfig.primaryBtn(download);
        JButton fullscreen = new JButton("Full Screen");
        UIConfig.infoBtn(fullscreen);

        back.addActionListener(e -> {
            BookingContext.clear();
            frame.showScreen(MainFrame.SCREEN_USER);
        });

        download.addActionListener(e -> downloadTicket());
        fullscreen.addActionListener(e -> openFullScreenPreview());

        p.add(back,BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(fullscreen);
        right.add(download);
        p.add(right,BorderLayout.EAST);

        return p;
    }

    /* ================= REFRESH ================= */

    @Override
    public void refreshData(){

        pdfLabel.setIcon(null);
        pdfLabel.setText("Generating ticket preview...");
        metaLabel.setText(" ");
        statusLabel.setText("Collecting ticket details...");
        fallbackText = "";
        generatedFile = null;
        renderedPreview = null;

        try{

            int ticketId = BookingContext.ticketId;
            BookingDAO bookingDAO = new BookingDAO();
            if (ticketId <= 0) {
                ticketId = bookingDAO.getLatestConfirmedBookingId(Session.userId);
                if (ticketId <= 0) {
                    ticketId = bookingDAO.getLatestBookingId(Session.userId);
                }
                BookingContext.ticketId = ticketId;
            }
            if (ticketId <= 0) {
                pdfLabel.setText("Ticket data not found");
                statusLabel.setText("No booking available to preview.");
                return;
            }

            // Primary source is bookings flow; fallback to tickets table for legacy records.
            String[] data = bookingDAO.getTicketPreviewData(ticketId);
            if (data == null) {
                data = new TicketDAO().getTicketFullDetails(ticketId);
            }

            if(data == null){

                pdfLabel.setText("Ticket data not found");
                statusLabel.setText("No ticket record found.");
                return;
            }

            String passenger;
            String route;
            String bus;
            String seats;
            String date;
            String amount;
            String phone = "";
            String email = "";

            // BookingDAO format: [id, passenger, route, src, dst, seat, amount, journey_date, departure]
            if (data.length >= 11) {
                passenger = bestPassenger(data[1]);
                route = safe(data[2], safe(data[3], "-") + " -> " + safe(data[4], "-"));
                bus = "BusYatra Intercity";
                seats = safe(data[5], "-");
                date = safe(data[8], safe(data[7], "-"));
                amount = safe(data[6], "0");
                phone = safe(data[9], safe(BookingContext.passengerPhone, "-"));
                email = safe(data[10], safe(BookingContext.passengerEmail, "-"));
            } else {
                // TicketDAO legacy format: [passenger, route, operator, busType, seats, departure, amount]
                passenger = bestPassenger(data[0]);
                route = safe(data[1], "-");
                bus = safe(data[2], "-") + " | " + safe(data[3], "-");
                seats = safe(data[4], "-");
                date = safe(data[5], "-");
                amount = safe(data[6], "0");
                phone = safe(BookingContext.passengerPhone, "-");
                email = safe(BookingContext.passengerEmail, "-");
            }

            String path =
                    System.getProperty("java.io.tmpdir") +
                    "/ticket_" + ticketId + ".pdf";

            TicketPdfUtil.generateTicketPDF(
                    path,
                    String.valueOf(ticketId),
                    passenger,
                    route,
                    bus,
                    seats,
                    date,
                    amount
            );

            generatedFile = new File(path);
            if (!generatedFile.exists() || generatedFile.length() <= 0) {
                pdfLabel.setText("Ticket file generation failed");
                return;
            }
            fallbackText =
                    "Ticket ID: " + ticketId + "\n" +
                    "Passenger: " + passenger + "\n" +
                    "Phone: " + phone + "\n" +
                    "Email: " + email + "\n" +
                    "Route: " + route + "\n" +
                    "Bus: " + bus + "\n" +
                    "Seats: " + seats + "\n" +
                    "Departure: " + date + "\n" +
                    "Amount: INR " + amount;
            metaLabel.setText(
                    "<html><b>Ticket ID:</b> " + ticketId +
                    "&nbsp;&nbsp;&nbsp; <b>Passenger:</b> " + safe(passenger, "-") +
                    "&nbsp;&nbsp;&nbsp; <b>Phone:</b> " + safe(phone, "-") +
                    "&nbsp;&nbsp;&nbsp; <b>Seat(s):</b> " + safe(seats, "-") +
                    "&nbsp;&nbsp;&nbsp; <b>Amount:</b> INR " + safe(amount, "0") + "</html>"
            );
            statusLabel.setText("Rendering PDF preview...");

            showPdfPreviewAsync();

        }catch(Exception e){

            e.printStackTrace();
            pdfLabel.setText("Error loading ticket preview");
            statusLabel.setText("Failed to build preview.");
        }
    }

    /* ================= SHOW PDF ================= */

    private void showPdfPreviewAsync() {
        SwingWorker<Image, Void> worker = new SwingWorker<>() {
            @Override
            protected Image doInBackground() throws Exception {
                try (PDDocument doc = Loader.loadPDF(generatedFile)) {
                    PDFRenderer renderer = new PDFRenderer(doc);
                    return renderer.renderImageWithDPI(0, 140);
                }
            }

            @Override
            protected void done() {
                try {
                    Image img = get();
                    if (img == null || img.getWidth(null) <= 8 || img.getHeight(null) <= 8) {
                        throw new IllegalStateException("Rendered image is empty");
                    }
                    renderedPreview = img;
                    pdfLabel.setIcon(new ImageIcon(scaleToViewport(img)));
                    pdfLabel.setText(" ");
                    if (previewScroll != null) {
                        previewScroll.getHorizontalScrollBar().setValue(0);
                        previewScroll.getVerticalScrollBar().setValue(0);
                    }
                    statusLabel.setText("Preview ready.");
                } catch (Exception e) {
                    pdfLabel.setIcon(null);
                    if (fallbackText != null && !fallbackText.isBlank()) {
                        pdfLabel.setText("<html>" + fallbackText.replace("\n", "<br>") + "</html>");
                    } else {
                        pdfLabel.setText("Unable to render ticket preview");
                    }
                    statusLabel.setText("PDF preview not available. Ticket details shown.");
                }
            }
        };
        worker.execute();
    }

    /* ================= DOWNLOAD ================= */

    private void downloadTicket(){

        if(generatedFile==null){

            JOptionPane.showMessageDialog(this,"Ticket not ready");
            return;
        }

        JFileChooser chooser = new JFileChooser();

        chooser.setSelectedFile(
                new File("BusYatra_Ticket_" +
                        BookingContext.ticketId + ".pdf")
        );

        if(chooser.showSaveDialog(this)
                == JFileChooser.APPROVE_OPTION){

            try{

                Files.copy(
                        generatedFile.toPath(),
                        chooser.getSelectedFile().toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );

                JOptionPane.showMessageDialog(
                        this,
                        "Ticket saved successfully"
                );

            }catch(Exception ex){

                JOptionPane.showMessageDialog(
                        this,
                        "Failed to save ticket"
                );
            }
        }
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value;
    }

    private String bestPassenger(String fromData) {
        String p = safe(fromData, "").trim();
        if (!p.isBlank() && !"PASSENGER".equalsIgnoreCase(p)) return p;
        p = safe(BookingContext.passengerName, "").trim();
        if (!p.isBlank()) return p;
        p = safe(Session.username, "").trim();
        if (!p.isBlank()) return p;
        return "Guest Passenger";
    }

    private Image scaleToViewport(Image image) {
        int srcW = image.getWidth(null);
        int srcH = image.getHeight(null);
        if (srcW <= 0 || srcH <= 0) return image;

        int vw = previewScroll != null ? previewScroll.getViewport().getWidth() : 900;
        int vh = previewScroll != null ? previewScroll.getViewport().getHeight() : 500;
        vw = Math.max(760, vw - 20);
        vh = Math.max(440, vh - 20);

        double rw = (double) vw / (double) srcW;
        double rh = (double) vh / (double) srcH;
        double r = Math.min(rw, rh);
        if (r > 1.0) r = 1.0;

        int tw = Math.max(1, (int) Math.round(srcW * r));
        int th = Math.max(1, (int) Math.round(srcH * r));
        return image.getScaledInstance(tw, th, Image.SCALE_SMOOTH);
    }

    private void openFullScreenPreview() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Ticket Preview", Dialog.ModalityType.MODELESS);
        dialog.setUndecorated(true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.BLACK);

        JLabel content = new JLabel("", SwingConstants.CENTER);
        content.setForeground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        if (renderedPreview != null) {
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            int maxW = Math.max(800, size.width - 40);
            int maxH = Math.max(500, size.height - 80);
            int w = renderedPreview.getWidth(null);
            int h = renderedPreview.getHeight(null);
            if (w > 0 && h > 0) {
                double r = Math.min((double) maxW / w, (double) maxH / h);
                int tw = (int) Math.round(w * r);
                int th = (int) Math.round(h * r);
                content.setIcon(new ImageIcon(renderedPreview.getScaledInstance(tw, th, Image.SCALE_SMOOTH)));
            }
        } else if (fallbackText != null && !fallbackText.isBlank()) {
            content.setText("<html><div style='color:white;font-size:16px;'>" + fallbackText.replace("\n", "<br>") + "</div></html>");
        } else {
            content.setText("Ticket preview not available");
        }

        JButton close = new JButton("Close");
        UIConfig.secondaryBtn(close);
        close.addActionListener(e -> dialog.dispose());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.setOpaque(false);
        top.add(close);

        dialog.add(top, BorderLayout.NORTH);
        dialog.add(content, BorderLayout.CENTER);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setBounds(0, 0, screen.width, screen.height);
        dialog.setVisible(true);
    }
}
