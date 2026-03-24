package com.dentalclinic.patient;

import javax.swing.*;
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
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        setupUI(formContainer, gbc);

        JScrollPane scroll = new JScrollPane(formContainer);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        JButton btnSave = new JButton("Save All Changes");
        btnSave.setFont(new Font("Arial", Font.BOLD, 16));
        btnSave.setBackground(new Color(41, 128, 185));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> handleUpdate());
        add(btnSave, BorderLayout.SOUTH);
    }

    private void setupUI(JPanel pnl, GridBagConstraints gbc) {
        try {
            Patient p = patientDao.getPatientById(patientID);
            if (p == null) return;

            int row = 0;
            txtFName = addField(pnl, "First Name:", p.getFirstName(), gbc, row++);
            txtMName = addField(pnl, "Middle Name:", p.getMiddleName(), gbc, row++);
            txtLName = addField(pnl, "Last Name:", p.getLastName(), gbc, row++);

            // --- JCalendar Integration ---
            gbc.gridx = 0; gbc.gridy = row; pnl.add(new JLabel("Birth Date:"), gbc);
            birthDatePicker = new JDateChooser();
            birthDatePicker.setDateFormatString("MMMM d, yyyy");
            birthDatePicker.setDate(p.getBirthDate());
            gbc.gridx = 1; pnl.add(birthDatePicker, gbc);
            row++;

            // --- Age Sync Logic ---
            gbc.gridx = 0; gbc.gridy = row; pnl.add(new JLabel("Age:"), gbc);
            txtAge = new JTextField(String.valueOf(p.getAge()));
            txtAge.setEditable(false);
            txtAge.setBackground(new Color(230, 230, 230));
            gbc.gridx = 1; pnl.add(txtAge, gbc);
            row++;

            birthDatePicker.addPropertyChangeListener("date", evt -> {
                if (birthDatePicker.getDate() != null) {
                    txtAge.setText(String.valueOf(calculateAge(birthDatePicker.getDate())));
                }
            });

            // --- Other Fields ---
            txtAddr = addField(pnl, "Full Address:", p.getAddress(), gbc, row++);
            txtPhone = addField(pnl, "Contact No:", p.getContactNumber(), gbc, row++);
            
            // Assuming we added Email to Patient model or we fetch it here
            txtEmail = addField(pnl, "Email Address:", p.getEmail(), gbc, row++);
            txtUser = addField(pnl, "Username:", p.getUsername(), gbc, row++);

            // --- Security Section ---
            gbc.gridy = row++; gbc.gridwidth = 2;
            JLabel lblSec = new JLabel("--- Security (Verify Current Password to Save) ---");
            lblSec.setForeground(Color.RED);
            pnl.add(lblSec, gbc);
            gbc.gridwidth = 1;

            txtCurrentPass = addPassField(pnl, "Current Password:", gbc, row++);
            txtNewPass = addPassField(pnl, "New Password (Optional):", gbc, row++);
            txtConfirmPass = addPassField(pnl, "Confirm New Password:", gbc, row++);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private JTextField addField(JPanel p, String lbl, String val, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel(lbl), gbc);
        gbc.gridx = 1; JTextField t = new JTextField(val, 20); p.add(t, gbc);
        return t;
    }

    private JPasswordField addPassField(JPanel p, String lbl, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel(lbl), gbc);
        gbc.gridx = 1; JPasswordField t = new JPasswordField(20); p.add(t, gbc);
        return t;
    }

    private int calculateAge(java.util.Date birthDate) {
        java.time.LocalDate birth = new java.sql.Date(birthDate.getTime()).toLocalDate();
        return java.time.Period.between(birth, java.time.LocalDate.now()).getYears();
    }

    private void handleUpdate() {
        String currentPass = new String(txtCurrentPass.getPassword());
        String newPass = new String(txtNewPass.getPassword());
        String confirmPass = new String(txtConfirmPass.getPassword());

        try {
            // Validate Current Password FIRST
            if (!patientDao.verifyPassword(patientID, currentPass)) {
                JOptionPane.showMessageDialog(this, "Verification Failed: Current password is incorrect.", "Security", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Optional Password Change Logic
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