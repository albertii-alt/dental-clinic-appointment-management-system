package com.dentalclinic.patient;

import com.dentalclinic.controller.AppointmentController;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.awt.event.*;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.ui.PatientDashboard;

public class PatientNotificationPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private int patientID;
    private PatientDashboard dashboard; 
    private JButton clearAllBtn;
    
    private JPanel cards; 
    private CardLayout cardLayout;
    private static final String TABLE_VIEW = "TABLE";
    private static final String EMPTY_VIEW = "EMPTY";
    private final AppointmentController appointmentController = new AppointmentController();

    public PatientNotificationPanel(int patientID, PatientDashboard dashboard) {
        this.patientID = patientID;
        this.dashboard = dashboard; 
        
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250));
        setBorder(new EmptyBorder(25, 30, 25, 30));

        // --- TOP PANEL ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        
        JLabel title = new JLabel("Notifications");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(44, 62, 80));
        topPanel.add(title, BorderLayout.WEST);
        
        clearAllBtn = new JButton("Clear All");
        clearAllBtn.setBackground(new Color(189, 195, 199));
        clearAllBtn.setForeground(Color.WHITE);
        clearAllBtn.setFocusPainted(false);
        clearAllBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        clearAllBtn.addActionListener(e -> clearAllNotifications());
        topPanel.add(clearAllBtn, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);

        // --- CENTER CARDS ---
        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.setOpaque(false);

        // 1. TABLE VIEW
        String[] columns = {"ID", "Message", "Status", "Date", "isRead", "Action"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5; }
        };

        table = new JTable(model);
        styleTable(table);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedColumn() != 5) {
                    handleDoubleClick();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(230, 230, 230)));
        cards.add(scrollPane, TABLE_VIEW);

        // 2. EMPTY VIEW
        JPanel emptyPanel = new JPanel(new GridBagLayout());
        emptyPanel.setBackground(Color.WHITE);
        emptyPanel.setBorder(new LineBorder(new Color(230, 230, 230)));
        JLabel emptyLabel = new JLabel("<html><center><font size='5' color='#bdc3c7'>No notifications</font><br><font color='#bdc3c7'>You're all caught up!</font></center></html>");
        emptyPanel.add(emptyLabel);
        cards.add(emptyPanel, EMPTY_VIEW);

        add(cards, BorderLayout.CENTER);
        loadNotifications();
    }

    private void styleTable(JTable table) {
        table.setRowHeight(55);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        
        // Hide technical columns
        table.getColumnModel().getColumn(0).setMinWidth(0); 
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(4).setMinWidth(0); 
        table.getColumnModel().getColumn(4).setMaxWidth(0);

        // Setup Action Column (The Trash Button)
        table.getColumnModel().getColumn(5).setPreferredWidth(60);
        table.getColumnModel().getColumn(5).setCellRenderer(new DeleteButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new DeleteButtonEditor(new JCheckBox()));

        applyCustomRenderer();
    }

    private void loadNotifications() {
        model.setRowCount(0); 
        try {
            List<Appointment> list = appointmentController.getAppointmentsByPatient(patientID);
            int count = 0;
            for (Appointment a : list) {
                if (!a.getStatus().equalsIgnoreCase("Pending") && !a.isArchived()) {
                    count++;
                    String msg = "Your " + a.getServiceType() + " appointment is " + a.getStatus() + ".";
                    model.addRow(new Object[]{a.getAppointmentId(), msg, a.getStatus(), a.getAppointmentDate(), a.isRead(), "REMOVE"});
                }
            }
            cardLayout.show(cards, (count == 0) ? EMPTY_VIEW : TABLE_VIEW);
            clearAllBtn.setVisible(count > 0);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleDoubleClick() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        
        int appId = (int) model.getValueAt(row, 0);
        String msg = (String) model.getValueAt(row, 1);
        
        JOptionPane.showMessageDialog(this, msg, "Notification Detail", JOptionPane.INFORMATION_MESSAGE);

        try {
            if (appointmentController.markNotificationAsRead(appId)) {
                model.setValueAt(true, row, 4);
                if (dashboard != null) dashboard.refreshNotificationBadge();
                table.repaint();
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void applyCustomRenderer() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSel, boolean hasF, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSel, hasF, row, col);
                boolean isRead = (boolean) table.getModel().getValueAt(table.convertRowIndexToModel(row), 4);
                
                if (!isRead) {
                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    if (!isSel) c.setBackground(new Color(235, 245, 255));
                } else {
                    c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    if (!isSel) c.setBackground(Color.WHITE);
                }
                
                ((JLabel)c).setBorder(new EmptyBorder(0, 15, 0, 15));
                return c;
            }
        });
    }

    private void clearAllNotifications() {
        if (JOptionPane.showConfirmDialog(this, "Dismiss all notifications?", "Confirm", JOptionPane.YES_NO_OPTION) == 0) {
            try {
                appointmentController.archiveAllNotifications(patientID);
                loadNotifications();
                if (dashboard != null) dashboard.refreshNotificationBadge();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    // --- CUSTOM ACTION BUTTONS ---
    class DeleteButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton btn = new JButton("×"); 
        public DeleteButtonRenderer() {
            setOpaque(true);
            setLayout(new GridBagLayout());
            btn.setPreferredSize(new Dimension(30, 30));
            btn.setFont(new Font("Arial", Font.BOLD, 18));
            btn.setForeground(new Color(231, 76, 60));
            btn.setFocusable(false);
            btn.setBorder(new LineBorder(new Color(231, 76, 60), 1, true));
            btn.setContentAreaFilled(false);
            add(btn);
        }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean isSel, boolean hasF, int r, int c) {
            setBackground(isSel ? t.getSelectionBackground() : t.getBackground());
            return this;
        }
    }

    class DeleteButtonEditor extends DefaultCellEditor {
        private JPanel panel = new JPanel(new GridBagLayout());
        private JButton btn = new JButton("×");
        private int currentId;

        public DeleteButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            btn.setPreferredSize(new Dimension(30, 30));
            btn.setForeground(Color.RED);
            btn.addActionListener(e -> {
                try {
                    appointmentController.archiveNotification(currentId);
                    fireEditingStopped();
                    loadNotifications();
                    if (dashboard != null) dashboard.refreshNotificationBadge();
                } catch (Exception ex) { ex.printStackTrace(); }
            });
            panel.add(btn);
        }
        public Component getTableCellEditorComponent(JTable t, Object v, boolean isSel, int r, int c) {
            currentId = (int) t.getModel().getValueAt(t.convertRowIndexToModel(r), 0);
            return panel;
        }
    }
}
