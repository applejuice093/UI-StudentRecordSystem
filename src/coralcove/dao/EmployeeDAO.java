package coralcove.dao;

import coralcove.model.Employee;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class EmployeeDAO {

    public List<Employee> search(Connection connection, String text, String department) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM employees WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            sql.append(" AND (name LIKE ? OR phone LIKE ? OR email LIKE ? OR designation LIKE ?)");
            String like = "%" + text.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (department != null && !department.isBlank()) {
            sql.append(" AND department=?");
            args.add(department);
        }
        Long resortFilter = coralcove.service.Session.resortFilter();
        if (resortFilter != null) {
            sql.append(" AND resort_id=").append(resortFilter.longValue());
        } else {
            sql.append(" AND resort_id IN (SELECT id FROM resorts WHERE active=1)");
        }
        sql.append(" ORDER BY name");
        try (var ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                ps.setString(i + 1, args.get(i));
            }
            try (var rs = ps.executeQuery()) {
                List<Employee> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(map(rs));
                }
                return rows;
            }
        }
    }

    public Employee find(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT * FROM employees WHERE id=?")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public long insert(Connection connection, Employee employee) throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO employees(resort_id, name, phone, email, designation, department, salary, joining_date, status, is_sample)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, employee);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void update(Connection connection, Employee employee) throws SQLException {
        try (var ps = connection.prepareStatement("""
                UPDATE employees SET resort_id=?, name=?, phone=?, email=?, designation=?, department=?, salary=?,
                    joining_date=?, status=?, is_sample=? WHERE id=?
                """)) {
            fill(ps, employee);
            ps.setLong(11, employee.getId());
            ps.executeUpdate();
        }
    }

    public void markAttendance(Connection connection, long employeeId, String date, String status, String note)
            throws SQLException {
        try (var ps = connection.prepareStatement("""
                INSERT INTO employee_attendance(employee_id, work_date, status, note) VALUES (?,?,?,?)
                ON CONFLICT(employee_id, work_date) DO UPDATE SET status=excluded.status, note=excluded.note
                """)) {
            ps.setLong(1, employeeId);
            ps.setString(2, date);
            ps.setString(3, status);
            ps.setString(4, note);
            ps.executeUpdate();
        }
    }

    public List<String[]> attendance(Connection connection, String date) throws SQLException {
        try (var ps = connection.prepareStatement("""
                SELECT e.name, e.department, a.status, a.note
                FROM employee_attendance a JOIN employees e ON e.id=a.employee_id
                WHERE a.work_date=? ORDER BY e.name
                """)) {
            ps.setString(1, date);
            try (var rs = ps.executeQuery()) {
                List<String[]> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new String[] {rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)});
                }
                return rows;
            }
        }
    }

    private void fill(java.sql.PreparedStatement ps, Employee employee) throws SQLException {
        ps.setLong(1, employee.getResortId());
        ps.setString(2, employee.getName());
        ps.setString(3, employee.getPhone());
        ps.setString(4, employee.getEmail());
        ps.setString(5, employee.getDesignation());
        ps.setString(6, employee.getDepartment());
        Jdbc.money(ps, 7, employee.getSalary());
        ps.setString(8, employee.getJoiningDate());
        ps.setString(9, employee.getStatus());
        ps.setInt(10, employee.isSample() ? 1 : 0);
    }

    private Employee map(ResultSet rs) throws SQLException {
        Employee employee = new Employee();
        employee.setId(rs.getLong("id"));
        employee.setResortId(rs.getLong("resort_id"));
        employee.setName(rs.getString("name"));
        employee.setPhone(rs.getString("phone"));
        employee.setEmail(rs.getString("email"));
        employee.setDesignation(rs.getString("designation"));
        employee.setDepartment(rs.getString("department"));
        employee.setSalary(Jdbc.money(rs, "salary"));
        employee.setJoiningDate(rs.getString("joining_date"));
        employee.setStatus(rs.getString("status"));
        employee.setSample(rs.getInt("is_sample") == 1);
        return employee;
    }
}
