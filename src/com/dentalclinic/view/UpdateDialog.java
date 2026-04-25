package com.dentalclinic.view;

import com.dentalclinic.util.UpdateChecker.UpdateInfo;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class UpdateDialog extends JDialog {

    private static final Color BG           = new Color(245, 248, 252);
    private static final Color HEADER_BG    = new Color(20, 30, 48);
    private static final Color ACCENT       = new Color(41, 128, 185);
    private static final Color TEXT_DARK    = new Color(44, 62, 80);
    private static final Color TEXT_MUTED   = new Color(127, 140, 141);
    private static final Color SUCCESS      = new Color(39, 174, 96);

    private final UpdateInfo info;
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private JButton updateBtn;
    private JButton laterBtn;

    public UpdateDialog(UpdateInfo info) {
        super((Frame) null, "Update Available", true);
        this.info = info;
        initUI();
        pack();
        setSize(520, 460);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Update Available!");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        JLabel versions = new JLabel("v" + info.currentVersion + "  →  v" + info.latestVersion);
        versions.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        versions.setForeground(new Color(174, 214, 241));

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);
        headerText.add(title);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(versions);
        header.add(headerText, BorderLayout.CENTER);

        // Update badge icon
        JLabel badge = new JLabel();
        badge.setIcon(org.kordamp.ikonli.swing.FontIcon.of(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.ARROW_ALT_CIRCLE_UP, 32, new Color(41, 128, 185)));
        badge.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        header.add(badge, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // --- BODY ---
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(16, 24, 8, 24));

        JLabel notesTitle = new JLabel("What's new in v" + info.latestVersion + ":");
        notesTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        notesTitle.setForeground(TEXT_DARK);
        notesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(notesTitle);
        body.add(Box.createVerticalStrut(8));

        String notes = info.releaseNotes == null || info.releaseNotes.isBlank()
            ? "Bug fixes and performance improvements."
            : info.releaseNotes;
        JTextArea notesArea = new JTextArea(notes);
        notesArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        notesArea.setForeground(TEXT_DARK);
        notesArea.setBackground(Color.WHITE);
        notesArea.setEditable(false);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(notesArea);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        scroll.setPreferredSize(new Dimension(460, 140));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 230, 240)));
        body.add(scroll);
        body.add(Box.createVerticalStrut(16));

        // Progress area (hidden until download starts)
        progressLabel = new JLabel(" ");
        progressLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        progressLabel.setForeground(TEXT_MUTED);
        progressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(progressLabel);
        body.add(Box.createVerticalStrut(4));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        progressBar.setPreferredSize(new Dimension(460, 8));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setBackground(new Color(220, 230, 240));
        progressBar.setForeground(ACCENT);
        progressBar.setBorderPainted(false);
        progressBar.setVisible(false);
        body.add(progressBar);

        add(body, BorderLayout.CENTER);

        // --- FOOTER ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 230, 240)));

        laterBtn = new JButton("Remind Me Later");
        laterBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        laterBtn.setForeground(TEXT_MUTED);
        laterBtn.setBackground(Color.WHITE);
        laterBtn.setFocusPainted(false);
        laterBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 230, 240)),
            new EmptyBorder(8, 18, 8, 18)
        ));
        laterBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        laterBtn.addActionListener(e -> dispose());

        updateBtn = new JButton("  Update Now  ");
        updateBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        updateBtn.setForeground(Color.WHITE);
        updateBtn.setBackground(ACCENT);
        updateBtn.setFocusPainted(false);
        updateBtn.setBorderPainted(false);
        updateBtn.setOpaque(true);
        updateBtn.setBorder(new EmptyBorder(8, 18, 8, 18));
        updateBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateBtn.addActionListener(e -> startDownload());

        footer.add(laterBtn);
        footer.add(updateBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private void startDownload() {
        if (info.downloadUrl == null || info.downloadUrl.isBlank()) {
            JOptionPane.showMessageDialog(this,
                "Download URL not available. Please update manually from GitHub.",
                "Update", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        updateBtn.setEnabled(false);
        laterBtn.setEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        progressLabel.setText("Downloading update...");

        new Thread(() -> {
            try {
                File tempFile = Files.createTempFile("DentalClinicUpdate-", ".exe").toFile();
                tempFile.deleteOnExit();

                InputStream in = openWithRedirects(info.downloadUrl);
                if (in == null) throw new IOException("Failed to open download stream");

                try (InputStream is = in; FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int read, downloaded = 0;
                    while ((read = is.read(buf)) != -1) {
                        out.write(buf, 0, read);
                        downloaded += read;
                        final int dl = downloaded / 1024;
                        SwingUtilities.invokeLater(() -> progressLabel.setText("Downloading... " + dl + " KB"));
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressLabel.setForeground(SUCCESS);
                    progressLabel.setText("Download complete! Launching installer...");
                });

                Thread.sleep(800);
                Runtime.getRuntime().exec(new String[]{ tempFile.getAbsolutePath() });

                SwingUtilities.invokeLater(() -> {
                    dispose();
                    System.exit(0);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    progressLabel.setForeground(Color.RED);
                    progressLabel.setText("Download failed: " + ex.getMessage());
                    updateBtn.setEnabled(true);
                    laterBtn.setEnabled(true);
                });
            }
        }).start();
    }

    private InputStream openWithRedirects(String urlStr) throws IOException {
        int maxRedirects = 10;
        String currentUrl = urlStr;
        for (int i = 0; i < maxRedirects; i++) {
            HttpURLConnection conn = (HttpURLConnection) new URL(currentUrl).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "DentalClinicUpdater");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(300000);
            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                return conn.getInputStream();
            } else if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == 307 || status == 308) {
                currentUrl = conn.getHeaderField("Location");
                conn.disconnect();
                if (currentUrl == null) throw new IOException("Redirect with no Location header");
            } else {
                throw new IOException("HTTP " + status + " from " + currentUrl);
            }
        }
        throw new IOException("Too many redirects");
    }
}
