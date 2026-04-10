package ru.filden.api.endpoints;

import ru.filden.api.dao.GroupDao;
import ru.filden.api.dao.StudentDao;
import ru.filden.api.dao.UserDao;
import ru.filden.api.models.Group;
import ru.filden.api.models.Student;
import ru.filden.api.models.User;
import ru.filden.api.utils.JsonUtil;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class RestEndpoints {
    private static final Logger logger = LoggerFactory.getLogger(RestEndpoints.class);

    private final UserDao userDao;
    private final StudentDao studentDao;
    private final GroupDao groupDao;

    public RestEndpoints(UserDao userDao, StudentDao studentDao, GroupDao groupDao) {
        this.userDao = userDao;
        this.studentDao = studentDao;
        this.groupDao = groupDao;
    }

    public void registerEndpoints() {
        registerUserEndpoints();
        registerStudentEndpoints();
        registerGroupEndpoints();

        // Health check endpoint
        Spark.get("/api/health", (req, res) -> {
            JsonUtil.setJsonResponse(res);
            JsonObject response = new JsonObject();
            response.addProperty("status", "OK");
            response.addProperty("timestamp", System.currentTimeMillis());
            return JsonUtil.toJson(response);
        });
    }

    private void registerUserEndpoints() {

        // GET /api/users - получить всех пользователей
        Spark.get("/api/users", (req, res) -> {
            logger.info("GET /api/users - Fetching all users");
            JsonUtil.setJsonResponse(res);
            try {
                List<User> users = userDao.getAllUsers();
                res.status(200);
                return JsonUtil.successResponse(users);
            } catch (SQLException e) {
                logger.error("Error fetching users", e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // GET /api/users/:id - получить пользователя по ID
        Spark.get("/api/users/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("GET /api/users/{} - Fetching user by ID", id);
            JsonUtil.setJsonResponse(res);
            try {
                User user = userDao.getUserById(id);
                if (user != null) {
                    res.status(200);
                    return JsonUtil.successResponse(user);
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("User not found with id: " + id, 404);
                }
            } catch (SQLException e) {
                logger.error("Error fetching user by id: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // GET /api/users/login/:login - получить пользователя по логину
        Spark.get("/api/users/login/:login", (req, res) -> {
            String login = req.params(":login");
            logger.info("GET /api/users/login/{} - Fetching user by login", login);
            JsonUtil.setJsonResponse(res);
            try {
                User user = userDao.getUserByLogin(login);
                if (user != null) {
                    res.status(200);
                    return JsonUtil.successResponse(user);
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("User not found with login: " + login, 404);
                }
            } catch (SQLException e) {
                logger.error("Error fetching user by login: {}", login, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // POST /api/users - создать нового пользователя
        Spark.post("/api/users", (req, res) -> {
            logger.info("POST /api/users - Creating new user");
            JsonUtil.setJsonResponse(res);
            try {
                User userData = JsonUtil.fromJson(req.body(), User.class);

                // Валидация
                if (userData.getLogin() == null || userData.getPassword() == null || userData.getRole() == null) {
                    res.status(400);
                    return JsonUtil.errorResponse("Login, password and role are required", 400);
                }

                User created = userDao.addUser(
                        userData.getId(),
                        userData.getLogin(),
                        userData.getPassword(),
                        userData.getRole()
                );

                if (created != null) {
                    res.status(201);
                    return JsonUtil.successResponse(created);
                } else {
                    res.status(400);
                    return JsonUtil.errorResponse("Failed to create user", 400);
                }
            } catch (SQLException e) {
                logger.error("Error creating user", e);
                if (e.getMessage().contains("duplicate")) {
                    res.status(409);
                    return JsonUtil.errorResponse("User with this login or id already exists", 409);
                }
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // PUT /api/users/:id/role - обновить роль пользователя
        Spark.put("/api/users/:id/role", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("PUT /api/users/{}/role - Updating user role", id);
            JsonUtil.setJsonResponse(res);
            try {
                JsonObject request = JsonUtil.fromJson(req.body(), JsonObject.class);
                String role = request.get("role").getAsString();

                if (role == null || role.isEmpty()) {
                    res.status(400);
                    return JsonUtil.errorResponse("Role is required", 400);
                }

                boolean updated = userDao.updateRole(id, role);
                if (updated) {
                    res.status(200);
                    return JsonUtil.successResponse(Map.of("message", "Role updated successfully"));
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("User not found with id: " + id, 404);
                }
            } catch (SQLException e) {
                logger.error("Error updating user role: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // PUT /api/users/:id/password - обновить пароль пользователя
        Spark.put("/api/users/:id/password", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("PUT /api/users/{}/password - Updating user password", id);
            JsonUtil.setJsonResponse(res);
            try {
                JsonObject request = JsonUtil.fromJson(req.body(), JsonObject.class);
                String newPassword = request.get("password").getAsString();

                if (newPassword == null || newPassword.isEmpty()) {
                    res.status(400);
                    return JsonUtil.errorResponse("Password is required", 400);
                }

                boolean updated = userDao.updateUserPass(id, newPassword);
                if (updated) {
                    res.status(200);
                    return JsonUtil.successResponse(Map.of("message", "Password updated successfully"));
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("User not found with id: " + id, 404);
                }
            } catch (SQLException e) {
                logger.error("Error updating user password: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // PUT /api/users/:id/login - обновить логин пользователя
        Spark.put("/api/users/:id/login", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("PUT /api/users/{}/login - Updating user login", id);
            JsonUtil.setJsonResponse(res);
            try {
                JsonObject request = JsonUtil.fromJson(req.body(), JsonObject.class);
                String newLogin = request.get("login").getAsString();

                if (newLogin == null || newLogin.isEmpty()) {
                    res.status(400);
                    return JsonUtil.errorResponse("Login is required", 400);
                }

                boolean updated = userDao.updateUserLogin(id, newLogin);
                if (updated) {
                    res.status(200);
                    return JsonUtil.successResponse(Map.of("message", "Login updated successfully"));
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("User not found with id: " + id, 404);
                }
            } catch (SQLException e) {
                logger.error("Error updating user login: {}", id, e);
                if (e.getMessage().contains("duplicate")) {
                    res.status(409);
                    return JsonUtil.errorResponse("Login already exists", 409);
                }
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // DELETE /api/users/:id - удалить пользователя
        Spark.delete("/api/users/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("DELETE /api/users/{} - Deleting user", id);
            JsonUtil.setJsonResponse(res);
            try {
                boolean deleted = userDao.deleteUser(id);
                if (deleted) {
                    res.status(200);
                    return JsonUtil.successResponse(Map.of("message", "User deleted successfully"));
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("User not found with id: " + id, 404);
                }
            } catch (SQLException e) {
                logger.error("Error deleting user: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });
    }

    private void registerStudentEndpoints() {

        // GET /api/students - получить всех студентов
        Spark.get("/api/students", (req, res) -> {
            logger.info("GET /api/students - Fetching all students");
            JsonUtil.setJsonResponse(res);
            try {
                List<Student> students = studentDao.getAllStudents();
                res.status(200);
                return JsonUtil.successResponse(students);
            } catch (SQLException e) {
                logger.error("Error fetching students", e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // GET /api/students/:id - получить студента по ID
        Spark.get("/api/students/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("GET /api/students/{} - Fetching student by ID", id);
            JsonUtil.setJsonResponse(res);
            try {
                Student student = studentDao.getStudentById(id);
                if (student != null) {
                    res.status(200);
                    return JsonUtil.successResponse(student);
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("Student not found with id: " + id, 404);
                }
            } catch (SQLException e) {
                logger.error("Error fetching student by id: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // GET /api/students/search/fio - поиск студентов по ФИО
        Spark.get("/api/students/search/fio", (req, res) -> {
            String fio = req.queryParams("q");
            logger.info("GET /api/students/search/fio - Searching students by FIO: {}", fio);
            JsonUtil.setJsonResponse(res);
            try {
                if (fio == null || fio.isEmpty()) {
                    res.status(400);
                    return JsonUtil.errorResponse("Query parameter 'q' is required", 400);
                }

                List<Student> students = studentDao.getStudentByFio(fio);
                res.status(200);
                return JsonUtil.successResponse(students);
            } catch (SQLException e) {
                logger.error("Error searching students by FIO: {}", fio, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // GET /api/students/login/:login - получить студента по логину пользователя
        Spark.get("/api/students/login/:login", (req, res) -> {
            String login = req.params(":login");
            logger.info("GET /api/students/login/{} - Fetching student by user login", login);
            JsonUtil.setJsonResponse(res);
            try {
                Student student = studentDao.getStudentByUserLogin(login);
                if (student != null) {
                    res.status(200);
                    return JsonUtil.successResponse(student);
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("Student not found for user: " + login, 404);
                }
            } catch (SQLException e) {
                logger.error("Error fetching student by login: {}", login, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // GET /api/students/group/:groupNumber - получить студентов по группе
        Spark.get("/api/students/group/:groupNumber", (req, res) -> {
            String groupNumber = req.params(":groupNumber");
            logger.info("GET /api/students/group/{} - Fetching students by group", groupNumber);
            JsonUtil.setJsonResponse(res);
            try {
                List<Student> students = studentDao.getStudentsByGroup(groupNumber);
                res.status(200);
                return JsonUtil.successResponse(students);
            } catch (SQLException e) {
                logger.error("Error fetching students by group: {}", groupNumber, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // GET /api/students/duty/today - получить дежурных
        Spark.get("/api/students/duty/today", (req, res) -> {
            logger.info("GET /api/students/duty/today - Fetching duty students");
            JsonUtil.setJsonResponse(res);
            try {
                List<Student> dutyStudents = studentDao.getDutyStudents();
                res.status(200);
                return JsonUtil.successResponse(dutyStudents);
            } catch (SQLException e) {
                logger.error("Error fetching duty students", e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // POST /api/students - создать студента
        Spark.post("/api/students", (req, res) -> {
            logger.info("POST /api/students - Creating new student");
            JsonUtil.setJsonResponse(res);
            try {
                Student studentData = JsonUtil.fromJson(req.body(), Student.class);

                // Валидация
                if (studentData.getFio() == null || studentData.getGroupNumber() == null ||
                        studentData.getUserLogin() == null) {
                    res.status(400);
                    return JsonUtil.errorResponse("FIO, groupNumber and userLogin are required", 400);
                }

                Student created = studentDao.addStudent(studentData);
                if (created != null) {
                    res.status(201);
                    return JsonUtil.successResponse(created);
                } else {
                    res.status(400);
                    return JsonUtil.errorResponse("Failed to create student. Check if group and user exist", 400);
                }
            } catch (SQLException e) {
                logger.error("Error creating student", e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // PUT /api/students/:id/fio - обновить ФИО студента
        Spark.put("/api/students/:id/fio", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("PUT /api/students/{}/fio - Updating student FIO", id);
            JsonUtil.setJsonResponse(res);
            try {
                JsonObject request = JsonUtil.fromJson(req.body(), JsonObject.class);
                String newFio = request.get("fio").getAsString();

                if (newFio == null || newFio.isEmpty()) {
                    res.status(400);
                    return JsonUtil.errorResponse("FIO is required", 400);
                }

                boolean updated = studentDao.updateStudentFio(id, newFio);
                if (updated) {
                    res.status(200);
                    return JsonUtil.successResponse(Map.of("message", "Student FIO updated successfully"));
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("Student not found with id: " + id, 404);
                }
            } catch (SQLException e) {
                logger.error("Error updating student FIO: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // PUT /api/students/:id/group - изменить группу студента
        Spark.put("/api/students/:id/group", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("PUT /api/students/{}/group - Changing student group", id);
            JsonUtil.setJsonResponse(res);
            try {
                JsonObject request = JsonUtil.fromJson(req.body(), JsonObject.class);
                String newGroup = request.get("groupNumber").getAsString();

                if (newGroup == null || newGroup.isEmpty()) {
                    res.status(400);
                    return JsonUtil.errorResponse("Group number is required", 400);
                }

                boolean updated = studentDao.changeGroup(id, newGroup);
                if (updated) {
                    res.status(200);
                    return JsonUtil.successResponse(Map.of("message", "Student group changed successfully"));
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("Student not found or group doesn't exist", 404);
                }
            } catch (SQLException e) {
                logger.error("Error changing student group: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // PUT /api/students/:id/duty/count - обновить количество дежурств
        Spark.put("/api/students/:id/duty/count", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("PUT /api/students/{}/duty/count - Updating duty count", id);
            JsonUtil.setJsonResponse(res);
            try {
                JsonObject request = JsonUtil.fromJson(req.body(), JsonObject.class);
                int count = request.get("count").getAsInt();

                if (count < 0) {
                    res.status(400);
                    return JsonUtil.errorResponse("Count must be non-negative", 400);
                }

                boolean updated = studentDao.updateDuty(id, count);
                if (updated) {
                    res.status(200);
                    return JsonUtil.successResponse(Map.of("message", "Duty count updated successfully"));
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("Student not found with id: " + id, 404);
                }
            } catch (SQLException e) {
                logger.error("Error updating duty count: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // PUT /api/students/:id/duty/status - изменить статус дежурства
        Spark.put("/api/students/:id/duty/status", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("PUT /api/students/{}/duty/status - Changing duty status", id);
            JsonUtil.setJsonResponse(res);
            try {
                JsonObject request = JsonUtil.fromJson(req.body(), JsonObject.class);
                boolean isDuty = request.get("isDuty").getAsBoolean();

                boolean updated = studentDao.changeDutyStatus(id, isDuty);
                if (updated) {
                    res.status(200);
                    return JsonUtil.successResponse(Map.of("message", "Duty status changed successfully"));
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("Student not found with id: " + id, 404);
                }
            } catch (SQLException e) {
                logger.error("Error changing duty status: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // DELETE /api/students/:id - удалить студента
        Spark.delete("/api/students/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("DELETE /api/students/{} - Deleting student", id);
            JsonUtil.setJsonResponse(res);
            try {
                boolean deleted = studentDao.deleteStudent(id);
                if (deleted) {
                    res.status(200);
                    return JsonUtil.successResponse(Map.of("message", "Student deleted successfully"));
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("Student not found with id: " + id, 404);
                }
            } catch (SQLException e) {
                logger.error("Error deleting student: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });
    }

    private void registerGroupEndpoints() {

        // GET /api/groups - получить все группы
        Spark.get("/api/groups", (req, res) -> {
            logger.info("GET /api/groups - Fetching all groups");
            JsonUtil.setJsonResponse(res);
            try {
                List<Group> groups = groupDao.getAllGroups();
                res.status(200);
                return JsonUtil.successResponse(groups);
            } catch (SQLException e) {
                logger.error("Error fetching groups", e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // GET /api/groups/:id - получить группу по ID
        Spark.get("/api/groups/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("GET /api/groups/{} - Fetching group by ID", id);
            JsonUtil.setJsonResponse(res);
            try {
                Group group = groupDao.getGroupById(id);
                if (group != null) {
                    res.status(200);
                    return JsonUtil.successResponse(group);
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("Group not found with id: " + id, 404);
                }
            } catch (SQLException e) {
                logger.error("Error fetching group by id: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // GET /api/groups/number/:number - получить группу по номеру
        Spark.get("/api/groups/number/:number", (req, res) -> {
            String number = req.params(":number");
            logger.info("GET /api/groups/number/{} - Fetching group by number", number);
            JsonUtil.setJsonResponse(res);
            try {
                Group group = groupDao.getGroupByNumber(number);
                if (group != null) {
                    res.status(200);
                    return JsonUtil.successResponse(group);
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("Group not found with number: " + number, 404);
                }
            } catch (SQLException e) {
                logger.error("Error fetching group by number: {}", number, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // GET /api/groups/with-students - получить группы, в которых есть студенты
        Spark.get("/api/groups/with-students", (req, res) -> {
            logger.info("GET /api/groups/with-students - Fetching groups with students");
            JsonUtil.setJsonResponse(res);
            try {
                List<Group> groups = groupDao.getGroupsWithStudents();
                res.status(200);
                return JsonUtil.successResponse(groups);
            } catch (SQLException e) {
                logger.error("Error fetching groups with students", e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // GET /api/groups/empty - получить пустые группы
        Spark.get("/api/groups/empty", (req, res) -> {
            logger.info("GET /api/groups/empty - Fetching empty groups");
            JsonUtil.setJsonResponse(res);
            try {
                List<Group> groups = groupDao.getEmptyGroups();
                res.status(200);
                return JsonUtil.successResponse(groups);
            } catch (SQLException e) {
                logger.error("Error fetching empty groups", e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // GET /api/groups/:number/students/count - получить количество студентов в группе
        Spark.get("/api/groups/:number/students/count", (req, res) -> {
            String number = req.params(":number");
            logger.info("GET /api/groups/{}/students/count - Getting student count", number);
            JsonUtil.setJsonResponse(res);
            try {
                int count = groupDao.getStudentCountInGroup(number);
                res.status(200);
                return JsonUtil.successResponse(Map.of("groupNumber", number, "studentCount", count));
            } catch (SQLException e) {
                logger.error("Error getting student count for group: {}", number, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // POST /api/groups - создать группу
        Spark.post("/api/groups", (req, res) -> {
            logger.info("POST /api/groups - Creating new group");
            JsonUtil.setJsonResponse(res);
            try {
                Group groupData = JsonUtil.fromJson(req.body(), Group.class);

                if (groupData.getNumber() == null || groupData.getNumber().isEmpty()) {
                    res.status(400);
                    return JsonUtil.errorResponse("Group number is required", 400);
                }

                Group created = groupDao.addGroup(groupData);
                if (created != null) {
                    res.status(201);
                    return JsonUtil.successResponse(created);
                } else {
                    res.status(400);
                    return JsonUtil.errorResponse("Failed to create group", 400);
                }
            } catch (SQLException e) {
                logger.error("Error creating group", e);
                if (e.getMessage().contains("duplicate")) {
                    res.status(409);
                    return JsonUtil.errorResponse("Group with this number already exists", 409);
                }
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // PUT /api/groups/:id/name - изменить название группы
        Spark.put("/api/groups/:id/name", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("PUT /api/groups/{}/name - Changing group name", id);
            JsonUtil.setJsonResponse(res);
            try {
                JsonObject request = JsonUtil.fromJson(req.body(), JsonObject.class);
                String newName = request.get("number").getAsString();

                if (newName == null || newName.isEmpty()) {
                    res.status(400);
                    return JsonUtil.errorResponse("Group number is required", 400);
                }

                boolean updated = groupDao.changeGroupName(id, newName);
                if (updated) {
                    res.status(200);
                    return JsonUtil.successResponse(Map.of("message", "Group name changed successfully"));
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("Group not found or new number already exists", 404);
                }
            } catch (SQLException e) {
                logger.error("Error changing group name: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // DELETE /api/groups/:id - удалить группу
        Spark.delete("/api/groups/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            logger.info("DELETE /api/groups/{} - Deleting group", id);
            JsonUtil.setJsonResponse(res);
            try {
                boolean deleted = groupDao.deleteGroup(id);
                if (deleted) {
                    res.status(200);
                    return JsonUtil.successResponse(Map.of("message", "Group deleted successfully"));
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("Group not found with id: " + id, 404);
                }
            } catch (SQLException e) {
                logger.error("Error deleting group: {}", id, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });

        // DELETE /api/groups/number/:number - удалить группу по номеру
        Spark.delete("/api/groups/number/:number", (req, res) -> {
            String number = req.params(":number");
            logger.info("DELETE /api/groups/number/{} - Deleting group by number", number);
            JsonUtil.setJsonResponse(res);
            try {
                boolean deleted = groupDao.deleteGroupByNumber(number);
                if (deleted) {
                    res.status(200);
                    return JsonUtil.successResponse(Map.of("message", "Group deleted successfully"));
                } else {
                    res.status(404);
                    return JsonUtil.errorResponse("Group not found with number: " + number, 404);
                }
            } catch (SQLException e) {
                logger.error("Error deleting group by number: {}", number, e);
                res.status(500);
                return JsonUtil.errorResponse("Database error: " + e.getMessage(), 500);
            }
        });
    }
}