package studentmanagementsystem;

/**
 * Application entry point: opens the login window.
 */
public class StudentManagementSystem {

    /**
     * @param args the command line arguments (unused)
     */
    public static void main(String[] args) {
        // Swing components must be created on the Event Dispatch Thread.
        java.awt.EventQueue.invokeLater(() -> new LoginForm().setVisible(true));
    }
}
