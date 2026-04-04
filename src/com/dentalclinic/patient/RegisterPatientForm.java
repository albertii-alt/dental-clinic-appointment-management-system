package com.dentalclinic.patient;

import com.dentalclinic.ui.LoginPage;
import com.dentalclinic.ui.components.SuccessDialog;
import com.dentalclinic.ui.components.ErrorDialog;
import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import java.io.InputStream;
import com.dentalclinic.util.PasswordValidator;
import java.util.List;

public class RegisterPatientForm extends JFrame {

    private JTextField firstNameField, middleNameField, lastNameField;
    private JTextField emailField, usernameField, addressField, contactField, ageField;
    private JPasswordField passwordField, confirmPasswordField;
    private JDateChooser birthDatePicker;
    private JButton submitBtn, cancelBtn;

    // --- UI Theme Constants ---
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color SECONDARY_BLUE = new Color(52, 152, 219);
    private final Color FOCUS_COLOR = new Color(52, 152, 219, 100);
    private final Color SIDEBAR_BG = new Color(242, 245, 248);
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color TEXT_GRAY = new Color(127, 140, 141);
    private final Color BORDER_COLOR = new Color(218, 226, 234);

    public RegisterPatientForm() {
        setTitle("Dental Clinic - Patient Registration");
        setSize(1000, 750); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setLocationRelativeTo(null);

        // --- MASTER PANEL ---
        JPanel masterPanel = new JPanel(new GridLayout(1, 2, 0, 0));
        add(masterPanel);

        // =========================================
        // SECTION 1: VISUAL SIDEBAR (LEFT)
        // =========================================
        JPanel visualSidebar = new JPanel(new GridBagLayout());
        visualSidebar.setBackground(SIDEBAR_BG);
        visualSidebar.setBorder(new EmptyBorder(60, 50, 60, 50));

        GridBagConstraints gLeft = new GridBagConstraints();
        gLeft.gridx = 0; 
        gLeft.fill = GridBagConstraints.HORIZONTAL;
        gLeft.anchor = GridBagConstraints.NORTHWEST;

        JLabel clinicName = new JLabel("Join Our Clinic");
        clinicName.setFont(new Font("Segoe UI", Font.BOLD, 36));
        clinicName.setForeground(PRIMARY_BLUE);
        gLeft.gridy = 0;
        visualSidebar.add(clinicName, gLeft);

        JLabel appTitle = new JLabel("Registration Portal");
        appTitle.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 20));
        appTitle.setForeground(TEXT_DARK);
        gLeft.gridy = 1;
        gLeft.insets = new Insets(5, 0, 0, 0);
        visualSidebar.add(appTitle, gLeft);

        gLeft.gridy = 2; 
        gLeft.weighty = 1.0; 
        visualSidebar.add(Box.createVerticalGlue(), gLeft);

        // Logo
        JLabel logoLabel = loadLogo("/com/dentalclinic/resources/VantageLogo.png", 350, -1);
        if (logoLabel != null) {
            gLeft.gridy = 3; 
            gLeft.weighty = 0; 
            gLeft.anchor = GridBagConstraints.CENTER;
            visualSidebar.add(logoLabel, gLeft);
        }

        gLeft.gridy = 4; 
        gLeft.weighty = 1.0; 
        visualSidebar.add(Box.createVerticalGlue(), gLeft);
        
                // --- Enhanced Sidebar Footer ---
        JPanel footerContainer = new JPanel(new BorderLayout(15, 0));
        footerContainer.setOpaque(false);

        // Modern Accent Bar
        JPanel accentBar = new JPanel();
        accentBar.setPreferredSize(new Dimension(4, 0));
        accentBar.setBackground(PRIMARY_BLUE);
        footerContainer.add(accentBar, BorderLayout.WEST);

        // Text Content with HEX color formatting for stability
        String darkHex = String.format("#%02x%02x%02x", TEXT_DARK.getRed(), TEXT_DARK.getGreen(), TEXT_DARK.getBlue());
        String grayHex = String.format("#%02x%02x%02x", TEXT_GRAY.getRed(), TEXT_GRAY.getGreen(), TEXT_GRAY.getBlue());

        JLabel footerText = new JLabel("<html><div style='font-family: Segoe UI;'>" +
                "<b style='font-size: 15px; color: " + darkHex + ";'>Start Your Journey to a Brighter Smile</b><br>" +
                "<span style='font-size: 11px; color: " + grayHex + "; line-height: 1.4;'>" +
                "Creating an account allows you to book appointments, view your<br>" +
                "dental history, and communicate with specialists directly.</span></div></html>");

        footerContainer.add(footerText, BorderLayout.CENTER);

        gLeft.gridy = 5; 
        gLeft.weighty = 0;
        gLeft.insets = new Insets(20, 0, 0, 0); // Spacing from logo
        gLeft.anchor = GridBagConstraints.SOUTHWEST;
        visualSidebar.add(footerContainer, gLeft);

        masterPanel.add(visualSidebar);

        // =========================================
        // SECTION 2: FORM AREA (RIGHT)
        // =========================================
        JPanel formArea = new JPanel(new BorderLayout());
        formArea.setBackground(Color.WHITE);
        
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(Color.WHITE);
        fieldsPanel.setBorder(new EmptyBorder(40, 45, 20, 45));
        
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.gridx = 0;
        
        // Header
        JLabel formTitle = new JLabel("Create Patient Profile");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        formTitle.setForeground(TEXT_DARK);
        g.gridy = 0;
        g.gridwidth = 2;
        g.insets = new Insets(0, 5, 25, 5);
        fieldsPanel.add(formTitle, g);
        
        g.gridwidth = 1;
        int currentRow = 1;
        
        // Row 1: Names
        addFormField(fieldsPanel, "FIRST NAME", firstNameField = new JTextField(), g, currentRow, 0);
        addFormField(fieldsPanel, "LAST NAME", lastNameField = new JTextField(), g, currentRow, 1);
        currentRow++;
        
        // Row 2: Middle Name & Birth Date
        addFormField(fieldsPanel, "MIDDLE NAME", middleNameField = new JTextField(), g, currentRow, 0);
        
        g.gridx = 1;
        g.gridy = currentRow * 2;
        g.insets = new Insets(5, 5, 4, 5);
        fieldsPanel.add(createFieldLabel("BIRTH DATE"), g);
        
        birthDatePicker = new JDateChooser();
        birthDatePicker.setDateFormatString("MMMM d, yyyy");
        styleInputField(birthDatePicker);
        g.gridy = (currentRow * 2) + 1;
        g.insets = new Insets(0, 5, 15, 5);
        fieldsPanel.add(birthDatePicker, g);
        currentRow++;
        
        // Row 3: Age & Contact
        addFormField(fieldsPanel, "AGE", ageField = new JTextField(), g, currentRow, 0);
        ageField.setEditable(false);
        ageField.setBackground(new Color(248, 250, 252));
        addFormField(fieldsPanel, "CONTACT NUMBER", contactField = new JTextField(), g, currentRow, 1);
        currentRow++;
        
        // Row 4: Address
        g.gridwidth = 2;
        addFormField(fieldsPanel, "FULL ADDRESS", addressField = new JTextField(), g, currentRow, 0);
        currentRow++;
        
        // Row 5: Email & Username
        g.gridwidth = 1;
        addFormField(fieldsPanel, "EMAIL ADDRESS", emailField = new JTextField(), g, currentRow, 0);
        addFormField(fieldsPanel, "USERNAME", usernameField = new JTextField(), g, currentRow, 1);
        currentRow++;
        
        // Row 6: Passwords
        addFormField(fieldsPanel, "PASSWORD", passwordField = new JPasswordField(), g, currentRow, 0);
        addFormField(fieldsPanel, "CONFIRM PASSWORD", confirmPasswordField = new JPasswordField(), g, currentRow, 1);
        currentRow++;
        
        // Vertical Glue
        g.gridwidth = 2;
        g.gridy = currentRow * 2;
        g.weighty = 1.0;
        fieldsPanel.add(Box.createVerticalGlue(), g);
        
        JScrollPane scrollPane = new JScrollPane(fieldsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Action Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 20));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        
        cancelBtn = new JButton("Back to Login");
        styleButton(cancelBtn, Color.WHITE, TEXT_DARK);
        cancelBtn.setBorder(new LineBorder(BORDER_COLOR));
        
        submitBtn = new JButton("Complete Registration");
        styleButton(submitBtn, SECONDARY_BLUE, Color.WHITE);
        
        btnPanel.add(cancelBtn);
        btnPanel.add(submitBtn);
        
        formArea.add(scrollPane, BorderLayout.CENTER);
        formArea.add(btnPanel, BorderLayout.SOUTH);
        
        masterPanel.add(formArea);
        
        initLogic();
        setVisible(true);
    }

    private void addFormField(JPanel p, String label, JComponent field, GridBagConstraints c, int row, int col) {
        c.gridx = col;
        c.gridy = row * 2;
        c.insets = new Insets(5, 5, 4, 5);
        p.add(createFieldLabel(label), c);
        
        c.gridy = (row * 2) + 1;
        c.insets = new Insets(0, 5, 15, 5);
        styleInputField(field);
        p.add(field, c);
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI Bold", Font.BOLD, 11));
        lbl.setForeground(TEXT_GRAY);
        return lbl;
    }

    private void styleInputField(JComponent field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(0, 42));
        field.setBackground(Color.WHITE);
        
        if (field instanceof JTextField || field instanceof JPasswordField) {
            Border normalBorder = BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
            );
            Border activeBorder = BorderFactory.createCompoundBorder(
                new LineBorder(SECONDARY_BLUE, 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
            );
            
            field.setBorder(normalBorder);
            
            field.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { field.setBorder(activeBorder); }
                @Override public void focusLost(FocusEvent e) { field.setBorder(normalBorder); }
            });
        }
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setPreferredSize(new Dimension(200, 50));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Simple Hover logic
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                if(bg != Color.WHITE) btn.setBackground(bg.darker());
                else btn.setBackground(new Color(250, 250, 250));
            }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
    }

    private void initLogic() {
        birthDatePicker.addPropertyChangeListener("date", evt -> {
            if (birthDatePicker.getDate() != null) {
                ageField.setText(String.valueOf(calculateAge(birthDatePicker.getDate())));
            }
        });

        contactField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                if (!Character.isDigit(evt.getKeyChar()) || contactField.getText().length() >= 11) {
                    evt.consume(); 
                }
            }
        });

        submitBtn.addActionListener(e -> handleRegistration());
        cancelBtn.addActionListener(e -> { new LoginPage(); dispose(); });
    }

    private int calculateAge(java.util.Date birthDate) {
        java.time.LocalDate birth = new java.sql.Date(birthDate.getTime()).toLocalDate();
        return java.time.Period.between(birth, java.time.LocalDate.now()).getYears();
    }

    private void handleRegistration() {
        String fName = firstNameField.getText();
        String mName = middleNameField.getText();
        String lName = lastNameField.getText();
        String address = addressField.getText();
        String contact = contactField.getText(); 
        String email = emailField.getText();
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());
        String confirm = new String(confirmPasswordField.getPassword());

        // Basic validation
        if (fName.isEmpty() || lName.isEmpty() || address.isEmpty() || contact.isEmpty() || user.isEmpty() || pass.isEmpty() || birthDatePicker.getDate() == null) {
            com.dentalclinic.ui.components.ErrorDialog.show(this, "Incomplete Form", "All required fields must be filled!");
            return;
        }

        // Check if passwords match
        if (!pass.equals(confirm)) {
            com.dentalclinic.ui.components.ErrorDialog.show(this, "Password Mismatch", "Passwords do not match!");
            return;
        }

        // Validate password complexity BEFORE calling the service
        List<String> passwordErrors = com.dentalclinic.util.PasswordValidator.validatePassword(pass);
        if (!passwordErrors.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("Password requirements not met:\n");
            for (String error : passwordErrors) {
                errorMsg.append("• ").append(error).append("\n");
            }
            com.dentalclinic.ui.components.ErrorDialog.show(this, "Invalid Password", errorMsg.toString());
            return;
        }

        try {
            com.dentalclinic.service.AuthService authService = new com.dentalclinic.service.AuthService();
            java.sql.Date sqlDate = new java.sql.Date(birthDatePicker.getDate().getTime());
            int ageValue = Integer.parseInt(ageField.getText());

            boolean success = authService.registerNewPatient(fName, mName, lName, sqlDate, ageValue, address, contact, email, user, pass);

            if (success) {
                com.dentalclinic.ui.components.SuccessDialog.show(this, "Account Created!", "Your profile has been successfully registered. You can now log in to book your first appointment.");
                new LoginPage();
                dispose();
            } else {
                com.dentalclinic.ui.components.ErrorDialog.show(this, "Registration Failed", "Username may already exist. Please try a different username.");
            }
        } catch (SQLException ex) {
            com.dentalclinic.ui.components.ErrorDialog.show(this, "Database Error", "Unable to connect to the server: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            // Fallback for any validation errors from the service
            com.dentalclinic.ui.components.ErrorDialog.show(this, "Invalid Password", ex.getMessage());
        }
    }

    private JLabel loadLogo(String path, int width, int height) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) return null;
            Image img = ImageIO.read(is);
            Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new JLabel(new ImageIcon(scaledImg));
        } catch (Exception e) { return null; }
    }
}