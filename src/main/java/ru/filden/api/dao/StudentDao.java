package ru.filden.api.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.filden.api.db.DatabaseConnection;
import ru.filden.api.models.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDao {
    private static final Logger logger = LoggerFactory.getLogger(StudentDao.class);
    private final DatabaseConnection dbConnection;

    public StudentDao(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    // AddStudent (юзер уже должен быть!)
    public Student addStudent(Student student) throws SQLException {
        String sql = "INSERT INTO students (fio, group_number, is_duty, count_duty, user_login) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, student.getFio());
            pstmt.setString(2, student.getGroupNumber());
            pstmt.setBoolean(3, student.isDuty());
            pstmt.setInt(4, student.getCountDuty());
            pstmt.setString(5, student.getUserLogin());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        student.setId(generatedKeys.getInt(1));
                        logger.info("Student added successfully: id={}, fio={}", student.getId(), student.getFio());
                        return student;
                    }
                }
            }

            logger.warn("Failed to add student: fio={}", student.getFio());
            return null;
        } catch (SQLException e) {
            logger.error("Error adding student: fio={}", student.getFio(), e);
            throw new SQLException("Failed to add student", e);
        }
    }

    // DeleteStudent(id)
    public boolean deleteStudent(int id) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Student deleted successfully: id={}", id);
                return true;
            } else {
                logger.warn("Student not found for deletion: id={}", id);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error deleting student: id={}", id, e);
            throw new SQLException("Failed to delete student", e);
        }
    }

    // UpdateDuty(id, count)
    public boolean updateDuty(int id, int count) throws SQLException {
        String sql = "UPDATE students SET count_duty = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, count);
            pstmt.setInt(2, id);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Student duty count updated: id={}, countDuty={}", id, count);
                return true;
            } else {
                logger.warn("Student not found for duty update: id={}", id);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error updating student duty count: id={}", id, e);
            throw new SQLException("Failed to update student duty count", e);
        }
    }

    // UpdateStudentFio(id, newFio)
    public boolean updateStudentFio(int id, String newFio) throws SQLException {
        String sql = "UPDATE students SET fio = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newFio);
            pstmt.setInt(2, id);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Student FIO updated: id={}, newFio={}", id, newFio);
                return true;
            } else {
                logger.warn("Student not found for FIO update: id={}", id);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error updating student FIO: id={}", id, e);
            throw new SQLException("Failed to update student FIO", e);
        }
    }

    // ChangeGroup(id, newGroupName)
    public boolean changeGroup(int id, String newGroupName) throws SQLException {
        // Сначала проверяем, существует ли группа
        if (!groupExists(newGroupName)) {
            logger.warn("Group does not exist: {}", newGroupName);
            return false;
        }

        String sql = "UPDATE students SET group_number = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newGroupName);
            pstmt.setInt(2, id);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Student group changed: id={}, newGroup={}", id, newGroupName);
                return true;
            } else {
                logger.warn("Student not found for group change: id={}", id);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error changing student group: id={}", id, e);
            throw new SQLException("Failed to change student group", e);
        }
    }

    // ChangeDutyStatus(id, bool)
    public boolean changeDutyStatus(int id, boolean isDuty) throws SQLException {
        String sql = "UPDATE students SET is_duty = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, isDuty);
            pstmt.setInt(2, id);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Student duty status changed: id={}, isDuty={}", id, isDuty);
                return true;
            } else {
                logger.warn("Student not found for duty status change: id={}", id);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error changing student duty status: id={}", id, e);
            throw new SQLException("Failed to change student duty status", e);
        }
    }

    // GetStudent(Id)
    public Student getStudentById(int id) throws SQLException {
        String sql = "SELECT s.*, u.login as user_login_field FROM students s " +
                "LEFT JOIN users u ON s.user_login = u.login " +
                "WHERE s.id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Student student = extractStudentFromResultSet(rs);
                logger.debug("Student found by id: {}", id);
                return student;
            } else {
                logger.debug("Student not found by id: {}", id);
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error getting student by id: {}", id, e);
            throw new SQLException("Failed to get student", e);
        }
    }

    // GetStudent(fio)
    public List<Student> getStudentByFio(String fio) throws SQLException {
        String sql = "SELECT s.*, u.login as user_login_field FROM students s " +
                "LEFT JOIN users u ON s.user_login = u.login " +
                "WHERE s.fio LIKE ?";

        List<Student> students = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + fio + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                students.add(extractStudentFromResultSet(rs));
            }

            logger.debug("Found {} students by FIO: {}", students.size(), fio);
            return students;
        } catch (SQLException e) {
            logger.error("Error getting student by FIO: {}", fio, e);
            throw new SQLException("Failed to get student by FIO", e);
        }
    }

    // GetStudent(login)
    public Student getStudentByUserLogin(String login) throws SQLException {
        String sql = "SELECT s.*, u.login as user_login_field FROM students s " +
                "LEFT JOIN users u ON s.user_login = u.login " +
                "WHERE s.user_login = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Student student = extractStudentFromResultSet(rs);
                logger.debug("Student found by user login: {}", login);
                return student;
            } else {
                logger.debug("Student not found by user login: {}", login);
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error getting student by user login: {}", login, e);
            throw new SQLException("Failed to get student by user login", e);
        }
    }

    public List<Student> getAllStudents() throws SQLException {
        String sql = "SELECT s.*, u.login as user_login_field FROM students s " +
                "LEFT JOIN users u ON s.user_login = u.login " +
                "ORDER BY s.id";
        List<Student> students = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                students.add(extractStudentFromResultSet(rs));
            }

            logger.debug("Retrieved {} students", students.size());
            return students;
        } catch (SQLException e) {
            logger.error("Error getting all students", e);
            throw new SQLException("Failed to get all students", e);
        }
    }

    public List<Student> getStudentsByGroup(String groupNumber) throws SQLException {
        String sql = "SELECT s.*, u.login as user_login_field FROM students s " +
                "LEFT JOIN users u ON s.user_login = u.login " +
                "WHERE s.group_number = ? ORDER BY s.fio";
        List<Student> students = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, groupNumber);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                students.add(extractStudentFromResultSet(rs));
            }

            logger.debug("Found {} students in group: {}", students.size(), groupNumber);
            return students;
        } catch (SQLException e) {
            logger.error("Error getting students by group: {}", groupNumber, e);
            throw new SQLException("Failed to get students by group", e);
        }
    }

    public List<Student> getDutyStudents() throws SQLException {
        String sql = "SELECT s.*, u.login as user_login_field FROM students s " +
                "LEFT JOIN users u ON s.user_login = u.login " +
                "WHERE s.is_duty = 1 ORDER BY s.fio";
        List<Student> students = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                students.add(extractStudentFromResultSet(rs));
            }

            logger.debug("Found {} duty students", students.size());
            return students;
        } catch (SQLException e) {
            logger.error("Error getting duty students", e);
            throw new SQLException("Failed to get duty students", e);
        }
    }

    private boolean groupExists(String groupNumber) throws SQLException {
        String sql = "SELECT 1 FROM groups WHERE number = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, groupNumber);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    private Student extractStudentFromResultSet(ResultSet rs) throws SQLException {
        return new Student(
                rs.getInt("id"),
                rs.getString("fio"),
                rs.getString("group_number"),
                rs.getBoolean("is_duty"),
                rs.getInt("count_duty"),
                rs.getString("user_login")
        );
    }
}
