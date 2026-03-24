package com.dentalclinic.admin;

import com.dentalclinic.service.DashboardService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;
import java.util.List;

// JFreeChart Imports
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;

public class AdminDashboardPanel extends JPanel {

    private DashboardService dashboardService;
    private JLabel lblTotalPatients, lblPendingAppts, lblTodayAppts, lblActiveStaff;
    private DefaultListModel<String> activityModel;
    private JList<String> activityList;
    private List<String[]> currentLogs;
    
    // Chart Components
    private ChartPanel chartPanel;
    private DefaultCategoryDataset dataset;

    public AdminDashboardPanel() {
        this.dashboardService = new DashboardService();
        initComponents();
        refreshStats();
    }

    private void initComponents() {
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(25, 25, 25, 25));
        setBackground(new Color(245, 246, 250));

        // --- TOP HEADER ---
        JLabel title = new JLabel("System Overview");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(44, 62, 80));
        add(title, BorderLayout.NORTH);

        // --- CENTER CONTENT (KPI Cards) ---
        JPanel cardContainer = new JPanel(new GridLayout(1, 4, 20, 0));
        cardContainer.setOpaque(false);

        lblTotalPatients = createStatCard(cardContainer, "Total Patients", "0", new Color(52, 152, 219));
        lblPendingAppts = createStatCard(cardContainer, "Pending Requests", "0", new Color(241, 196, 15));
        lblTodayAppts = createStatCard(cardContainer, "Today's Schedule", "0", new Color(46, 204, 113));
        lblActiveStaff = createStatCard(cardContainer, "Active Staff", "0", new Color(155, 89, 182));

        add(cardContainer, BorderLayout.CENTER);

        // --- BOTTOM SECTION (Using GridBagLayout for unequal widths) ---
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setPreferredSize(new Dimension(0, 400));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 20); // Gap between components

        // 1. RECENT ACTIVITY (Smaller width)
        activityModel = new DefaultListModel<>();
        activityList = new JList<>(activityModel) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index > -1 && currentLogs != null && index < currentLogs.size()) {
                    String details = currentLogs.get(index)[1];
                    return "<html><div style='background-color: #2c3e50; color: white; padding: 8px; border-radius: 4px;'>" +
                           "<b style='color: #3498db;'>LOG DETAILS:</b><br>" + details + "</div></html>";
                }
                return null;
            }
        };
        activityList.setCellRenderer(new ActivityCellRenderer());
        activityList.setBackground(new Color(245, 246, 250));
        activityList.setFixedCellHeight(60);
        ToolTipManager.sharedInstance().registerComponent(activityList);

        JScrollPane activityScroll = new JScrollPane(activityList);
        activityScroll.setBorder(null);
        activityScroll.setOpaque(false);
        activityScroll.getViewport().setOpaque(false);

        JPanel activityContainer = new JPanel(new BorderLayout(0, 10));
        activityContainer.setOpaque(false);
        JLabel activityTitle = new JLabel("Recent System Activity");
        activityTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        activityContainer.add(activityTitle, BorderLayout.NORTH);
        activityContainer.add(activityScroll, BorderLayout.CENTER);

        gbc.weightx = 0.3; // 30% width
        gbc.weighty = 1.0;
        bottomPanel.add(activityContainer, gbc);

        // 2. APPOINTMENT TRENDS CHART (Larger width)
        JPanel chartContainer = new JPanel(new BorderLayout(0, 10));
        chartContainer.setOpaque(false);
        JLabel chartTitle = new JLabel("Appointment Trends (By Service)");
        chartTitle.setFont(new Font("SansSerif", Font.BOLD, 16));

        dataset = new DefaultCategoryDataset();
        JFreeChart barChart = ChartFactory.createBarChart(
            null, "Service Type", "Appointments", dataset, 
            PlotOrientation.VERTICAL, false, true, false
        );

        // Styling
        barChart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = barChart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(230, 233, 237));
        plot.setOutlineVisible(false);

        // Bar Styling
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(52, 152, 219));
        renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
        renderer.setShadowVisible(false);

        // X-Axis Rotation Fix
     /*   CategoryAxis xAxis = plot.getDomainAxis();
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        xAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11)); */

        chartPanel = new ChartPanel(barChart);
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setBorder(BorderFactory.createLineBorder(new Color(230, 233, 237)));

        chartContainer.add(chartTitle, BorderLayout.NORTH);
        chartContainer.add(chartPanel, BorderLayout.CENTER);

        gbc.weightx = 0.7; // 70% width
        gbc.insets = new Insets(0, 0, 0, 0); // Reset insets
        bottomPanel.add(chartContainer, gbc);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JLabel createStatCard(JPanel parent, String title, String value, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 5, 0, 0, accentColor),
            new EmptyBorder(15, 20, 15, 20)
        ));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblTitle.setForeground(Color.GRAY);
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblValue.setForeground(new Color(44, 62, 80));
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(10));
        card.add(lblValue);
        parent.add(card);
        return lblValue;
    }

    public void refreshStats() {
        Map<String, Integer> stats = dashboardService.fetchDashboardStats();
        if (stats != null) {
            lblTotalPatients.setText(String.valueOf(stats.getOrDefault("totalPatients", 0)));
            lblPendingAppts.setText(String.valueOf(stats.getOrDefault("pendingAppts", 0)));
            lblTodayAppts.setText(String.valueOf(stats.getOrDefault("todayAppts", 0)));
            lblActiveStaff.setText(String.valueOf(stats.getOrDefault("activeStaff", 0)));
        }

        currentLogs = dashboardService.fetchRecentActivity(); 
        activityModel.clear();
        if (currentLogs != null) {
            for (String[] log : currentLogs) activityModel.addElement(log[0]);
        }

        Map<String, Integer> trends = dashboardService.fetchAppointmentTrends();
        dataset.clear();
        if (trends != null) {
            trends.forEach((service, count) -> dataset.addValue(count, "Appointments", service));
        }
    }

    private class ActivityCellRenderer extends DefaultListCellRenderer {
        private JPanel p = new JPanel(new BorderLayout(15, 0));
        private JLabel iconLabel = new JLabel();
        private JLabel textLabel = new JLabel();
        private JLabel timeLabel = new JLabel();

        public ActivityCellRenderer() {
            p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 0, 3, 0),
                BorderFactory.createLineBorder(new Color(230, 233, 237), 1, true)
            ));
            textLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
            timeLabel.setForeground(Color.GRAY);
            JPanel textContainer = new JPanel(new GridLayout(2, 1));
            textContainer.setOpaque(false);
            textContainer.add(textLabel);
            textContainer.add(timeLabel);
            p.add(iconLabel, BorderLayout.WEST);
            p.add(textContainer, BorderLayout.CENTER);
            iconLabel.setBorder(new EmptyBorder(0, 8, 0, 0));
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String fullText = (String) value;
            if (fullText != null && fullText.length() > 7) {
                String time = fullText.substring(1, 6);
                String action = fullText.substring(8);
                textLabel.setText(action);
                timeLabel.setText("at " + time);
                iconLabel.setText(" ● ");
                if (action.toLowerCase().contains("cancel")) iconLabel.setForeground(new Color(231, 76, 60));
                else if (action.toLowerCase().contains("approve")) iconLabel.setForeground(new Color(46, 204, 113));
                else iconLabel.setForeground(new Color(52, 152, 219));
            }
            p.setBackground(isSelected ? new Color(236, 240, 241) : Color.WHITE);
            return p;
        }
    }
}