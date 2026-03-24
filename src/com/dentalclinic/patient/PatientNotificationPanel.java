package com.dentalclinic.patient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dentalclinic.dao.AppointmentDAO;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.ui.PatientDashboard;

public class PatientNotificationPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private int patientID;
    private PatientDashboard dashboard; 
    private JButton clearAllBtn;
    
    // NEW COMPONENTS FOR EMPTY STATE
    private JPanel cards; // The container that switches views
    private CardLayout cardLayout;
    private static final String TABLE_VIEW = "TABLE";
    private static final String EMPTY_VIEW = "EMPTY";

    public PatientNotificationPanel(int patientID, PatientDashboard dashboard) {
        this.patientID = patientID;
        this.dashboard = dashboard; 
        
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(236, 240, 241));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- TOP PANEL ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        
        JLabel title = new JLabel("Notifications");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(39, 174, 96));
        topPanel.add(title, BorderLayout.WEST);
        
        clearAllBtn = new JButton("Clear All");
        clearAllBtn.setFocusPainted(false);
        clearAllBtn.addActionListener(e -> clearAllNotifications());
        topPanel.add(clearAllBtn, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);

        // --- CENTER CARDS SETUP ---
        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.setOpaque(false);

        // 1. THE TABLE VIEW
        String[] columns = {"ID", "Message", "Status", "Date Updated", "isRead", "Action"};
        model = new DefaultTableModel(columns, 0) {
            @Override 
            public boolean isCellEditable(int r, int c) { return c == 5; }
        };

        table = new JTable(model);
        table.setRowHeight(45);
        
        // Hide technical columns
        table.getColumnModel().getColumn(0).setMinWidth(0); 
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(4).setMinWidth(0); 
        table.getColumnModel().getColumn(4).setMaxWidth(0);

        table.getColumnModel().getColumn(5).setPreferredWidth(50);
        table.getColumnModel().getColumn(5).setCellRenderer(new DeleteButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new DeleteButtonEditor(new JCheckBox()));

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedColumn() != 5) {
                    handleDoubleClick();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);
        cards.add(scrollPane, TABLE_VIEW);

        // 2. THE EMPTY VIEW
        JPanel emptyPanel = new JPanel(new GridBagLayout()); // GridBagLayout centers components easily
        emptyPanel.setBackground(Color.WHITE);
        JLabel emptyLabel = new JLabel("No new notifications at this time.");
        emptyLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        emptyLabel.setForeground(Color.GRAY);
        emptyPanel.add(emptyLabel);
        cards.add(emptyPanel, EMPTY_VIEW);

        add(cards, BorderLayout.CENTER);

        loadNotifications();
    }

    private void loadNotifications() {
        model.setRowCount(0); 
        try {
            AppointmentDAO dao = new AppointmentDAO();
            List<Appointment> list = dao.getAppointmentsByPatient(patientID);

            int visibleCount = 0;
            for (Appointment a : list) {
                if (!a.getStatus().equalsIgnoreCase("Pending") && !a.isArchived()) {
                    visibleCount++;
                    String msg = "Your " + a.getServiceType() + " appointment.";
                    if (a.getClinicalNotes() != null && !a.getClinicalNotes().isEmpty()) {
                        msg = "Update: " + a.getClinicalNotes();
                    }

                    model.addRow(new Object[]{
                        a.getAppointmentId(), 
                        msg,
                        a.getStatus(),
                        a.getAppointmentDate().toString(),
                        a.isRead(),
                        "X"
                    });
                }
            }
            
            // TOGGLE VIEW LOGIC
            if (visibleCount == 0) {
                cardLayout.show(cards, EMPTY_VIEW);
                clearAllBtn.setVisible(false); // Hide "Clear All" if nothing to clear
            } else {
                cardLayout.show(cards, TABLE_VIEW);
                clearAllBtn.setVisible(true);
            }

            applyCustomRenderer();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ... [Keep handleDoubleClick, applyCustomRenderer, and clearAllNotifications same as your current code] ...

    private void handleDoubleClick() {
        int row = table.getSelectedRow();
        if (row != -1) {
            int appId = (int) model.getValueAt(row, 0); 
            String msg = (String) model.getValueAt(row, 1);
            String status = (String) model.getValueAt(row, 2);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            String htmlMsg = "<html><body style='width: 250px;'>" +
                             "<h3>Appointment Update</h3>" +
                             "<p><b>Message:</b> " + msg + "</p>" +
                             "<p><b>Status:</b> " + status + "</p>" +
                             "</body></html>";
            
            panel.add(new JLabel(htmlMsg), BorderLayout.CENTER);
            JOptionPane.showMessageDialog(this, panel, "Notification Detail", JOptionPane.INFORMATION_MESSAGE);

            try {
                AppointmentDAO dao = new AppointmentDAO();
                if (dao.markAsRead(appId)) {
                    model.setValueAt(true, row, 4);
                    if (dashboard != null) dashboard.refreshNotificationBadge();
                    table.repaint();
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private void applyCustomRenderer() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                if (column == 5) return c;

                boolean isRead = (boolean) table.getModel().getValueAt(modelRow, 4); 
                if (!isRead) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                    c.setForeground(Color.BLACK);
                    if (!isSelected) c.setBackground(new Color(235, 245, 255));
                } else {
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                    c.setForeground(Color.DARK_GRAY);
                    if (!isSelected) c.setBackground(Color.WHITE);
                }
                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                }
                return c;
            }
        });
    }
    
    private void clearAllNotifications() {
        int confirm = JOptionPane.showConfirmDialog(this, "Clear all notifications?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                new AppointmentDAO().archiveAllNotifications(patientID);
                loadNotifications();
                if (dashboard != null) dashboard.refreshNotificationBadge();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    // --- INNER CLASSES (Keep these as they are) ---
    class DeleteButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public DeleteButtonRenderer() { 
            setText("X"); 
            setForeground(Color.RED);
            setFont(new Font("Arial", Font.BOLD, 12));
            setBorderPainted(false);
            setContentAreaFilled(false);
        }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    class DeleteButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private int currentId;

        public DeleteButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton("X");
            button.setForeground(Color.RED);
            button.addActionListener(e -> {
                try {
                    new AppointmentDAO().archiveNotification(currentId);
                    fireEditingStopped(); 
                    loadNotifications();
                    if (dashboard != null) dashboard.refreshNotificationBadge();
                } catch (Exception ex) { ex.printStackTrace(); }
            });
        }
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentId = (int) table.getModel().getValueAt(row, 0);
            return button;
        }
    }
}