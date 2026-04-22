package com.dentalclinic.view;

import com.dentalclinic.controller.AuthController;
import com.dentalclinic.dto.auth.LoginRequest;
import com.dentalclinic.dto.auth.LoginResult;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;
import com.dentalclinic.model.Patient;
import com.dentalclinic.util.UserSession;
import com.dentalclinic.view.components.SuccessDialog;
import com.dentalclinic.view.components.ErrorDialog;
import com.dentalclinic.util.PasswordValidator;
import com.dentalclinic.util.Sanitizer;  // ADDED: Import Sanitizer
import java.util.List;
import javax.imageio.ImageIO;
import java.io.InputStream;

public class LoginPage extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleDropdown;
    private JButton loginButton, registerButton;
    private final AuthController authController;
    
    // ADDED: Rate limiting for forgot password
    private long lastForgotPasswordRequest = 0;
    private static final long FORGOT_PASSWORD_COOLDOWN_MS = 60000; // 1 minute

    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color SECONDARY_BLUE = new Color(52, 152, 219);
    
    // Gradient Sidebar Colors
    private final Color SIDEBAR_START = new Color(20, 30, 48); 
    private final Color SIDEBAR_END = new Color(36, 59, 85);
    
    private final Color TEXT_DARK = new Color(52, 73, 94);
    private final Color TEXT_GRAY = new Color(127, 140, 141);
    private final Color BORDER_COLOR = new Color(218, 226, 234);
    private final Color SUCCESS_GREEN = new Color(46, 204, 113);

    public LoginPage() {
        authController = new AuthController();
        
        setTitle("Vantage Dental - Login");
        com.dentalclinic.util.AppIcon.apply(this);
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        if (!authController.isDatabaseAvailable()) {
            showNoConnectionDialog();
            return;
        }

        JPanel masterPanel = new JPanel(new GridLayout(1, 2));
        add(masterPanel);

        // --- Sidebar (Left) ---
        JPanel sidebar = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, SIDEBAR_START, w, h, SIDEBAR_END);
                
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        sidebar.setOpaque(false);
        sidebar.setBorder(new EmptyBorder(50, 50, 50, 50));
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0; gbcL.fill = GridBagConstraints.HORIZONTAL; gbcL.anchor = GridBagConstraints.NORTHWEST;

        JLabel name = new JLabel("Vantage Dental");
        name.setFont(new Font("Segoe UI", Font.BOLD, 36));
        name.setForeground(Color.WHITE);
        gbcL.gridy = 0; sidebar.add(name, gbcL);

        JLabel sub = new JLabel("Appointment Portal");
        sub.setFont(new Font("Segoe UI Semilight", Font.PLAIN, 20));
        sub.setForeground(new Color(236, 240, 241));
        gbcL.gridy = 1; gbcL.insets = new Insets(5, 0, 0, 0);
        sidebar.add(sub, gbcL);

        gbcL.gridy = 2; gbcL.weighty = 1.0; sidebar.add(Box.createVerticalGlue(), gbcL);

        JLabel logo = loadLogo("/com/dentalclinic/resources/VantageLogoInForms.png", 350, -1);
        if (logo != null) {
            gbcL.gridy = 3; gbcL.weighty = 0; gbcL.anchor = GridBagConstraints.CENTER;
            sidebar.add(logo, gbcL);
        }

        gbcL.gridy = 4; gbcL.weighty = 1.0; sidebar.add(Box.createVerticalGlue(), gbcL);
        
        JPanel footerContainer = new JPanel(new BorderLayout(15, 0));
        footerContainer.setOpaque(false);

        JPanel accentBar = new JPanel();
        accentBar.setPreferredSize(new Dimension(4, 0));
        accentBar.setBackground(SECONDARY_BLUE);
        footerContainer.add(accentBar, BorderLayout.WEST);

        JLabel footerText = new JLabel("<html><div style='font-family: Segoe UI;'>" +
                "<b style='font-size: 14px; color: #FFFFFF;'>Manage Your Oral Health</b><br>" +
                "<span style='font-size: 11px; color: #BDC3C7;'>Log in to view clinic schedules, treatment<br>history, and digital prescriptions.</span></div></html>");

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
        
        // --- ROLE DROPDOWN ENHANCEMENT ---

        // 1. Define the items
        String[] roles = {"Patient", "Staff", "Dentist", "Admin"};
        roleDropdown = new JComboBox<>(roles);

        // 2. Apply Custom Styling
        styleRoleDropdown(roleDropdown);

        // Add to formArea (using your existing gbcR constraints)
        gbcR.gridy = 6; gbcR.insets = new Insets(0, 0, 5, 0);
        formArea.add(createFieldLabel("SIGN IN AS"), gbcR);

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
        
        btns.add(loginButton); 
        btns.add(Box.createHorizontalStrut(15)); 
        btns.add(registerButton);

        gbcR.gridy = 8;
        gbcR.insets = new Insets(0, 0, 10, 0);
        formArea.add(btns, gbcR);

        JButton forgotPasswordBtn = new JButton("Forgot Password?");
        forgotPasswordBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        forgotPasswordBtn.setForeground(SECONDARY_BLUE);
        forgotPasswordBtn.setContentAreaFilled(false);
        forgotPasswordBtn.setBorderPainted(false);
        forgotPasswordBtn.setFocusPainted(false);
        forgotPasswordBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        forgotPasswordBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { forgotPasswordBtn.setText("<html><u>Forgot Password?</u></html>"); }
            public void mouseExited(MouseEvent e) { forgotPasswordBtn.setText("Forgot Password?"); }
        });
        forgotPasswordBtn.addActionListener(e -> showForgotPasswordDialog());

        GridBagConstraints gbcCenter = new GridBagConstraints();
        gbcCenter.gridx = 0;
        gbcCenter.gridy = 9;
        gbcCenter.gridwidth = 1;
        gbcCenter.anchor = GridBagConstraints.CENTER;
        gbcCenter.insets = new Insets(5, 0, 0, 0);
        
        formArea.add(forgotPasswordBtn, gbcCenter);

        masterPanel.add(formArea);
        initActionListeners();
        setVisible(true);
    }

    private void showNoConnectionDialog() {
        setSize(420, 280);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(35, 40, 30, 40));

        JLabel icon = new JLabel();
        icon.setIcon(org.kordamp.ikonli.swing.FontIcon.of(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.WIFI, 48, new Color(189, 195, 199)));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(icon);
        panel.add(Box.createVerticalStrut(15));

        JLabel title = new JLabel("No Connection");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(44, 62, 80));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(8));

        JLabel msg = new JLabel("<html><div style='text-align:center;'>Unable to connect to the clinic server.<br>Please check your internet connection<br>and try again.</div></html>");
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msg.setForeground(new Color(127, 140, 141));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(msg);
        panel.add(Box.createVerticalStrut(25));

        JButton retryBtn = new JButton("Retry");
        retryBtn.setIcon(org.kordamp.ikonli.swing.FontIcon.of(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SYNC_ALT, 13, Color.WHITE));
        retryBtn.setBackground(PRIMARY_BLUE);
        retryBtn.setForeground(Color.WHITE);
        retryBtn.setFocusPainted(false);
        retryBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        retryBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        retryBtn.setBorder(new EmptyBorder(10, 30, 10, 30));
        retryBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        retryBtn.addActionListener(e -> {
            dispose();
            new LoginPage();
        });
        panel.add(retryBtn);

        add(panel, BorderLayout.CENTER);
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
            // FIXED: Sanitize username input
            final String user = Sanitizer.sanitizeUsername(usernameField.getText());
            final String pass = new String(passwordField.getPassword());
            final String role = (String) roleDropdown.getSelectedItem();

            loginButton.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            SwingWorker<Boolean, Void> loginWorker = new SwingWorker<Boolean, Void>() {
                private LoginResult loginResult;
                private Exception loginError;

                @Override
                protected Boolean doInBackground() {
                    try {
                        loginResult = authController.login(new LoginRequest(user, pass, role));
                        return true;
                    } catch (Exception ex) {
                        loginError = ex;
                        return false;
                    }
                }

                @Override
                protected void done() {
                    try {
                        boolean completed = get();
                        if (!completed) {
                            String errorMessage = loginError != null ? loginError.getMessage() : "Unknown login error";
                            ErrorDialog.show(LoginPage.this, "Database Error", "Unable to connect to the clinic server: " + errorMessage);
                            passwordField.setText("");
                            return;
                        }

                        if (loginResult.getStatus() == LoginResult.Status.ACCOUNT_LOCKED) {
                            String message = "Your account has been locked due to multiple failed login attempts.\n" +
                                            "Please try again in " + loginResult.getRemainingMinutes() + " minute(s).";
                            ErrorDialog.show(LoginPage.this, "Account Locked", message);
                            passwordField.setText("");
                            return;
                        }

                        if (loginResult.getStatus() == LoginResult.Status.RESET_REQUIRED) {
                            showPasswordResetDialog(loginResult);
                            return;
                        }

                        if (loginResult.getStatus() == LoginResult.Status.SUCCESS_STAFF) {
                            int id = loginResult.getUserId();
                            String rStr = loginResult.getRoleName();
                            boolean isS = loginResult.isSuperAdmin();
                            String name = loginResult.getFullName();
                            String email = loginResult.getEmail() != null ? loginResult.getEmail() : "No Email";

                            UserSession.initialize(id, name, isS ? "Super Admin" : rStr, loginResult.getPermissions());

                            SuccessDialog.show(LoginPage.this, "Access Granted", "Welcome back, " + name + "!");

                            if (rStr.equalsIgnoreCase("ADMIN")) new AdminDashboard(id, isS, name, email, user);
                            else if (rStr.equalsIgnoreCase("DENTIST")) new DentistDashboard(id, name, user, email);
                            else if (rStr.equalsIgnoreCase("STAFF")) new StaffDashboard(id, name, user, email);
                            dispose();
                            return;
                        }

                        if (loginResult.getStatus() == LoginResult.Status.SUCCESS_PATIENT) {
                            Patient p = loginResult.getPatient();
                            UserSession.initialize(p.getPatientId(), p.getFirstName() + " " + p.getLastName(), "PATIENT", null);
                            SuccessDialog.show(LoginPage.this, "Welcome Back!", "Logging you in, " + p.getFirstName());
                            new PatientDashboard(p.getPatientId(), p.getFirstName(), p.getMiddleName(), p.getLastName(), p.getBirthDate().toString(), String.valueOf(p.getAge()), p.getAddress(), p.getContactNumber(), p.getUsername());
                            dispose();
                            return;
                        }

                        ErrorDialog.show(LoginPage.this, "Login Failed", "The username or password you entered is incorrect for the selected role.");
                        passwordField.setText("");
                    } catch (Exception ex) {
                        ErrorDialog.show(LoginPage.this, "Database Error", "Unable to connect to the clinic server: " + ex.getMessage());
                        passwordField.setText("");
                    } finally {
                        loginButton.setEnabled(true);
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            };

            loginWorker.execute();
        });

        registerButton.addActionListener(e -> {
            new com.dentalclinic.view.patient.RegisterPatientForm();
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
    
    private void showPasswordResetDialog(LoginResult loginResult) {
        JDialog dialog = new JDialog(this, "Password Reset Required", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 20, 10, 20);

        JLabel warningLabel = new JLabel("<html><center><b>Security Notice</b><br>For security reasons, you must change your password.<br>This is required once due to system security upgrade.</center></html>");
        warningLabel.setForeground(new Color(255, 100, 100));
        gbc.gridy = 0;
        dialog.add(warningLabel, gbc);

        gbc.gridy = 1;
        dialog.add(new JLabel("New Password:"), gbc);

        JPasswordField newPassField = new JPasswordField();
        newPassField.setPreferredSize(new Dimension(300, 30));
        gbc.gridy = 2;
        dialog.add(newPassField, gbc);

        gbc.gridy = 3;
        dialog.add(new JLabel("Confirm Password:"), gbc);

        JPasswordField confirmPassField = new JPasswordField();
        confirmPassField.setPreferredSize(new Dimension(300, 30));
        gbc.gridy = 4;
        dialog.add(confirmPassField, gbc);

        JLabel reqLabel = new JLabel("<html><small>Password must be at least 6 characters</small></html>");
        reqLabel.setForeground(Color.GRAY);
        gbc.gridy = 5;
        dialog.add(reqLabel, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton resetButton = new JButton("Reset Password");
        JButton cancelButton = new JButton("Logout");

        resetButton.addActionListener(evt -> {
            String newPass = new String(newPassField.getPassword());
            String confirmPass = new String(confirmPassField.getPassword());

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
                boolean success = authController.resetForcedPassword(loginResult.getUserId(), loginResult.getRoleName(), newPass);

                if (success) {
                    SuccessDialog.show(LoginPage.this, "Success", "Password updated successfully! Please login again.");
                    dialog.dispose();
                } else {
                    ErrorDialog.show(LoginPage.this, "Error", "Failed to update password. Please try again.");
                }
            } catch (Exception ex) {
                ErrorDialog.show(LoginPage.this, "Database Error", ex.getMessage());
            }
        });

        cancelButton.addActionListener(evt -> {
            dialog.dispose();
            usernameField.setText("");
            passwordField.setText("");
        });

        buttonPanel.add(resetButton);
        buttonPanel.add(cancelButton);
        gbc.gridy = 6;
        dialog.add(buttonPanel, gbc);

        dialog.setVisible(true);
    }

    private void showForgotPasswordDialog() {
        // FIXED: Rate limiting check
        long now = System.currentTimeMillis();
        if (now - lastForgotPasswordRequest < FORGOT_PASSWORD_COOLDOWN_MS) {
            long remaining = (FORGOT_PASSWORD_COOLDOWN_MS - (now - lastForgotPasswordRequest)) / 1000;
            ErrorDialog.show(this, "Please Wait", "Please wait " + remaining + " seconds before requesting another code.");
            return;
        }
        
        JDialog forgotDialog = new JDialog(this, "Forgot Password", true);
        forgotDialog.setSize(450, 300);
        forgotDialog.setLocationRelativeTo(this);
        forgotDialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel instructionLabel = new JLabel("Enter your username:");
        instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridy = 0;
        forgotDialog.add(instructionLabel, gbc);

        JTextField usernameField = new JTextField(20);
        usernameField.setPreferredSize(new Dimension(300, 35));
        gbc.gridy = 1;
        forgotDialog.add(usernameField, gbc);

        JButton sendButton = new JButton("Send Reset Code");
        styleFormButton(sendButton, PRIMARY_BLUE, Color.WHITE);
        gbc.gridy = 2;
        forgotDialog.add(sendButton, gbc);

        sendButton.addActionListener(evt -> {
            // FIXED: Sanitize username input
            String username = Sanitizer.sanitizeUsername(usernameField.getText().trim());
            if (username.isEmpty()) {
                ErrorDialog.show(LoginPage.this, "Error", "Please enter your username.");
                return;
            }

            // FIXED: Update rate limiting timestamp
            lastForgotPasswordRequest = System.currentTimeMillis();

            sendButton.setEnabled(false);
            sendButton.setText("Sending...");

            new Thread(() -> {
                try {
                    String maskedEmail = authController.requestPasswordResetByUsername(username);

                    SwingUtilities.invokeLater(() -> {
                        sendButton.setEnabled(true);
                        sendButton.setText("Send Reset Code");
                        forgotDialog.dispose();

                        if (maskedEmail != null) {
                            SuccessDialog.show(LoginPage.this, "Code Sent", 
                                "A 6-digit reset code has been sent to:\n" + maskedEmail + "\n\n" +
                                "The code will expire in 15 minutes.");
                            showVerifyCodeDialog(username);
                        } else {
                            SuccessDialog.show(LoginPage.this, "Code Sent", 
                                "If an account exists with that username, a reset code has been sent.");
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        sendButton.setEnabled(true);
                        sendButton.setText("Send Reset Code");
                        ErrorDialog.show(LoginPage.this, "Error", "Database error: " + ex.getMessage());
                    });
                }
            }).start();
        });

        forgotDialog.setVisible(true);
    }

    private void showVerifyCodeDialog(String username) {
        JDialog verifyDialog = new JDialog(this, "Verify Reset Code", true);
        verifyDialog.setSize(450, 300);
        verifyDialog.setLocationRelativeTo(this);
        verifyDialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel instructionLabel = new JLabel("Enter the 6-digit code sent to your email:");
        instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridy = 0;
        verifyDialog.add(instructionLabel, gbc);

        JTextField codeField = new JTextField(20);
        codeField.setPreferredSize(new Dimension(300, 35));
        codeField.setHorizontalAlignment(JTextField.CENTER);
        codeField.setFont(new Font("Segoe UI", Font.BOLD, 18));
        gbc.gridy = 1;
        verifyDialog.add(codeField, gbc);

        JButton verifyButton = new JButton("Verify Code");
        styleFormButton(verifyButton, PRIMARY_BLUE, Color.WHITE);
        gbc.gridy = 2;
        verifyDialog.add(verifyButton, gbc);

        verifyButton.addActionListener(evt -> {
            String code = codeField.getText().trim();
            if (code.length() != 6 || !code.matches("\\d+")) {
                ErrorDialog.show(LoginPage.this, "Invalid Code", "Please enter a valid 6-digit code.");
                return;
            }

            verifyButton.setEnabled(false);
            verifyButton.setText("Verifying...");

            new Thread(() -> {
                try {
                    boolean valid = authController.verifyResetCode(code);

                    SwingUtilities.invokeLater(() -> {
                        verifyButton.setEnabled(true);
                        verifyButton.setText("Verify Code");

                        if (valid) {
                            verifyDialog.dispose();
                            showResetPasswordDialog(username, code);
                        } else {
                            ErrorDialog.show(LoginPage.this, "Invalid Code", 
                                "The code is invalid or has expired. Please request a new code.");
                        }
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        verifyButton.setEnabled(true);
                        verifyButton.setText("Verify Code");
                        ErrorDialog.show(LoginPage.this, "Error", "Database error: " + ex.getMessage());
                    });
                }
            }).start();
        });

        verifyDialog.setVisible(true);
    }

    private void showResetPasswordDialog(String username, String resetCode) {
        JDialog resetDialog = new JDialog(this, "Reset Password", true);
        resetDialog.setSize(450, 400);
        resetDialog.setLocationRelativeTo(this);
        resetDialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        JLabel instructionLabel = new JLabel("Enter your new password:");
        instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridy = 0;
        resetDialog.add(instructionLabel, gbc);

        JPasswordField newPassField = new JPasswordField();
        newPassField.setPreferredSize(new Dimension(300, 35));
        gbc.gridy = 1;
        resetDialog.add(newPassField, gbc);

        JLabel confirmLabel = new JLabel("Confirm new password:");
        confirmLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridy = 2;
        resetDialog.add(confirmLabel, gbc);

        JPasswordField confirmPassField = new JPasswordField();
        confirmPassField.setPreferredSize(new Dimension(300, 35));
        gbc.gridy = 3;
        resetDialog.add(confirmPassField, gbc);

        JLabel reqLabel = new JLabel("<html><small>Password must be at least 8 characters with uppercase,<br>lowercase, number, and special character.</small></html>");
        reqLabel.setForeground(Color.GRAY);
        gbc.gridy = 4;
        resetDialog.add(reqLabel, gbc);

        JButton resetButton = new JButton("Reset Password");
        styleFormButton(resetButton, SUCCESS_GREEN, Color.WHITE);
        gbc.gridy = 5;
        resetDialog.add(resetButton, gbc);

        resetButton.addActionListener(evt -> {
            String newPass = new String(newPassField.getPassword());
            String confirmPass = new String(confirmPassField.getPassword());

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

            resetButton.setEnabled(false);
            resetButton.setText("Resetting...");

            new Thread(() -> {
                try {
                    boolean success = authController.resetPasswordByUsername(username, resetCode, newPass);

                    SwingUtilities.invokeLater(() -> {
                        if (success) {
                            SuccessDialog.show(LoginPage.this, "Success", 
                                "Your password has been reset successfully!\n\nPlease login with your new password.");
                            resetDialog.dispose();
                        } else {
                            ErrorDialog.show(LoginPage.this, "Error", 
                                "Failed to reset password. The code may have expired. Please request a new code.");
                        }
                        resetButton.setEnabled(true);
                        resetButton.setText("Reset Password");
                    });
                } catch (IllegalArgumentException ex) {
                    SwingUtilities.invokeLater(() -> {
                        ErrorDialog.show(LoginPage.this, "Invalid Password", ex.getMessage());
                        resetButton.setEnabled(true);
                        resetButton.setText("Reset Password");
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        ErrorDialog.show(LoginPage.this, "Error", "Database error: " + ex.getMessage());
                        resetButton.setEnabled(true);
                        resetButton.setText("Reset Password");
                    });
                }
            }).start();
        });

        resetDialog.setVisible(true);
    }
    
    private void styleRoleDropdown(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        combo.setPreferredSize(new Dimension(0, 45));
        combo.setBackground(Color.WHITE);

        // 1. Flatten the UI Look
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                // Create a custom, flat arrow button
                JButton button = new JButton();
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setContentAreaFilled(false);
                button.setFocusPainted(false);
                button.setBackground(Color.WHITE);
                // Add a simple chevron/arrow icon or text
                button.setText("▼ "); 
                button.setForeground(TEXT_GRAY);
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
                return button;
            }
        });

        // 2. Consistent Borders (Same logic as your styleInputField)
        Border n = BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR), 
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        );
        Border a = BorderFactory.createCompoundBorder(
            new LineBorder(SECONDARY_BLUE, 1), 
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        );

        combo.setBorder(n);

        // 3. Focus Listeners for Border Transition
        combo.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { combo.setBorder(a); }
            public void focusLost(FocusEvent e) { combo.setBorder(n); }
        });

        // 4. Custom Renderer for the Popup List (keeps your previous enhancement)
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
                if (isSelected) {
                    label.setBackground(SECONDARY_BLUE);
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(TEXT_DARK);
                }
                return label;
            }
        });
    }
}
