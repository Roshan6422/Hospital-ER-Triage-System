import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.ArrayList;

// --- 1. PATIENT CLASS (Serializable for File Saving) ---
class Patient implements Serializable {
    String name;
    int severity;
    int ticketNumber;
    String condition;

    public Patient(String name, int severity, int ticketNumber, String condition) {
        this.name = name;
        this.severity = severity;
        this.ticketNumber = ticketNumber;
        this.condition = condition;
    }
}

// --- 2. COMPARATOR LOGIC ---
class PatientComparator implements Comparator<Patient> {
    @Override
    public int compare(Patient p1, Patient p2) {
        if (p1.severity != p2.severity) {
            return p1.severity - p2.severity; // Critical (1) comes first
        }
        return p1.ticketNumber - p2.ticketNumber; // Then FCFS
    }
}

// --- 3. MAIN SYSTEM CLASS ---
public class HospitalSystemPro extends JFrame {

    private PriorityQueue<Patient> erQueue;
    private int tokenCounter = 1;
    private int treatedCount = 0; // Statistics

    // GUI Components
    private JTextField nameField;
    private JComboBox<String> severityBox;
    private DefaultTableModel tableModel;
    private JLabel lblWaiting, lblCritical, lblTreated;

    // File Path
    private final String FILE_NAME = "hospital_data.txt";

    public HospitalSystemPro() {
        erQueue = new PriorityQueue<>(new PatientComparator());

        // Load Data on Startup
        loadData();

        // Window Setup
        setTitle("Advanced ER Triage System (Group Project)");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- TOP PANEL: INPUTS ---
        JPanel topPanel = new JPanel(new GridLayout(2, 1));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Patient Registration"));
        inputPanel.setBackground(new Color(240, 248, 255));

        inputPanel.add(new JLabel("Name:"));
        nameField = new JTextField(15);
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Condition:"));
        String[] severities = {
                "1 - Critical (Life Threatening)",
                "2 - High (Severe Injury)",
                "3 - Medium (Flu/Fever)",
                "4 - Low (Checkup)"
        };
        severityBox = new JComboBox<>(severities);
        inputPanel.add(severityBox);

        JButton btnAdd = new JButton("Admit Patient");
        btnAdd.setBackground(new Color(0, 100, 0));
        btnAdd.setForeground(Color.WHITE);
        inputPanel.add(btnAdd);

        // --- STATS PANEL ---
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        statsPanel.setBackground(new Color(255, 250, 205));
        statsPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

        lblWaiting = new JLabel("Waiting: 0");
        lblWaiting.setFont(new Font("Arial", Font.BOLD, 14));
        lblCritical = new JLabel("Critical: 0");
        lblCritical.setForeground(Color.RED);
        lblCritical.setFont(new Font("Arial", Font.BOLD, 14));
        lblTreated = new JLabel("Total Treated: " + treatedCount);
        lblTreated.setForeground(new Color(0, 100, 0));
        lblTreated.setFont(new Font("Arial", Font.BOLD, 14));

        statsPanel.add(lblWaiting);
        statsPanel.add(lblCritical);
        statsPanel.add(lblTreated);

        topPanel.add(inputPanel);
        topPanel.add(statsPanel);
        add(topPanel, BorderLayout.NORTH);

        // --- CENTER PANEL: TABLE ---
        String[] cols = { "Token", "Severity", "Condition", "Patient Name" };
        tableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));

        // Color Coding Renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String sev = (String) table.getValueAt(row, 1);
                if (sev.startsWith("1"))
                    c.setBackground(new Color(255, 180, 180)); // Red
                else if (sev.startsWith("2"))
                    c.setBackground(new Color(255, 220, 150)); // Orange
                else
                    c.setBackground(Color.WHITE);
                return c;
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- BOTTOM PANEL: ACTIONS ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnTreat = new JButton("Treat Next Patient");
        btnTreat.setFont(new Font("Arial", Font.BOLD, 14));
        btnTreat.setBackground(new Color(178, 34, 34));
        btnTreat.setForeground(Color.WHITE);

        JButton btnSave = new JButton("Save & Exit");
        btnSave.setFont(new Font("Arial", Font.BOLD, 14));

        bottomPanel.add(btnSave);
        bottomPanel.add(btnTreat);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- ACTION LISTENERS ---
        btnAdd.addActionListener(e -> addPatient());
        btnTreat.addActionListener(e -> treatPatient());
        btnSave.addActionListener(e -> {
            saveData();
            System.exit(0);
        });

        // Add window listener to save on close X
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveData();
            }
        });

        updateStats(); // Init stats
    }

    // --- LOGIC METHODS ---

    private void addPatient() {
        String name = nameField.getText().trim();
        if (name.isEmpty())
            return;

        int index = severityBox.getSelectedIndex();
        int sev = index + 1;
        String cond = (String) severityBox.getSelectedItem();

        Patient p = new Patient(name, sev, tokenCounter++, cond);
        erQueue.add(p);

        nameField.setText("");
        refreshTable();
        updateStats();
    }

    private void treatPatient() {
        if (erQueue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Queue is empty!");
            return;
        }
        Patient p = erQueue.poll();
        treatedCount++;

        JOptionPane.showMessageDialog(this, "Treating: " + p.name + "\nSeverity: " + p.severity);
        refreshTable();
        updateStats();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        PriorityQueue<Patient> temp = new PriorityQueue<>(erQueue); // Copy for display

        // Sorting happens here automatically
        while (!temp.isEmpty()) {
            Patient p = temp.poll(); // Pulls highest priority
            tableModel.addRow(new Object[] { p.ticketNumber,
                    p.severity + " (" + (p.severity == 1 ? "Critical" : "Normal") + ")", p.condition, p.name });
        }
    }

    private void updateStats() {
        lblWaiting.setText("Waiting: " + erQueue.size());

        // Count Critical
        long criticalCount = erQueue.stream().filter(p -> p.severity == 1).count();
        lblCritical.setText("Critical: " + criticalCount);
        lblTreated.setText("Total Treated: " + treatedCount);
    }

    // --- FILE HANDLING METHODS (COMPLEXITY UPGRADE) ---

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            // Convert Queue to List for saving
            ArrayList<Patient> list = new ArrayList<>(erQueue);
            oos.writeObject(list);
            oos.writeInt(tokenCounter);
            oos.writeInt(treatedCount);
            System.out.println("Data Saved Successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        File f = new File(FILE_NAME);
        if (!f.exists())
            return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            ArrayList<Patient> list = (ArrayList<Patient>) ois.readObject();
            erQueue.addAll(list);
            tokenCounter = ois.readInt();
            treatedCount = ois.readInt();
            refreshTable();
        } catch (Exception e) {
            System.out.println("No previous data found or error loading.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HospitalSystemPro().setVisible(true));
    }
}