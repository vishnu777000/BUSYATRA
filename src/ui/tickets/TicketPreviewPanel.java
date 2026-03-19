package ui.tickets;

import config.UIConfig;
import dao.TicketDAO;
import ui.common.MainFrame;
import util.BookingContext;
import util.Refreshable;
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
    private File generatedFile;

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

        JLabel title = new JLabel("Booking Successful 🎉",SwingConstants.CENTER);
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

        pdfLabel = new JLabel("Generating ticket preview...",SwingConstants.CENTER);

        JScrollPane scroll = new JScrollPane(pdfLabel);
        scroll.setBorder(null);

        card.add(scroll);

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

        back.addActionListener(e -> {
            BookingContext.clear();
            frame.showScreen(MainFrame.SCREEN_USER);
        });

        download.addActionListener(e -> downloadTicket());

        p.add(back,BorderLayout.WEST);
        p.add(download,BorderLayout.EAST);

        return p;
    }

    /* ================= REFRESH ================= */

    @Override
    public void refreshData(){

        pdfLabel.setIcon(null);
        pdfLabel.setText("Generating ticket preview...");

        try{

            int ticketId = BookingContext.ticketId;

            /* ✅ FIXED: NO RESULTSET */
            String[] data =
                    new TicketDAO().getTicketFullDetails(ticketId);

            if(data == null){

                pdfLabel.setText("Ticket data not found");
                return;
            }

            String passenger = data[0];
            String route     = data[1];
            String bus       = data[2] + " | " + data[3];
            String seats     = data[4];
            String date      = data[5];
            String amount    = data[6];

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

            showPdfPreview();

        }catch(Exception e){

            e.printStackTrace();
            pdfLabel.setText("Error loading ticket preview");
        }
    }

    /* ================= SHOW PDF ================= */

    private void showPdfPreview() {

        try(PDDocument doc = Loader.loadPDF(generatedFile)) {

            PDFRenderer renderer = new PDFRenderer(doc);

            Image img = renderer.renderImageWithDPI(0,150);

            pdfLabel.setIcon(new ImageIcon(img));
            pdfLabel.setText("");

        }catch(Exception e){

            pdfLabel.setText("Unable to render ticket preview");
            e.printStackTrace();
        }
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
}