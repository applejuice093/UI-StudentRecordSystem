package coralcove.ui.components;

import coralcove.util.AppException;
import coralcove.util.DateUtils;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

public final class Ui {

    private Ui() {
    }

    public static JButton primary(String text) {
        JButton button = new JButton(text);
        button.setBackground(Theme.NAVY);
        button.setForeground(ColorSafe.WHITE);
        button.setFont(Theme.BODY);
        return button;
    }

    public static JButton secondary(String text) {
        JButton button = new JButton(text);
        button.setFont(Theme.BODY);
        return button;
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.TITLE);
        label.setForeground(Theme.TEXT);
        return label;
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "CoralCove", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "CoralCove", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "CoralCove", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    public static GridBagConstraints constraints(int row, int col) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = col;
        constraints.gridy = row;
        constraints.insets = new Insets(6, 8, 6, 8);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = col % 2 == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        constraints.weightx = col % 2 == 1 ? 1 : 0;
        return constraints;
    }

    public static JPanel padded() {
        JPanel panel = new JPanel();
        panel.setBackground(Theme.CONTENT);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        return panel;
    }

    public static JSpinner dateSpinner(LocalDate date) {
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd-MM-yyyy"));
        spinner.setFont(Theme.BODY);
        setDate(spinner, date);
        return spinner;
    }

    public static void setDate(JSpinner spinner, LocalDate date) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth(), 0, 0, 0);
        spinner.setValue(calendar.getTime());
    }

    public static LocalDate date(JSpinner spinner) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime((Date) spinner.getValue());
        return LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }

    public static JSpinner numberSpinner(int value, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, 1));
    }

    public static void run(Component parent, Callable<?> work, Consumer<Object> success) {
        parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Object, Void>() {
            @Override
            protected Object doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                parent.setCursor(Cursor.getDefaultCursor());
                try {
                    success.accept(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    String message = cause.getMessage() == null ? "The operation failed." : cause.getMessage();
                    error(parent, message);
                }
            }
        }.execute();
    }

    public static void cardBorder(JComponent component) {
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.LINE),
                new EmptyBorder(12, 12, 12, 12)));
        component.setBackground(Theme.CARD);
    }

    public static String todayText() {
        try {
            return DateUtils.display(DateUtils.today());
        } catch (AppException ex) {
            return "";
        }
    }

    public static void later(Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }

    /** Avoid a static import cycle with java.awt.Color in button styling. */
    private static final class ColorSafe {
        static final java.awt.Color WHITE = java.awt.Color.WHITE;
    }

    static {
        ZoneId.of("Asia/Kolkata");
    }
}
