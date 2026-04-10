package ru.filden.api.models;

public class Student {
    private int id;
    private String fio;
    private String groupNumber;
    private boolean isDuty;
    private int countDuty;
    private String userLogin; // ссылка на user.login

    public Student() {}

    public Student(int id, String fio, String groupNumber, boolean isDuty,
                   int countDuty, String userLogin) {
        this.id = id;
        this.fio = fio;
        this.groupNumber = groupNumber;
        this.isDuty = isDuty;
        this.countDuty = countDuty;
        this.userLogin = userLogin;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFio() { return fio; }
    public void setFio(String fio) { this.fio = fio; }
    public String getGroupNumber() { return groupNumber; }
    public void setGroupNumber(String groupNumber) { this.groupNumber = groupNumber; }
    public boolean isDuty() { return isDuty; }
    public void setDuty(boolean duty) { isDuty = duty; }
    public int getCountDuty() { return countDuty; }
    public void setCountDuty(int countDuty) { this.countDuty = countDuty; }
    public String getUserLogin() { return userLogin; }
    public void setUserLogin(String userLogin) { this.userLogin = userLogin; }
}
