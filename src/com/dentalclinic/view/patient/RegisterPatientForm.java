package com.dentalclinic.view.patient;

import com.dentalclinic.controller.AuthController;
import com.dentalclinic.view.LoginPage;
import com.dentalclinic.view.components.SuccessDialog;
import com.dentalclinic.view.components.ErrorDialog;
import com.dentalclinic.util.Sanitizer;
import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
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
    private final AuthController authController = new AuthController();

    // ADDED: Field length limits
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_ADDRESS_LENGTH = 200;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MAX_CONTACT_LENGTH = 11;

    // --- UI Theme Constants ---
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color SECONDARY_BLUE = new Color(52, 152, 219);
    private final Color SIDEBAR_START = new Color(44, 62, 80);
    private final Color SIDEBAR_END = new Color(24, 34, 45);
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color TEXT_GRAY = new Color(127, 140, 141);
    private final Color BORDER_COLOR = new Color(218, 226, 234);

    // --- OPTIMIZED GRADIENT PANEL (Static inner class for reusability) ---
    private static class GradientPanel extends JPanel {
        private final Color startColor;
        private final Color endColor;
        
        public GradientPanel(Color start, Color end) {
            this.startColor = start;
            this.endColor = end;
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(0, 0, startColor, 0, getHeight(), endColor);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.dispose();
            super.paintComponent(g);
        }
    }

    public RegisterPatientForm() {
        setTitle("Dental Clinic - Patient Registration");
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/com/dentalclinic/resources/VantageLogo.png");
            if (iconStream != null) {
                setIconImage(javax.imageio.ImageIO.read(iconStream));
            }
        } catch (Exception ignored) {}
        setSize(1000, 750); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setLocationRelativeTo(null);

        // --- MASTER PANEL ---
        JPanel masterPanel = new JPanel(new GridLayout(1, 2, 0, 0));
        add(masterPanel);

        // =========================================
        // SECTION 1: VISUAL SIDEBAR (LEFT) - GRADIENT
        // =========================================
        JPanel visualSidebar = new GradientPanel(SIDEBAR_START, SIDEBAR_END);
        visualSidebar.setLayout(new GridBagLayout());
        visualSidebar.setBorder(new EmptyBorder(60, 50, 60, 50));

        GridBagConstraints gLeft = new GridBagConstraints();
        gLeft.gridx = 0; 
        gLeft.fill = GridBagConstraints.HORIZONTAL;
        gLeft.anchor = GridBagConstraints.NORTHWEST;

        JLabel clinicName = new JLabel("Join Our Clinic");
        clinicName.setFont(new Font("Segoe UI", Font.BOLD, 36));
        clinicName.setForeground(Color.WHITE);
        gLeft.gridy = 0;
        visualSidebar.add(clinicName, gLeft);

        JLabel appTitle = new JLabel("Registration Portal");
        appTitle.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 20));
        appTitle.setForeground(new Color(189, 195, 199));
        gLeft.gridy = 1;
        gLeft.insets = new Insets(5, 0, 0, 0);
        visualSidebar.add(appTitle, gLeft);

        gLeft.gridy = 2; 
        gLeft.weighty = 1.0; 
        visualSidebar.add(Box.createVerticalGlue(), gLeft);

        // Logo
        JLabel logoLabel = loadLogo("/com/dentalclinic/resources/VantageLogoInForms.png", 350, -1);
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

        JPanel accentBar = new JPanel();
        accentBar.setPreferredSize(new Dimension(4, 0));
        accentBar.setBackground(PRIMARY_BLUE);
        footerContainer.add(accentBar, BorderLayout.WEST);

        JLabel footerText = new JLabel("<html><div style='font-family: Segoe UI;'>" +
                "<b style='font-size: 15px; color: #FFFFFF;'>Start Your Journey to a Brighter Smile</b><br>" +
                "<span style='font-size: 11px; color: #BDC3C7; line-height: 1.4;'>" +
                "Creating an account allows you to book appointments, view your<br>" +
                "dental history, and communicate with specialists directly.</span></div></html>");

        footerContainer.add(footerText, BorderLayout.CENTER);

        gLeft.gridy = 5; 
        gLeft.weighty = 0;
        gLeft.insets = new Insets(20, 0, 0, 0); 
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
        if (field instanceof JPasswordField) {
            p.add(createPasswordWrapper((JPasswordField) field), c);
        } else {
            p.add(field, c);
        }
    }

    private JPanel createPasswordWrapper(JPasswordField field) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setPreferredSize(new Dimension(0, 42));
        field.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 5));
        field.setOpaque(false);

        JButton eyeBtn = new JButton(org.kordamp.ikonli.swing.FontIcon.of(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.EYE_SLASH, 14, TEXT_GRAY));
        eyeBtn.setFocusPainted(false);
        eyeBtn.setBorderPainted(false);
        eyeBtn.setContentAreaFilled(false);
        eyeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeBtn.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));

        final boolean[] visible = {false};
        eyeBtn.addActionListener(e -> {
            visible[0] = !visible[0];
            field.setEchoChar(visible[0] ? (char) 0 : '\u2022');
            eyeBtn.setIcon(org.kordamp.ikonli.swing.FontIcon.of(
                visible[0] ? org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.EYE
                           : org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.EYE_SLASH,
                14, TEXT_GRAY));
        });

        Border normal = BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR), BorderFactory.createEmptyBorder(0, 0, 0, 0));
        Border active = BorderFactory.createCompoundBorder(
            new LineBorder(SECONDARY_BLUE, 1), BorderFactory.createEmptyBorder(0, 0, 0, 0));
        wrapper.setBorder(normal);
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { wrapper.setBorder(active); }
            public void focusLost(FocusEvent e) { wrapper.setBorder(normal); }
        });

        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(eyeBtn, BorderLayout.EAST);
        return wrapper;
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
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                if(bg != Color.WHITE) btn.setBackground(bg.darker());
                else btn.setBackground(new Color(250, 250, 250));
            }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
    }

    // ADDED: Field length limiter using DocumentFilter
    private void limitTextFieldLength(JTextField field, int maxLength) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (fb.getDocument().getLength() + string.length() <= maxLength) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (fb.getDocument().getLength() - length + text.length() <= maxLength) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }
    
    // ADDED: Enhanced contact validation with DocumentFilter
    private void setupContactValidation(JTextField field) {
        // Prevent pasting non-digits
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (fb.getDocument().getLength() + string.length() <= MAX_CONTACT_LENGTH && string.matches("\\d*")) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (fb.getDocument().getLength() - length + text.length() <= MAX_CONTACT_LENGTH && text.matches("\\d*")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
        
        // Also keep key listener for real-time feedback
        field.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                char c = evt.getKeyChar();
                if (!Character.isDigit(c) || field.getText().length() >= MAX_CONTACT_LENGTH) {
                    evt.consume(); 
                }
            }
        });
    }

    private void initLogic() {
        birthDatePicker.addPropertyChangeListener("date", evt -> {
            if (birthDatePicker.getDate() != null) {
                ageField.setText(String.valueOf(calculateAge(birthDatePicker.getDate())));
            }
        });

        // FIXED: Enhanced contact validation
        setupContactValidation(contactField);
        
        // FIXED: Add length limits to all text fields
        limitTextFieldLength(firstNameField, MAX_NAME_LENGTH);
        limitTextFieldLength(middleNameField, MAX_NAME_LENGTH);
        limitTextFieldLength(lastNameField, MAX_NAME_LENGTH);
        limitTextFieldLength(addressField, MAX_ADDRESS_LENGTH);
        limitTextFieldLength(emailField, MAX_EMAIL_LENGTH);
        limitTextFieldLength(usernameField, MAX_USERNAME_LENGTH);

        submitBtn.addActionListener(e -> handleRegistration());
        cancelBtn.addActionListener(e -> { new LoginPage(); dispose(); });
    }

    private int calculateAge(java.util.Date birthDate) {
        java.time.LocalDate birth = java.time.Instant.ofEpochMilli(birthDate.getTime())
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
        return java.time.Period.between(birth, java.time.LocalDate.now()).getYears();
    }

    private void handleRegistration() {
        String rawFirstName = firstNameField.getText().trim();
        String rawMiddleName = middleNameField.getText().trim();
        String rawLastName = lastNameField.getText().trim();
        String rawAddress = addressField.getText().trim();
        String rawContact = contactField.getText().trim();
        String rawEmail = emailField.getText().trim();
        String rawUsername = usernameField.getText().trim();
        String rawPassword = new String(passwordField.getPassword());
        String rawConfirmPassword = new String(confirmPasswordField.getPassword());

        String fName = Sanitizer.sanitizeName(rawFirstName);
        String mName = Sanitizer.sanitizeName(rawMiddleName);
        String lName = Sanitizer.sanitizeName(rawLastName);
        String address = Sanitizer.sanitizeTextField(rawAddress);
        String contact = Sanitizer.sanitizePhone(rawContact);
        String email = Sanitizer.sanitizeEmail(rawEmail);
        String user = Sanitizer.sanitizeUsername(rawUsername);
        
        String pass = rawPassword;
        String confirm = rawConfirmPassword;

        if (fName.isEmpty() || lName.isEmpty() || address.isEmpty() || contact.isEmpty() || user.isEmpty() || pass.isEmpty() || birthDatePicker.getDate() == null) {
            ErrorDialog.show(this, "Incomplete Form", "All required fields must be filled!");
            return;
        }

        if (!rawEmail.isEmpty() && email.isEmpty()) {
            ErrorDialog.show(this, "Invalid Email", "Please enter a valid email address.");
            return;
        }

        if (!pass.equals(confirm)) {
            ErrorDialog.show(this, "Password Mismatch", "Passwords do not match!");
            return;
        }

        List<String> passwordErrors = PasswordValidator.validatePassword(pass);
        if (!passwordErrors.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("Password requirements not met:\n");
            for (String error : passwordErrors) {
                errorMsg.append("- ").append(error).append("\n");
            }
            ErrorDialog.show(this, "Invalid Password", errorMsg.toString());
            return;
        }

        try {
            int ageValue = Integer.parseInt(ageField.getText());

            boolean success = authController.registerNewPatient(fName, mName, lName, birthDatePicker.getDate(), ageValue, address, contact, email, user, pass);

            if (success) {
                SuccessDialog.show(this, "Account Created!", "Your profile has been successfully registered. You can now log in to book your first appointment.");
                new LoginPage();
                dispose();
            } else {
                ErrorDialog.show(this, "Registration Failed", "Username may already exist. Please try a different username.");
            }
        } catch (IllegalArgumentException ex) {
            ErrorDialog.show(this, "Registration Failed", ex.getMessage());
        } catch (Exception ex) {
            ErrorDialog.show(this, "Database Error", "Unable to connect to the server. Please try again later.");
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
