package ru.filden.api.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.filden.api.db.DatabaseConnection;
import ru.filden.api.models.Group;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupDao {
    private static final Logger logger = LoggerFactory.getLogger(GroupDao.class);
    private final DatabaseConnection dbConnection;

    public GroupDao(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    // AddGroup
    public Group addGroup(Group group) throws SQLException {
        String sql = "INSERT INTO groups (number) VALUES (?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, group.getNumber());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        group.setId(generatedKeys.getInt(1));
                        logger.info("Group added successfully: id={}, number={}", group.getId(), group.getNumber());
                        return group;
                    }
                }
            }

            logger.warn("Failed to add group: number={}", group.getNumber());
            return null;
        } catch (SQLException e) {
            logger.error("Error adding group: number={}", group.getNumber(), e);
            throw new SQLException("Failed to add group", e);
        }
    }

    // AddGroup с явным указанием ID
    public Group addGroup(int id, String number) throws SQLException {
        String sql = "INSERT INTO groups (id, number) VALUES (?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.setString(2, number);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Group added successfully: id={}, number={}", id, number);
                return new Group(id, number);
            } else {
                logger.warn("Failed to add group: id={}, number={}", id, number);
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error adding group: id={}, number={}", id, number, e);
            throw new SQLException("Failed to add group", e);
        }
    }

    // DeleteGroup
    public boolean deleteGroup(int id) throws SQLException {
        String sql = "DELETE FROM groups WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Group deleted successfully: id={}", id);
                return true;
            } else {
                logger.warn("Group not found for deletion: id={}", id);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error deleting group: id={}", id, e);
            throw new SQLException("Failed to delete group", e);
        }
    }

    // DeleteGroup by number
    public boolean deleteGroupByNumber(String number) throws SQLException {
        String sql = "DELETE FROM groups WHERE number = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, number);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Group deleted successfully: number={}", number);
                return true;
            } else {
                logger.warn("Group not found for deletion: number={}", number);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error deleting group: number={}", number, e);
            throw new SQLException("Failed to delete group", e);
        }
    }

    // ChangeName(Id, new)
    public boolean changeGroupName(int id, String newNumber) throws SQLException {
        // Проверяем, не существует ли уже группа с таким номером
        if (isGroupNumberExists(newNumber)) {
            logger.warn("Group number already exists: {}", newNumber);
            return false;
        }

        String sql = "UPDATE groups SET number = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newNumber);
            pstmt.setInt(2, id);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Group name changed: id={}, newNumber={}", id, newNumber);
                return true;
            } else {
                logger.warn("Group not found for name change: id={}", id);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error changing group name: id={}", id, e);
            throw new SQLException("Failed to change group name", e);
        }
    }

    // GetGroup(name) - получение группы по номеру
    public Group getGroupByNumber(String number) throws SQLException {
        String sql = "SELECT * FROM groups WHERE number = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, number);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Group group = extractGroupFromResultSet(rs);
                logger.debug("Group found by number: {}", number);
                return group;
            } else {
                logger.debug("Group not found by number: {}", number);
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error getting group by number: {}", number, e);
            throw new SQLException("Failed to get group", e);
        }
    }

    // GetGroup by ID
    public Group getGroupById(int id) throws SQLException {
        String sql = "SELECT * FROM groups WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Group group = extractGroupFromResultSet(rs);
                logger.debug("Group found by id: {}", id);
                return group;
            } else {
                logger.debug("Group not found by id: {}", id);
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error getting group by id: {}", id, e);
            throw new SQLException("Failed to get group", e);
        }
    }

    public List<Group> getAllGroups() throws SQLException {
        String sql = "SELECT * FROM groups ORDER BY number";
        List<Group> groups = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                groups.add(extractGroupFromResultSet(rs));
            }

            logger.debug("Retrieved {} groups", groups.size());
            return groups;
        } catch (SQLException e) {
            logger.error("Error getting all groups", e);
            throw new SQLException("Failed to get all groups", e);
        }
    }

    public boolean isGroupNumberExists(String number) throws SQLException {
        String sql = "SELECT 1 FROM groups WHERE number = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, number);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    public int getStudentCountInGroup(String groupNumber) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM students WHERE group_number = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, groupNumber);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        } catch (SQLException e) {
            logger.error("Error getting student count for group: {}", groupNumber, e);
            throw new SQLException("Failed to get student count", e);
        }
    }

    public List<Group> getGroupsWithStudents() throws SQLException {
        String sql = "SELECT DISTINCT g.* FROM groups g " +
                "INNER JOIN students s ON g.number = s.group_number " +
                "ORDER BY g.number";
        List<Group> groups = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                groups.add(extractGroupFromResultSet(rs));
            }

            logger.debug("Retrieved {} groups with students", groups.size());
            return groups;
        } catch (SQLException e) {
            logger.error("Error getting groups with students", e);
            throw new SQLException("Failed to get groups with students", e);
        }
    }

    public List<Group> getEmptyGroups() throws SQLException {
        String sql = "SELECT g.* FROM groups g " +
                "LEFT JOIN students s ON g.number = s.group_number " +
                "WHERE s.id IS NULL " +
                "ORDER BY g.number";
        List<Group> groups = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                groups.add(extractGroupFromResultSet(rs));
            }

            logger.debug("Retrieved {} empty groups", groups.size());
            return groups;
        } catch (SQLException e) {
            logger.error("Error getting empty groups", e);
            throw new SQLException("Failed to get empty groups", e);
        }
    }

    private Group extractGroupFromResultSet(ResultSet rs) throws SQLException {
        return new Group(
                rs.getInt("id"),
                rs.getString("number")
        );
    }
}
