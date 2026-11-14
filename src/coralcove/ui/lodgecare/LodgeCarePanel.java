package coralcove.ui.lodgecare;

import coralcove.model.Employee;
import coralcove.model.LodgeCareTask;
import coralcove.model.Role;
import coralcove.model.Lodge;
import coralcove.service.EmployeeService;
import coralcove.service.LodgeCareService;
import coralcove.service.LodgeService;
import coralcove.service.Session;
import coralcove.ui.Refreshable;
import coralcove.ui.components.DataTable;
import coralcove.ui.components.Theme;
import coralcove.ui.components.Ui;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class LodgeCarePanel extends JPanel implements Refreshable {
    private final LodgeCareService service = new LodgeCareService();
    private final DataTable tasks = new DataTable("Id", "Lodge", "Assignee", "Priority", "Status", "Inspection", "Sample");
    private final DataTable maintenance = new DataTable("Id", "Lodge", "Issue", "Status", "Opened");
    private final JComboBox<String> status = new JComboBox<>(new String[] {"", "PENDING", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "INSPECTION_REQUIRED"});
    private final JComboBox<String> priority = new JComboBox<>(new String[] {"", "LOW", "NORMAL", "HIGH"});
    private List<LodgeCareTask> rows = List.of();
    private final boolean canWrite = Session.get().getRole() == Role.ADMIN
            || Session.get().getRole() == Role.MANAGER
            || Session.get().getRole() == Role.ATTENDANT;

    public LodgeCarePanel() {
        super(new BorderLayout(0, 8));
        setBackground(Theme.CONTENT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.setOpaque(false);
        north.add(status);
        north.add(priority);
        var refresh = Ui.secondary("Refresh");
        refresh.addActionListener(event -> refresh());
        north.add(refresh);
        if (canWrite) {
            var create = Ui.primary("New task");
            create.addActionListener(event -> create());
            var update = Ui.secondary("Update task");
            update.addActionListener(event -> update());
            var report = Ui.secondary("Report maintenance");
            report.addActionListener(event -> report());
            var resolve = Ui.secondary("Resolve maintenance");
            resolve.addActionListener(event -> resolve());
            north.add(create);
            north.add(update);
            north.add(report);
            north.add(resolve);
        }
        add(north, BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(2, 1, 0, 8));
        center.setOpaque(false);
        center.add(tasks);
        center.add(maintenance);
        add(center, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Ui.run(this, () -> new Object[] {
                service.search((String) status.getSelectedItem(), (String) priority.getSelectedItem()),
                service.openMaintenance()
        }, result -> {
            Object[] pair = (Object[]) result;
            rows = (List<LodgeCareTask>) pair[0];
            List<String[]> data = new ArrayList<>();
            for (LodgeCareTask task : rows) {
                data.add(new String[] {
                        String.valueOf(task.getId()), task.getUnitCode(),
                        task.getAssigneeName() == null ? "" : task.getAssigneeName(),
                        task.getPriority(), task.getStatus(), task.getInspectionStatus(),
                        task.isSample() ? "Sample" : ""
                });
            }
            tasks.setRows(data);
            maintenance.setRows((List<String[]>) pair[1]);
        });
    }

    private void create() {
        Ui.run(this, () -> new Object[] {service.lodges(), service.staff()}, result -> {
            Object[] pair = (Object[]) result;
            JComboBox<Lodge> room = new JComboBox<>(((List<Lodge>) pair[0]).toArray(Lodge[]::new));
            JComboBox<Employee> staff = new JComboBox<>();
            staff.addItem(null);
            for (Employee employee : (List<Employee>) pair[1]) {
                staff.addItem(employee);
            }
            JComboBox<String> level = new JComboBox<>(new String[] {"LOW", "NORMAL", "HIGH"});
            level.setSelectedItem("NORMAL");
            JTextField notes = new JTextField(16);
            JPanel form = new JPanel(new GridLayout(4, 2));
            form.add(new javax.swing.JLabel("Lodge"));
            form.add(room);
            form.add(new javax.swing.JLabel("Assignee"));
            form.add(staff);
            form.add(new javax.swing.JLabel("Priority"));
            form.add(level);
            form.add(new javax.swing.JLabel("Notes"));
            form.add(notes);
            if (JOptionPane.showConfirmDialog(this, form, "Housekeeping task", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            Employee assignee = (Employee) staff.getSelectedItem();
            Ui.run(this, () -> {
                service.create(((Lodge) room.getSelectedItem()).getId(), assignee == null ? null : assignee.getId(),
                        (String) level.getSelectedItem(), notes.getText());
                return null;
            }, ignored -> refresh());
        });
    }

    private void update() {
        int row = tasks.selectedRow();
        if (row < 0) {
            Ui.error(this, "Select a task first.");
            return;
        }
        LodgeCareTask task = rows.get(row);
        JComboBox<String> next = new JComboBox<>(new String[] {"PENDING", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "INSPECTION_REQUIRED"});
        next.setSelectedItem(task.getStatus());
        JComboBox<String> inspect = new JComboBox<>(new String[] {"NONE", "PASSED", "FAILED"});
        inspect.setSelectedItem(task.getInspectionStatus());
        JPanel form = new JPanel(new GridLayout(2, 2));
        form.add(new javax.swing.JLabel("Status"));
        form.add(next);
        form.add(new javax.swing.JLabel("Inspection"));
        form.add(inspect);
        if (JOptionPane.showConfirmDialog(this, form, "Update task", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        Ui.run(this, () -> {
            service.update(task.getId(), task.getAssigneeId(), task.getPriority(), (String) next.getSelectedItem(),
                    (String) inspect.getSelectedItem(), task.getNotes());
            return null;
        }, ignored -> refresh());
    }

    private void report() {
        Ui.run(this, () -> service.lodges(), result -> {
            JComboBox<Lodge> room = new JComboBox<>(((List<Lodge>) result).toArray(Lodge[]::new));
            JTextField description = new JTextField(18);
            JPanel form = new JPanel(new GridLayout(2, 2));
            form.add(new javax.swing.JLabel("Lodge"));
            form.add(room);
            form.add(new javax.swing.JLabel("Issue"));
            form.add(description);
            if (JOptionPane.showConfirmDialog(this, form, "Maintenance", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            Ui.run(this, () -> {
                service.reportMaintenance(((Lodge) room.getSelectedItem()).getId(), description.getText());
                return null;
            }, ignored -> refresh());
        });
    }

    private void resolve() {
        int row = maintenance.selectedRow();
        if (row < 0) {
            Ui.error(this, "Select a maintenance alert first.");
            return;
        }
        long id = Long.parseLong(maintenance.value(row, 0));
        String unitCode = maintenance.value(row, 1);
        Ui.run(this, () -> {
            Lodge match = service.lodges().stream()
                    .filter(room -> room.getUnitCode().equals(unitCode))
                    .findFirst()
                    .orElseThrow(() -> new coralcove.util.AppException("Lodge was not found."));
            service.resolveMaintenance(id, match.getId());
            return null;
        }, ignored -> refresh());
    }
}
