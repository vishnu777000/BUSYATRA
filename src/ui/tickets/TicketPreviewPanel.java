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
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TicketPreviewPanel extends JPanel implements Refreshable {

    private final MainFrame frame;

    private JLabel pdfLabel;
    private JLabel metaLabel;
    private JLabel statusLabel;
    private JScrollPane previewScroll;
    private File generatedFile;
    private String fallbackText = "";
    private String currentTicketRef = "";
    private Image renderedPreview;
    private PreviewPayload currentPreview;
    private volatile long previewToken = 0L;

    public TicketPreviewPanel(MainFrame frame) {

        this.frame = frame;

        setLayout(new BorderLayout(18, 18));
        setBackground(UIConfig.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(header(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);
    }

    private JComponent header() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel title = new JLabel("Booking Successful", SwingConstants.CENTER);
        title.setFont(UIConfig.FONT_TITLE);

        panel.add(title, BorderLayout.CENTER);

        return panel;
    }

    private JComponent center() {

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel card = new JPanel(new BorderLayout());
        UIConfig.styleCard(card);
        card.setPreferredSize(new Dimension(980, 560));

        pdfLabel = new JLabel("Generating ticket preview...", SwingConstants.CENTER);
        pdfLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pdfLabel.setVerticalAlignment(SwingConstants.TOP);
        pdfLabel.setOpaque(true);
        pdfLabel.setBackground(Color.WHITE);
        pdfLabel.setForeground(UIConfig.TEXT);
        pdfLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
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

    private JComponent actions() {

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

        p.add(back, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(fullscreen);
        right.add(download);
        p.add(right, BorderLayout.EAST);

        return p;
    }

    @Override
    public void refreshData() {

        long token = ++previewToken;
        pdfLabel.setIcon(null);
        pdfLabel.setText("Generating ticket preview...");
        metaLabel.setText(" ");
        statusLabel.setText("Collecting ticket details...");
        fallbackText = "";
        currentTicketRef = "";
        generatedFile = null;
        renderedPreview = null;
        currentPreview = null;

        SwingWorker<PreviewPayload, Void> worker = new SwingWorker<>() {
            @Override
            protected PreviewPayload doInBackground() throws Exception {
                return buildPreviewPayload();
            }

            @Override
            protected void done() {
                if (token != previewToken) {
                    return;
                }

                try {
                    PreviewPayload payload = get();
                    if (payload == null) {
                        throw new IllegalStateException("Preview payload missing");
                    }
                    if (payload.errorMessage != null) {
                        pdfLabel.setText(payload.errorMessage);
                        statusLabel.setText(payload.statusMessage == null ? "Preview unavailable." : payload.statusMessage);
                        return;
                    }

                    BookingContext.setRecentTicketIds(payload.bookingIds);
                    generatedFile = payload.file;
                    fallbackText = payload.fallbackText;
                    currentTicketRef = payload.ticketRef;
                    currentPreview = payload;
                    metaLabel.setText(payload.metaHtml);
                    statusLabel.setText("Rendering PDF preview...");
                    showPdfPreviewAsync(token, payload);
                } catch (Exception e) {
                    e.printStackTrace();
                    pdfLabel.setText("Error loading ticket preview");
                    statusLabel.setText("Failed to build preview.");
                }
            }
        };

        worker.execute();
    }

    private PreviewPayload buildPreviewPayload() throws Exception {
        PreviewPayload payload = new PreviewPayload();

        BookingDAO bookingDAO = new BookingDAO();
        List<Integer> bookingIds = new ArrayList<>(BookingContext.getRecentTicketIds());
        int ticketId = BookingContext.getPrimaryTicketId();

        if (bookingIds.isEmpty() && ticketId > 0) {
            bookingIds = Collections.singletonList(ticketId);
        }
        if (bookingIds.isEmpty()) {
            ticketId = bookingDAO.getLatestConfirmedBookingId(Session.userId);
            if (ticketId <= 0) {
                ticketId = bookingDAO.getLatestBookingId(Session.userId);
            }
            if (ticketId > 0) {
                bookingIds = Collections.singletonList(ticketId);
            }
        }
        if (bookingIds.isEmpty()) {
            payload.errorMessage = "Ticket data not found";
            payload.statusMessage = "No booking available to preview.";
            return payload;
        }

        String[] data = bookingIds.size() > 1
                ? bookingDAO.getCombinedTicketPreviewData(bookingIds)
                : bookingDAO.getTicketPreviewData(bookingIds.get(0));
        if (data == null && bookingIds.size() == 1) {
            data = new TicketDAO().getTicketFullDetails(bookingIds.get(0));
        }
        if (data == null) {
            payload.errorMessage = "Ticket data not found";
            payload.statusMessage = "No ticket record found.";
            return payload;
        }

        String passenger;
        String route;
        String bus;
        String seats;
        String date;
        String amount;
        String phone;
        String email;
        String ticketRef;

        if (data.length >= 11) {
            ticketRef = safe(data[0], String.valueOf(bookingIds.get(bookingIds.size() - 1)));
            passenger = bestPassenger(data[1]);
            route = safe(data[2], safe(data[3], "-") + " -> " + safe(data[4], "-"));
            bus = "BusYatra Intercity";
            seats = safe(data[5], "-");
            date = safe(data[8], safe(data[7], "-"));
            amount = safe(data[6], "0");
            phone = safe(data[9], safe(BookingContext.passengerPhone, "-"));
            email = safe(data[10], safe(BookingContext.passengerEmail, "-"));
        } else {
            ticketRef = String.valueOf(bookingIds.get(0));
            passenger = bestPassenger(data[0]);
            route = safe(data[1], "-");
            bus = safe(data[2], "-") + " | " + safe(data[3], "-");
            seats = safe(data[4], "-");
            date = safe(data[5], "-");
            amount = safe(data[6], "0");
            phone = safe(BookingContext.passengerPhone, "-");
            email = safe(BookingContext.passengerEmail, "-");
        }

        File file = createWorkingPdfFile(ticketRef);
        TicketPdfUtil.generateTicketPDF(
                file.getAbsolutePath(),
                ticketRef,
                passenger,
                route,
                bus,
                seats,
                date,
                amount
        );

        if (!file.exists() || file.length() <= 0) {
            payload.errorMessage = "Ticket file generation failed";
            payload.statusMessage = "Ticket PDF could not be generated.";
            return payload;
        }

        payload.ticketId = bookingIds.get(bookingIds.size() - 1);
        payload.bookingIds = new ArrayList<>(bookingIds);
        payload.ticketRef = ticketRef;
        payload.file = file;
        payload.fallbackText =
                "Ticket Ref: " + ticketRef + "\n" +
                (bookingIds.size() > 1 ? "Tickets: " + bookingIds.size() + "\n" : "") +
                "Passenger: " + passenger + "\n" +
                "Phone: " + phone + "\n" +
                "Email: " + email + "\n" +
                "Route: " + route + "\n" +
                "Bus: " + bus + "\n" +
                "Seats: " + seats + "\n" +
                "Departure: " + date + "\n" +
                "Amount: INR " + amount;
        payload.metaHtml =
                "<html><b>Ticket Ref:</b> " + ticketRef +
                (bookingIds.size() > 1
                        ? "&nbsp;&nbsp;&nbsp; <b>Tickets:</b> " + bookingIds.size()
                        : "") +
                "&nbsp;&nbsp;&nbsp; <b>Passenger:</b> " + safe(passenger, "-") +
                "&nbsp;&nbsp;&nbsp; <b>Phone:</b> " + safe(phone, "-") +
                "&nbsp;&nbsp;&nbsp; <b>Seat(s):</b> " + safe(seats, "-") +
                "&nbsp;&nbsp;&nbsp; <b>Amount:</b> INR " + safe(amount, "0") + "</html>";
        payload.passenger = passenger;
        payload.route = route;
        payload.bus = bus;
        payload.seats = seats;
        payload.date = date;
        payload.amount = amount;
        payload.phone = phone;
        payload.email = email;
        return payload;
    }

    private void showPdfPreviewAsync(long token, PreviewPayload payload) {
        SwingWorker<PreviewRenderResult, Void> worker = new SwingWorker<>() {
            @Override
            protected PreviewRenderResult doInBackground() {
                PreviewRenderResult result = new PreviewRenderResult();
                result.image = createFallbackPreviewImage(payload);
                try {
                    Image pdfImage = renderPdfPreview(payload == null ? null : payload.file);
                    if (isUsableImage(pdfImage)) {
                        result.image = pdfImage;
                        result.usedPdfRenderer = true;
                    }
                } catch (Throwable ex) {
                    result.usedPdfRenderer = false;
                    result.renderError = ex.getMessage();
                }
                return result;
            }

            @Override
            protected void done() {
                if (token != previewToken) {
                    return;
                }
                try {
                    PreviewRenderResult result = get();
                    Image img = result == null ? null : result.image;
                    if (!isUsableImage(img)) {
                        img = createFallbackPreviewImage(payload);
                    }
                    applyPreviewImage(img);
                    statusLabel.setText(result != null && result.usedPdfRenderer
                            ? "Preview ready."
                            : "Preview ready. Showing ticket layout.");
                } catch (Exception e) {
                    try {
                        applyPreviewImage(createFallbackPreviewImage(payload));
                        statusLabel.setText("Preview ready. Showing ticket layout.");
                    } catch (Exception ignored) {
                        pdfLabel.setIcon(null);
                        if (fallbackText != null && !fallbackText.isBlank()) {
                            pdfLabel.setText("<html>" + fallbackText.replace("\n", "<br>") + "</html>");
                        } else {
                            pdfLabel.setText("Unable to render ticket preview");
                        }
                        statusLabel.setText("PDF preview not available. Ticket details shown.");
                    }
                }
            }
        };
        worker.execute();
    }

    private Image renderPdfPreview(File pdfFile) throws Exception {
        if (pdfFile == null || !pdfFile.isFile() || pdfFile.length() <= 0) {
            throw new IllegalStateException("PDF file not available");
        }

        Class<?> loaderClass = Class.forName("org.apache.pdfbox.Loader");
        Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
        Class<?> pdfRendererClass = Class.forName("org.apache.pdfbox.rendering.PDFRenderer");

        Method loadPdf = loaderClass.getMethod("loadPDF", File.class);
        Object document = loadPdf.invoke(null, pdfFile);

        try {
            Constructor<?> constructor = pdfRendererClass.getConstructor(pdDocumentClass);
            Object renderer = constructor.newInstance(document);
            Method renderMethod = pdfRendererClass.getMethod("renderImageWithDPI", int.class, float.class);
            return (Image) renderMethod.invoke(renderer, 0, 140f);
        } finally {
            Method closeMethod = pdDocumentClass.getMethod("close");
            closeMethod.invoke(document);
        }
    }

    private boolean isUsableImage(Image image) {
        return image != null && image.getWidth(null) > 8 && image.getHeight(null) > 8;
    }

    private void applyPreviewImage(Image image) {
        if (!isUsableImage(image)) {
            throw new IllegalStateException("Rendered image is empty");
        }
        renderedPreview = image;
        pdfLabel.setIcon(new ImageIcon(scaleToViewport(image)));
        pdfLabel.setText(" ");
        if (previewScroll != null) {
            previewScroll.getHorizontalScrollBar().setValue(0);
            previewScroll.getVerticalScrollBar().setValue(0);
        }
    }

    private Image createFallbackPreviewImage(PreviewPayload payload) {
        int width = 1240;
        int height = 1754;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color background = new Color(245, 247, 250);
            Color panel = Color.WHITE;
            Color accent = new Color(220, 53, 69);
            Color accentDark = new Color(35, 46, 62);
            Color ink = new Color(33, 37, 41);
            Color muted = new Color(108, 117, 125);
            Color border = new Color(230, 232, 236);

            g.setColor(background);
            g.fillRect(0, 0, width, height);

            int cardX = 80;
            int cardY = 70;
            int cardW = width - (cardX * 2);
            int cardH = height - 140;

            g.setColor(panel);
            g.fillRoundRect(cardX, cardY, cardW, cardH, 32, 32);
            g.setColor(border);
            g.drawRoundRect(cardX, cardY, cardW, cardH, 32, 32);

            GradientPaint headerPaint = new GradientPaint(
                    cardX,
                    cardY,
                    accent,
                    cardX + cardW,
                    cardY + 180,
                    accentDark
            );
            g.setPaint(headerPaint);
            g.fillRoundRect(cardX, cardY, cardW, 190, 32, 32);
            g.setColor(panel);
            g.fillRect(cardX, cardY + 150, cardW, 40);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 38));
            g.drawString("BusYatra E-Ticket", cardX + 40, cardY + 68);

            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g.drawString("Issued on " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")), cardX + 42, cardY + 105);

            drawChip(g, cardX + 40, cardY + 124, 250, 36, accentDark, "PNR: BY-" + safe(payload == null ? null : payload.ticketRef, "-"));
            drawChip(g, cardX + 308, cardY + 124, 250, 36, new Color(63, 81, 181), "Booking ID: " + safe(payload == null ? null : payload.ticketRef, "-"));

            int contentX = cardX + 40;
            int contentY = cardY + 220;
            int leftW = 720;
            int rightX = contentX + leftW + 34;
            int rightW = cardX + cardW - rightX - 40;

            g.setColor(ink);
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.drawString("Passenger Details", contentX, contentY);

            int rowY = contentY + 34;
            rowY = drawField(g, contentX, rowY, leftW, "Passenger Name", payload == null ? "-" : payload.passenger);
            rowY = drawField(g, contentX, rowY, leftW, "Phone", payload == null ? "-" : payload.phone);
            rowY = drawField(g, contentX, rowY, leftW, "Email", payload == null ? "-" : payload.email);
            rowY += 10;

            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.setColor(ink);
            g.drawString("Journey Details", contentX, rowY);
            rowY += 34;
            rowY = drawField(g, contentX, rowY, leftW, "Route", payload == null ? "-" : payload.route);
            rowY = drawField(g, contentX, rowY, leftW, "Service", payload == null ? "-" : payload.bus);
            rowY = drawField(g, contentX, rowY, leftW, "Seat Number(s)", payload == null ? "-" : payload.seats);
            rowY = drawField(g, contentX, rowY, leftW, "Journey Time", payload == null ? "-" : payload.date);
            rowY = drawField(g, contentX, rowY, leftW, "Ticket Fare", payload == null ? "-" : "INR " + safe(payload.amount, "0"));

            int qrBoxY = contentY + 8;
            int qrBoxH = 270;
            g.setColor(new Color(248, 249, 250));
            g.fillRoundRect(rightX, qrBoxY, rightW, qrBoxH, 24, 24);
            g.setColor(border);
            g.drawRoundRect(rightX, qrBoxY, rightW, qrBoxH, 24, 24);
            g.setColor(ink);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.drawString("Quick Access", rightX + 22, qrBoxY + 36);
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g.drawString("Ticket preview generated successfully.", rightX + 22, qrBoxY + 72);
            g.drawString("Use the actions below to open", rightX + 22, qrBoxY + 100);
            g.drawString("full screen or download the PDF.", rightX + 22, qrBoxY + 128);

            g.setColor(new Color(232, 245, 233));
            g.fillRoundRect(rightX + 22, qrBoxY + 160, rightW - 44, 74, 18, 18);
            g.setColor(new Color(46, 125, 50));
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Download Ticket saves the original PDF.", rightX + 32, qrBoxY + 203);

            int noteY = Math.max(rowY, qrBoxY + qrBoxH) + 34;
            g.setColor(ink);
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.drawString("Passenger Instructions", contentX, noteY);
            noteY += 28;
            noteY = drawNote(g, contentX, noteY, "1. Carry valid government ID proof for verification.");
            noteY = drawNote(g, contentX, noteY, "2. Report at boarding point at least 15 minutes before departure.");
            drawNote(g, contentX, noteY, "3. Keep this ticket for support, cancellation or refund requests.");

            g.setColor(muted);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.drawString("Thank you for booking with BusYatra. Have a safe journey.", contentX, cardY + cardH - 38);
        } finally {
            g.dispose();
        }

        return image;
    }

    private void drawChip(Graphics2D g, int x, int y, int w, int h, Color color, String text) {
        g.setColor(color);
        g.fillRoundRect(x, y, w, h, 18, 18);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        int textY = y + ((h - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(safe(text, "-"), x + 16, textY);
    }

    private int drawField(Graphics2D g, int x, int y, int width, String label, String value) {
        g.setColor(new Color(248, 249, 250));
        g.fillRoundRect(x, y, width, 72, 20, 20);
        g.setColor(new Color(230, 232, 236));
        g.drawRoundRect(x, y, width, 72, 20, 20);

        g.setColor(new Color(108, 117, 125));
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString(label, x + 18, y + 25);

        g.setColor(new Color(33, 37, 41));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        drawWrappedText(g, safe(value, "-"), x + 18, y + 50, width - 36, 22, 1);
        return y + 88;
    }

    private int drawNote(Graphics2D g, int x, int y, String text) {
        g.setColor(new Color(73, 80, 87));
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        drawWrappedText(g, text, x, y, 980, 24, 2);
        return y + 34;
    }

    private void drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth, int lineHeight, int maxLines) {
        if (text == null || text.isBlank()) {
            g.drawString("-", x, y);
            return;
        }

        FontMetrics metrics = g.getFontMetrics();
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        int lineCount = 0;
        int drawY = y;

        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (metrics.stringWidth(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }

            g.drawString(line.toString(), x, drawY);
            lineCount++;
            if (lineCount >= maxLines) {
                return;
            }

            line.setLength(0);
            line.append(word);
            drawY += lineHeight;
        }

        if (line.length() > 0 && lineCount < maxLines) {
            g.drawString(line.toString(), x, drawY);
        }
    }

    private File createWorkingPdfFile(String ticketRef) throws Exception {
        Path dir = Path.of(System.getProperty("java.io.tmpdir"), "BusYatra", "tickets");
        Files.createDirectories(dir);
        return dir.resolve("ticket_" + safeFilePart(ticketRef) + ".pdf").toFile();
    }

    private static class PreviewPayload {
        private int ticketId;
        private List<Integer> bookingIds = new ArrayList<>();
        private String ticketRef = "";
        private File file;
        private String metaHtml = " ";
        private String fallbackText = "";
        private String errorMessage;
        private String statusMessage;
        private String passenger;
        private String route;
        private String bus;
        private String seats;
        private String date;
        private String amount;
        private String phone;
        private String email;
    }

    private static class PreviewRenderResult {
        private Image image;
        private boolean usedPdfRenderer;
        private String renderError;
    }

    private void downloadTicket() {

        if (generatedFile == null && currentPreview == null) {

            JOptionPane.showMessageDialog(this, "Ticket not ready");
            return;
        }

        JFileChooser chooser = new JFileChooser();

        chooser.setSelectedFile(
                new File("BusYatra_Ticket_" +
                        safeFilePart(safe(currentTicketRef, String.valueOf(BookingContext.getPrimaryTicketId()))) + ".pdf")
        );

        if (chooser.showSaveDialog(this)
                == JFileChooser.APPROVE_OPTION) {

            try {

                File target = chooser.getSelectedFile();
                if (currentPreview != null) {
                    TicketPdfUtil.generateTicketPDF(
                            target.getAbsolutePath(),
                            currentPreview.ticketRef,
                            currentPreview.passenger,
                            currentPreview.route,
                            currentPreview.bus,
                            currentPreview.seats,
                            currentPreview.date,
                            currentPreview.amount
                    );
                } else {
                    Files.copy(
                            generatedFile.toPath(),
                            target.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Ticket saved successfully"
                );

            } catch (Exception ex) {

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

    private String safeFilePart(String value) {
        String cleaned = safe(value, "ticket").replaceAll("[^A-Za-z0-9_-]", "_");
        return cleaned.isBlank() ? "ticket" : cleaned;
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
