package coralcove.ui.employees;

import coralcove.model.Employee;
import coralcove.service.EmployeeService;
import coralcove.ui.Refreshable;
import coralcove.ui.components.DataTable;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import coralcove.util.CurrencyUtils;
import coralcove.util.DateUtils;
import coralcove.util.Money;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class EmployeePanel extends JPanel implements Refreshable {
    private final EmployeeService service = new EmployeeService();
    private final JTextField search = new JTextField(12);
    private final JComboBox<String> department = new JComboBox<>();
    private final DataTable table = new DataTable("Id", "Name", "Department", "Designation", "Phone", "Salary", "Status", "Sample");
    private final DataTable attendance = new DataTable("Name", "Department", "Status", "Note");
    private List<Employee> rows = List.of();

    public EmployeePanel() {
        super(new BorderLayout(0, 8));
        setBackground(Theme.CONTENT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        department.addItem("");
        for (String value : EmployeeService.DEPARTMENTS) {
            department.addItem(value);
        }
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.setOpaque(false);
        north.add(search);
        north.add(department);
        var find = Ui.secondary("Search");
        find.addActionListener(event -> refresh());
        var add = Ui.primary("Add");
        add.addActionListener(event -> edit(null));
        var edit = Ui.secondary("Edit");
        edit.addActionListener(event -> {
            Employee employee = selected();
            if (employee != null) {
                edit(employee);
            }
        });
        var mark = Ui.secondary("Mark attendance");
        mark.addActionListener(event -> mark());
        north.add(find);
        north.add(add);
        north.add(edit);
        north.add(mark);
        add(north, BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(2, 1, 0, 8));
        center.setOpaque(false);
        center.add(table);
        center.add(attendance);
        add(center, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        String dept = (String) department.getSelectedItem();
        Ui.run(this, () -> service.search(search.getText(), dept == null || dept.isBlank() ? null : dept), result -> {
            rows = (List<Employee>) result;
            List<String[]> data = new ArrayList<>();
            for (Employee employee : rows) {
                data.add(new String[] {
                        String.valueOf(employee.getId()), employee.getName(), employee.getDepartment(),
                        employee.getDesignation(), employee.getPhone(), CurrencyUtils.format(employee.getSalary()),
                        employee.getStatus(), employee.isSample() ? "Sample" : ""
                });
            }
            table.setRows(data);
        });
        Ui.run(this, () -> service.attendanceOn(DateUtils.iso(DateUtils.today())), result -> attendance.setRows((List<String[]>) result));
    }

    private Employee selected() {
        int row = table.selectedRow();
        if (row < 0) {
            Ui.error(this, "Select an employee first.");
            return null;
        }
        return rows.get(row);
    }

    private void edit(Employee existing) {
        JTextField name = new JTextField(existing == null ? "" : existing.getName(), 16);
        JTextField phone = new JTextField(existing == null ? "" : nullToEmpty(existing.getPhone()), 16);
        JTextField email = new JTextField(existing == null ? "" : nullToEmpty(existing.getEmail()), 16);
        JTextField designation = new JTextField(existing == null ? "" : nullToEmpty(existing.getDesignation()), 16);
        JComboBox<String> dept = new JComboBox<>(EmployeeService.DEPARTMENTS.toArray(String[]::new));
        if (existing != null) {
            dept.setSelectedItem(existing.getDepartment());
        }
        JTextField salary = new JTextField(existing == null ? "0.00" : existing.getSalary().toPlainString(), 10);
        JTextField joined = new JTextField(existing == null ? DateUtils.display(DateUtils.today()) : DateUtils.display(existing.getJoiningDate()), 10);
        JComboBox<String> status = new JComboBox<>(new String[] {"ACTIVE", "INACTIVE"});
        if (existing != null) {
            status.setSelectedItem(existing.getStatus());
        }
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new javax.swing.JLabel("Name"));
        form.add(name);
        form.add(new javax.swing.JLabel("Phone"));
        form.add(phone);
        form.add(new javax.swing.JLabel("Email"));
        form.add(email);
        form.add(new javax.swing.JLabel("Designation"));
        form.add(designation);
        form.add(new javax.swing.JLabel("Department"));
        form.add(dept);
        form.add(new javax.swing.JLabel("Salary"));
        form.add(salary);
        form.add(new javax.swing.JLabel("Joined (dd-MM-yyyy)"));
        form.add(joined);
        form.add(new javax.swing.JLabel("Status"));
        form.add(status);
        if (JOptionPane.showConfirmDialog(this, form, "Employee", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        Employee employee = existing == null ? new Employee() : existing;
        employee.setName(name.getText());
        employee.setPhone(phone.getText());
        employee.setEmail(email.getText());
        employee.setDesignation(designation.getText());
        employee.setDepartment((String) dept.getSelectedItem());
        employee.setSalary(Money.of(salary.getText()));
        employee.setJoiningDate(joined.getText());
        employee.setStatus((String) status.getSelectedItem());
        Ui.run(this, () -> service.save(employee, existing == null), result -> refresh());
    }

    private void mark() {
        Employee employee = selected();
        if (employee == null) {
            return;
        }
        JComboBox<String> status = new JComboBox<>(new String[] {"PRESENT", "ABSENT", "LEAVE"});
        JTextField note = new JTextField(12);
        JPanel form = new JPanel(new GridLayout(2, 2));
        form.add(status);
        form.add(note);
        if (JOptionPane.showConfirmDialog(this, form, "Attendance for " + employee.getName(), JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        Ui.run(this, () -> {
            service.attendance(employee.getId(), DateUtils.iso(DateUtils.today()), (String) status.getSelectedItem(), note.getText());
            return null;
        }, result -> refresh());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
