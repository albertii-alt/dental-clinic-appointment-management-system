package com.dentalclinic.patient;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import com.toedter.calendar.JDateChooser;
import com.dentalclinic.model.Patient;
import com.dentalclinic.dao.PatientDAO;

public class PatientProfilePanel extends JPanel {
    private JTextField txtFName, txtMName, txtLName, txtAge, txtAddr, txtPhone, txtEmail, txtUser;
    private JDateChooser birthDatePicker;
    private JPasswordField txtCurrentPass, txtNewPass, txtConfirmPass;
    private PatientDAO patientDao = new PatientDAO();
    private int patientID;

    public PatientProfilePanel(int pID) {
        this.patientID = pID;
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250)); // Slightly cleaner off-white
        setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        // --- HEADER ---
        JLabel header = new JLabel("My Profile Settings");
        header.setFont(new Font("Segoe UI", Font.BOLD, 26));
        header.setForeground(new Color(44, 62, 80));
        add(header, BorderLayout.NORTH);

        // --- FORM CONTAINER ---
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);

        setupUI(formContainer);

        JScrollPane scroll = new JScrollPane(formContainer);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        // --- SAVE BUTTON ---
        JButton btnSave = new JButton("Save All Changes");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnSave.setBackground(new Color(41, 128, 185));
        btnSave.setForeground(Color.WHITE);
        btnSave.setPreferredSize(new Dimension(0, 50));
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> handleUpdate());
        add(btnSave, BorderLayout.SOUTH);
    }

    private void setupUI(JPanel container) {
        try {
            Patient p = patientDao.getPatientById(patientID);
            if (p == null) return;

            // 1. GENERAL INFORMATION SECTION
            JPanel generalPnl = createSection("General Information");
            GridBagConstraints gbc = createGBC();

            txtFName = addField(generalPnl, "First Name:", p.getFirstName(), gbc, 0);
            txtMName = addField(generalPnl, "Middle Name:", p.getMiddleName(), gbc, 1);
            txtLName = addField(generalPnl, "Last Name:", p.getLastName(), gbc, 2);

            // JDateChooser Row
            gbc.gridx = 0; gbc.gridy = 3;
            generalPnl.add(new JLabel("Birth Date:"), gbc);
            birthDatePicker = new JDateChooser();
            birthDatePicker.setDateFormatString("MMMM d, yyyy");
            birthDatePicker.setDate(p.getBirthDate());
            gbc.gridx = 1; generalPnl.add(birthDatePicker, gbc);

            // Age Row
            gbc.gridx = 0; gbc.gridy = 4;
            generalPnl.add(new JLabel("Current Age:"), gbc);
            txtAge = new JTextField(String.valueOf(p.getAge()));
            txtAge.setEditable(false);
            txtAge.setBackground(new Color(236, 240, 241));
            gbc.gridx = 1; generalPnl.add(txtAge, gbc);

            // LOGIC: Age Sync
            birthDatePicker.addPropertyChangeListener("date", evt -> {
                if (birthDatePicker.getDate() != null) {
                    txtAge.setText(String.valueOf(calculateAge(birthDatePicker.getDate())));
                }
            });

            txtAddr = addField(generalPnl, "Full Address:", p.getAddress(), gbc, 5);
            txtPhone = addField(generalPnl, "Contact No:", p.getContactNumber(), gbc, 6);
            txtEmail = addField(generalPnl, "Email Address:", p.getEmail(), gbc, 7);
            txtUser = addField(generalPnl, "Username:", p.getUsername(), gbc, 8);

            container.add(generalPnl);
            container.add(Box.createVerticalStrut(20));

            // 2. SECURITY SECTION
            JPanel securityPnl = createSection("Account Security");
            GridBagConstraints sGbc = createGBC();

            JLabel lblSec = new JLabel("Verification required to save changes");
            lblSec.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            lblSec.setForeground(new Color(192, 57, 43));
            sGbc.gridwidth = 2; sGbc.gridy = 0; sGbc.gridx = 0;
            securityPnl.add(lblSec, sGbc);
            sGbc.gridwidth = 1;

            txtCurrentPass = addPassField(securityPnl, "Current Password:", sGbc, 1);
            txtNewPass = addPassField(securityPnl, "New Password (Optional):", sGbc, 2);
            txtConfirmPass = addPassField(securityPnl, "Confirm New Password:", sGbc, 3);

            container.add(securityPnl);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private JPanel createSection(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        TitledBorder tb = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(220, 230, 235)), title);
        tb.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        tb.setTitleColor(new Color(41, 128, 185));
        p.setBorder(new CompoundBorder(tb, new EmptyBorder(15, 20, 15, 20)));
        return p;
    }

    private GridBagConstraints createGBC() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(6, 6, 6, 6);
        return g;
    }

    private JTextField addField(JPanel p, String lbl, String val, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        p.add(new JLabel(lbl), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        JTextField t = new JTextField(val, 20);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        p.add(t, gbc);
        return t;
    }

    private JPasswordField addPassField(JPanel p, String lbl, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        p.add(new JLabel(lbl), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        JPasswordField t = new JPasswordField(20);
        p.add(t, gbc);
        return t;
    }

    // LOGIC: Functional Methods (Stay the Same)
    private int calculateAge(java.util.Date birthDate) {
        java.time.LocalDate birth = new java.sql.Date(birthDate.getTime()).toLocalDate();
        return java.time.Period.between(birth, java.time.LocalDate.now()).getYears();
    }

    private void handleUpdate() {
        String currentPass = new String(txtCurrentPass.getPassword());
        String newPass = new String(txtNewPass.getPassword());
        String confirmPass = new String(txtConfirmPass.getPassword());

        try {
            if (!patientDao.verifyPassword(patientID, currentPass)) {
                JOptionPane.showMessageDialog(this, "Verification Failed: Current password is incorrect.", "Security", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String passToSave = null;
            if (!newPass.isEmpty()) {
                if (!newPass.equals(confirmPass)) {
                    JOptionPane.showMessageDialog(this, "New passwords do not match!");
                    return;
                }
                passToSave = newPass;
            }

            java.sql.Date sqlDob = new java.sql.Date(birthDatePicker.getDate().getTime());

            boolean success = patientDao.updateFullProfile(
                patientID, txtFName.getText(), txtMName.getText(), txtLName.getText(),
                sqlDob, Integer.parseInt(txtAge.getText()), txtAddr.getText(), 
                txtPhone.getText(), txtEmail.getText(), txtUser.getText(), passToSave
            );

            if (success) JOptionPane.showMessageDialog(this, "Profile updated successfully!");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}