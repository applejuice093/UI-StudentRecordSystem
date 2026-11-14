package coralcove.service;

import coralcove.dao.EmployeeDAO;
import coralcove.database.DatabaseManager;
import coralcove.database.Transactions;
import coralcove.model.AppModule;
import coralcove.model.Employee;
import coralcove.util.AppException;
import coralcove.util.DateUtils;
import coralcove.util.Money;
import coralcove.util.ValidationUtils;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public final class EmployeeService {

    public static final List<String> DEPARTMENTS = List.of(
            "Reception", "Housekeeping", "Maintenance", "Restaurant", "Security", "Management");
    private static final Set<String> ATTENDANCE = Set.of("PRESENT", "ABSENT", "LEAVE");
    private final EmployeeDAO employees = new EmployeeDAO();

    public List<Employee> search(String text, String department) {
        AuthorizationService.require(AppModule.EMPLOYEES);
        try (var connection = DatabaseManager.getConnection()) {
            return employees.search(connection, text, department);
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }

    public Employee save(Employee employee, boolean create) {
        AuthorizationService.require(AppModule.EMPLOYEES);
        employee.setName(ValidationUtils.required(employee.getName(), "Name"));
        if (!DEPARTMENTS.contains(employee.getDepartment())) {
            throw new AppException("Choose a department from the list.");
        }
        ValidationUtils.email(employee.getEmail(), false);
        Money.requireNonNegative(employee.getSalary(), "Salary");
        if (employee.getJoiningDate() != null && !employee.getJoiningDate().isBlank()) {
            employee.setJoiningDate(DateUtils.iso(DateUtils.parse(employee.getJoiningDate())));
        }
        if (employee.getStatus() == null) {
            employee.setStatus("ACTIVE");
        }
        Employee saved = Transactions.in(connection -> {
            if (!create) {
                Employee current = employees.find(connection, employee.getId());
                if (current == null) {
                    throw new AppException("Employee was not found.");
                }
                ResortScope.assertOwns(current.getResortId());
            }
            employee.setResortId(ResortScope.resolve(connection, employee.getResortId()));
            if (create) {
                employee.setId(employees.insert(connection, employee));
            } else {
                employees.update(connection, employee);
            }
            return employees.find(connection, employee.getId());
        });
        Session.dataChanged();
        return saved;
    }

    public void attendance(long employeeId, String date, String status, String note) {
        AuthorizationService.require(AppModule.EMPLOYEES);
        if (!ATTENDANCE.contains(status)) {
            throw new AppException("Attendance status is invalid.");
        }
        String iso = DateUtils.iso(DateUtils.parse(date));
        Transactions.in(connection -> {
            employees.markAttendance(connection, employeeId, iso, status, note);
            return null;
        });
    }

    public List<String[]> attendanceOn(String date) {
        AuthorizationService.require(AppModule.EMPLOYEES);
        try (var connection = DatabaseManager.getConnection()) {
            return employees.attendance(connection, DateUtils.iso(DateUtils.parse(date)));
        } catch (SQLException ex) {
            throw new AppException(Transactions.sqlMessage(ex), ex);
        }
    }
}
