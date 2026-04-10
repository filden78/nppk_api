package ru.filden.api.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.filden.api.db.DatabaseConnection;
import ru.filden.api.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);
    private final DatabaseConnection dbConnection;

    public UserDao(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    // AddUser(id, login, pass, role)
    public User addUser(int id, String login, String password, String role) throws SQLException {
        String sql = "INSERT INTO users (id, login, password, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, id);
            pstmt.setString(2, login);
            pstmt.setString(3, password);
            pstmt.setString(4, role);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("User added successfully: id={}, login={}", id, login);
                return new User(id, login, password, role);
            } else {
                logger.warn("Failed to add user: id={}", id);
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error adding user: id={}", id, e);
            throw new SQLException("Failed to add user", e);
        }
    }

    // DeleteUser(id)
    public boolean deleteUser(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("User deleted successfully: id={}", id);
                return true;
            } else {
                logger.warn("User not found for deletion: id={}", id);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error deleting user: id={}", id, e);
            throw new SQLException("Failed to delete user", e);
        }
    }

    // UpdateRole(id, role)
    public boolean updateRole(int id, String role) throws SQLException {
        String sql = "UPDATE users SET role = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role);
            pstmt.setInt(2, id);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("User role updated successfully: id={}, newRole={}", id, role);
                return true;
            } else {
                logger.warn("User not found for role update: id={}", id);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error updating user role: id={}", id, e);
            throw new SQLException("Failed to update user role", e);
        }
    }

    // UpdateUserPass(id, newPass)
    public boolean updateUserPass(int id, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newPassword);
            pstmt.setInt(2, id);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("User password updated successfully: id={}", id);
                return true;
            } else {
                logger.warn("User not found for password update: id={}", id);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error updating user password: id={}", id, e);
            throw new SQLException("Failed to update user password", e);
        }
    }

    // UpdateUserLogin(id, newLogin)
    public boolean updateUserLogin(int id, String newLogin) throws SQLException {
        String sql = "UPDATE users SET login = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newLogin);
            pstmt.setInt(2, id);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("User login updated successfully: id={}, newLogin={}", id, newLogin);
                return true;
            } else {
                logger.warn("User not found for login update: id={}", id);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error updating user login: id={}", id, e);
            throw new SQLException("Failed to update user login", e);
        }
    }

    // GetUser(id)
    public User getUserById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = extractUserFromResultSet(rs);
                logger.debug("User found by id: {}", id);
                return user;
            } else {
                logger.debug("User not found by id: {}", id);
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error getting user by id: {}", id, e);
            throw new SQLException("Failed to get user", e);
        }
    }

    // GetUser(login)
    public User getUserByLogin(String login) throws SQLException {
        String sql = "SELECT * FROM users WHERE login = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = extractUserFromResultSet(rs);
                logger.debug("User found by login: {}", login);
                return user;
            } else {
                logger.debug("User not found by login: {}", login);
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error getting user by login: {}", login, e);
            throw new SQLException("Failed to get user", e);
        }
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY id";
        List<User> users = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }

            logger.debug("Retrieved {} users", users.size());
            return users;
        } catch (SQLException e) {
            logger.error("Error getting all users", e);
            throw new SQLException("Failed to get all users", e);
        }
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("login"),
                rs.getString("password"),
                rs.getString("role")
        );
    }
}
