package com.dentalclinic.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import com.dentalclinic.model.Patient;
import com.dentalclinic.service.AuthService;
import com.dentalclinic.dao.RolesPermissionDAO;
import com.dentalclinic.dao.PatientDAO;
import com.dentalclinic.dao.StaffDAO;
import com.dentalclinic.util.UserSession;
import com.dentalclinic.util.DBConnection;
import com.dental.clinic.ui.components.SuccessDialog;
import com.dental.clinic.ui.components.ErrorDialog;
import com.dentalclinic.util.PasswordUtil;
import com.dentalclinic.util.PasswordValidator;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.InputStream;

public class LoginPage extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleDropdown;
    private JButton loginButton, registerButton;
    
    // DAO instances
    private PatientDAO patientDAO;
    private StaffDAO staffDAO;

    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color SECONDARY_BLUE = new Color(52, 152, 219);
    private final Color SIDEBAR_BG = new Color(242, 245, 248); 
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color TEXT_GRAY = new Color(127, 140, 141);
    private final Color BORDER_COLOR = new Color(218, 226, 234);

    public LoginPage() {
        // Initialize DAOs
        patientDAO = new PatientDAO();
        staffDAO = new StaffDAO();
        
        setTitle("Vantage Dental - Login");
        if (!com.dentalclinic.util.DBConnection.testConnection()) {
            // Database not configured - show setup wizard
            com.dentalclinic.util.DatabaseSetupWizard.showSetupWizard(this);
            return;
        }
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel masterPanel = new JPanel(new GridLayout(1, 2));
        add(masterPanel);

        // --- Sidebar (Left) ---
        JPanel sidebar = new JPanel(new GridBagLayout());
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setBorder(new EmptyBorder(50, 50, 50, 50));
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0; gbcL.fill = GridBagConstraints.HORIZONTAL; gbcL.anchor = GridBagConstraints.NORTHWEST;

        JLabel name = new JLabel("Vantage Dental");
        name.setFont(new Font("Segoe UI", Font.BOLD, 36));
        name.setForeground(PRIMARY_BLUE);
        gbcL.gridy = 0; sidebar.add(name, gbcL);

        JLabel sub = new JLabel("Appointment Portal");
        sub.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 20));
        sub.setForeground(TEXT_DARK);
        gbcL.gridy = 1; gbcL.insets = new Insets(5, 0, 0, 0);
        sidebar.add(sub, gbcL);

        gbcL.gridy = 2; gbcL.weighty = 1.0; sidebar.add(Box.createVerticalGlue(), gbcL);

        JLabel logo = loadLogo("/com/dentalclinic/resources/VantageLogo.png", 350, -1);
        if (logo != null) {
            gbcL.gridy = 3; gbcL.weighty = 0; gbcL.anchor = GridBagConstraints.CENTER;
            sidebar.add(logo, gbcL);
        }

        gbcL.gridy = 4; gbcL.weighty = 1.0; sidebar.add(Box.createVerticalGlue(), gbcL);
        
        // --- Enhanced Footer Section ---
        JPanel footerContainer = new JPanel(new BorderLayout(15, 0));
        footerContainer.setOpaque(false);

        // Modern Accent Bar
        JPanel accentBar = new JPanel();
        accentBar.setPreferredSize(new Dimension(4, 0));
        accentBar.setBackground(PRIMARY_BLUE);
        footerContainer.add(accentBar, BorderLayout.WEST);

        // Text Content
        JLabel footerText = new JLabel("<html><div style='font-family: Segoe UI;'>" +
                "<b style='font-size: 14px; color: " + String.format("#%02x%02x%02x", TEXT_DARK.getRed(), TEXT_DARK.getGreen(), TEXT_DARK.getBlue()) + ";'>Manage Your Oral Health</b><br>" +
                "<span style='font-size: 11px; color: " + String.format("#%02x%02x%02x", TEXT_GRAY.getRed(), TEXT_GRAY.getGreen(), TEXT_GRAY.getBlue()) + ";'>Log in to view clinic schedules, treatment<br>history, and digital prescriptions.</span></div></html>");

        footerContainer.add(footerText, BorderLayout.CENTER);

        gbcL.gridy = 5; 
        gbcL.weighty = 0; 
        gbcL.insets = new Insets(20, 0, 0, 0);
        gbcL.anchor = GridBagConstraints.SOUTHWEST;
        sidebar.add(footerContainer, gbcL);

        masterPanel.add(sidebar);

        // --- Form (Right) ---
        JPanel formArea = new JPanel(new GridBagLayout());
        formArea.setBackground(Color.WHITE);
        formArea.setBorder(new EmptyBorder(40, 70, 40, 70));
        GridBagConstraints gbcR = new GridBagConstraints();
        gbcR.gridx = 0; gbcR.fill = GridBagConstraints.HORIZONTAL; gbcR.weightx = 1.0;

        JLabel title = new JLabel("Welcome Back!");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(TEXT_DARK);
        gbcR.gridy = 0; formArea.add(title, gbcR);

        JLabel subtitle = new JLabel("Please enter your details to sign in.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(TEXT_GRAY);
        gbcR.gridy = 1; gbcR.insets = new Insets(8, 0, 35, 0);
        formArea.add(subtitle, gbcR);

        addInputSection(formArea, "USERNAME", usernameField = new JTextField(), gbcR, 2);
        addInputSection(formArea, "PASSWORD", passwordField = new JPasswordField(), gbcR, 4);

        gbcR.gridy = 6; gbcR.insets = new Insets(0, 0, 5, 0);
        formArea.add(createFieldLabel("SIGN IN AS"), gbcR);
        roleDropdown = new JComboBox<>(new String[]{"Patient", "Staff", "Dentist", "Admin"});
        roleDropdown.setPreferredSize(new Dimension(0, 45));
        roleDropdown.setBackground(Color.WHITE);
        gbcR.gridy = 7; gbcR.insets = new Insets(0, 0, 35, 0);
        formArea.add(roleDropdown, gbcR);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btns.setOpaque(false);
        loginButton = new JButton("Login");
        styleFormButton(loginButton, SECONDARY_BLUE, Color.WHITE);
        loginButton.setPreferredSize(new Dimension(140, 50));
        registerButton = new JButton("Create Account");
        styleFormButton(registerButton, Color.WHITE, TEXT_DARK);
        registerButton.setBorder(new LineBorder(BORDER_COLOR));
        registerButton.setPreferredSize(new Dimension(160, 50));
        btns.add(loginButton); btns.add(Box.createHorizontalStrut(15)); btns.add(registerButton);

        gbcR.gridy = 8; gbcR.insets = new Insets(0, 0, 0, 0);
        formArea.add(btns, gbcR);

        masterPanel.add(formArea);
        initActionListeners();
        setVisible(true);
    }

    private void addInputSection(JPanel p, String label, JTextField field, GridBagConstraints c, int row) {
        c.gridy = row; c.insets = new Insets(0, 0, 5, 0);
        p.add(createFieldLabel(label), c);
        styleInputField(field);
        c.gridy = row + 1; c.insets = new Insets(0, 0, 20, 0);
        p.add(field, c);
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI Bold", Font.BOLD, 11));
        lbl.setForeground(TEXT_GRAY);
        return lbl;
    }

    private void styleInputField(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        f.setPreferredSize(new Dimension(0, 45));
        Border n = BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR), BorderFactory.createEmptyBorder(5, 15, 5, 15));
        Border a = BorderFactory.createCompoundBorder(new LineBorder(SECONDARY_BLUE, 1), BorderFactory.createEmptyBorder(5, 15, 5, 15));
        f.setBorder(n);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.setBorder(a); }
            public void focusLost(FocusEvent e) { f.setBorder(n); }
        });
    }

    private void styleFormButton(JButton b, Color bg, Color fg) {
        b.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(bg != Color.WHITE ? bg.darker() : new Color(250, 250, 250)); }
            public void mouseExited(MouseEvent e) { b.setBackground(bg); }
        });
    }

    private void initActionListeners() {
        loginButton.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());
            String role = (String) roleDropdown.getSelectedItem();

            AuthService authService = new AuthService();
            RolesPermissionDAO rpDao = new RolesPermissionDAO();

            try {
                Object result = authService.login(user, pass, role);
                
                // NEW: Check if account is locked
                if (result instanceof Object[] && ((Object[]) result)[0].equals("ACCOUNT_LOCKED")) {
                    Object[] lockData = (Object[]) result;
                    int remainingMinutes = (int) lockData[1];

                    String message = "Your account has been locked due to multiple failed login attempts.\n" +
                                    "Please try again in " + remainingMinutes + " minute(s).";
                    ErrorDialog.show(this, "Account Locked", message);
                    return;
                }

                // Check if password reset is required
                if (result instanceof Object[] && ((Object[]) result)[0].equals("RESET_REQUIRED")) {
                    Object[] resetData = (Object[]) result;
                    Object userData = resetData[1];
                    showPasswordResetDialog(userData, role);
                    return;
                }
                // Check if password reset is required
                if (result instanceof Object[] && ((Object[]) result)[0].equals("RESET_REQUIRED")) {
                    Object[] resetData = (Object[]) result;
                    Object userData = resetData[1];
                    
                    // Show password reset dialog
                    showPasswordResetDialog(userData, role);
                    return;
                }

                if (result instanceof Object[]) {
                    Object[] data = (Object[]) result;
                    int id = (int) data[0]; 
                    String rStr = (String) data[1];
                    boolean isS = (boolean) data[2]; 
                    String name = (String) data[3];
                    String email = (data.length > 4) ? (String) data[4] : "No Email";

                    int rId = rStr.equalsIgnoreCase("ADMIN") ? 1 : (rStr.equalsIgnoreCase("DENTIST") ? 2 : 3);
                    UserSession.initialize(id, name, isS ? "Super Admin" : rStr, rpDao.getPermissionNamesForRole(rId));

                    SuccessDialog.show(this, "Access Granted", "Welcome back, " + name + "!");
                    
                    if (rStr.equalsIgnoreCase("ADMIN")) new AdminDashboard(id, isS, name, email, user);
                    else if (rStr.equalsIgnoreCase("DENTIST")) new DentistDashboard(id, name, user, email);
                    else if (rStr.equalsIgnoreCase("STAFF")) new StaffDashboard(id, name, user, email);
                    dispose();
                } 
                else if (result instanceof Patient) {
                    Patient p = (Patient) result;
                    UserSession.initialize(p.getPatientId(), p.getFirstName() + " " + p.getLastName(), "PATIENT", null);
                    SuccessDialog.show(this, "Welcome Back!", "Logging you in, " + p.getFirstName());
                    new PatientDashboard(p.getPatientId(), p.getFirstName(), p.getMiddleName(), p.getLastName(), p.getBirthDate().toString(), String.valueOf(p.getAge()), p.getAddress(), p.getContactNumber(), p.getUsername());
                    dispose();
                } 
                else {
                    ErrorDialog.show(this, "Login Failed", "The username or password you entered is incorrect for the selected role.");
                }
            } catch (SQLException ex) {
                ErrorDialog.show(this, "Database Error", "Unable to connect to the clinic server: " + ex.getMessage());
            }
        });

        registerButton.addActionListener(e -> {
            new com.dentalclinic.patient.RegisterPatientForm();
            dispose();
        });
    }

    private JLabel loadLogo(String path, int w, int h) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) return null;
            return new JLabel(new ImageIcon(ImageIO.read(is).getScaledInstance(w, h, Image.SCALE_SMOOTH)));
        } catch (Exception e) { return null; }
    }
    
    private void showPasswordResetDialog(Object userData, String role) {
        JDialog dialog = new JDialog(this, "Password Reset Required", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 20, 10, 20);

        // Warning message
        JLabel warningLabel = new JLabel("<html><center><b>Security Notice</b><br>For security reasons, you must change your password.<br>This is required once due to system security upgrade.</center></html>");
        warningLabel.setForeground(new Color(255, 100, 100));
        gbc.gridy = 0;
        dialog.add(warningLabel, gbc);

        // New password field
        gbc.gridy = 1;
        dialog.add(new JLabel("New Password:"), gbc);

        JPasswordField newPassField = new JPasswordField();
        newPassField.setPreferredSize(new Dimension(300, 30));
        gbc.gridy = 2;
        dialog.add(newPassField, gbc);

        // Confirm password field
        gbc.gridy = 3;
        dialog.add(new JLabel("Confirm Password:"), gbc);

        JPasswordField confirmPassField = new JPasswordField();
        confirmPassField.setPreferredSize(new Dimension(300, 30));
        gbc.gridy = 4;
        dialog.add(confirmPassField, gbc);

        // Requirements label
        JLabel reqLabel = new JLabel("<html><small>Password must be at least 6 characters</small></html>");
        reqLabel.setForeground(Color.GRAY);
        gbc.gridy = 5;
        dialog.add(reqLabel, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton resetButton = new JButton("Reset Password");
        JButton cancelButton = new JButton("Logout");

        resetButton.addActionListener(evt -> {
        String newPass = new String(newPassField.getPassword());
        String confirmPass = new String(confirmPassField.getPassword());

        // Validate password complexity
        List<String> passwordErrors = PasswordValidator.validatePassword(newPass);
        if (!passwordErrors.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("Password requirements not met:\n");
            for (String error : passwordErrors) {
                errorMsg.append("• ").append(error).append("\n");
            }
            ErrorDialog.show(LoginPage.this, "Invalid Password", errorMsg.toString());
            return;
        }

        if (!newPass.equals(confirmPass)) {
            ErrorDialog.show(LoginPage.this, "Error", "Passwords do not match");
            return;
        }

        try {
            boolean success = false;

            if (role.equalsIgnoreCase("Patient")) {
                Patient patient = (Patient) userData;
                String hashedPass = PasswordUtil.hashPassword(newPass);
                success = updatePatientPassword(patient.getPatientId(), hashedPass);
                if (success) {
                    patientDAO.clearPasswordResetFlag(patient.getPatientId());
                }
            } else {
                Object[] staffData = (Object[]) userData;
                int staffId = (int) staffData[0];
                String hashedPass = PasswordUtil.hashPassword(newPass);
                success = updateStaffPassword(staffId, hashedPass);
                if (success) {
                    staffDAO.clearPasswordResetFlag(staffId);
                }
            }

            if (success) {
                SuccessDialog.show(LoginPage.this, "Success", "Password updated successfully! Please login again.");
                dialog.dispose();
            } else {
                ErrorDialog.show(LoginPage.this, "Error", "Failed to update password. Please try again.");
            }
        } catch (SQLException ex) {
            ErrorDialog.show(LoginPage.this, "Database Error", ex.getMessage());
        }
    });

        cancelButton.addActionListener(evt -> {
            dialog.dispose();
            // Clear the login fields
            usernameField.setText("");
            passwordField.setText("");
        });

        buttonPanel.add(resetButton);
        buttonPanel.add(cancelButton);
        gbc.gridy = 6;
        dialog.add(buttonPanel, gbc);

        dialog.setVisible(true);
    }

    // Helper methods for password update
    private boolean updatePatientPassword(int patientId, String hashedPassword) throws SQLException {
        String query = "UPDATE patients SET password = ?, force_password_reset = 0 WHERE patient_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setInt(2, patientId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private boolean updateStaffPassword(int staffId, String hashedPassword) throws SQLException {
        String query = "UPDATE staff SET password = ?, force_password_reset = 0 WHERE staff_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setInt(2, staffId);
            return pstmt.executeUpdate() > 0;
        }
    }
}