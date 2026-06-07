package com.monitor;

public class Student {
    private int id;
    private String name;
    private String email;
    private String phone;
    private int attendance;
    private int assignmentScore;
    private String grades;

    // Model Constructor matching database attributes
    public Student(int id, String name, String email, String phone, int attendance, int assignmentScore, String grades) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.attendance = attendance;
        this.assignmentScore = assignmentScore;
        this.grades = grades;
    }

    // Encapsulation Getters (Called directly by the Servlet serialization engine)
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public int getAttendance() { return attendance; }
    public int getAssignmentScore() { return assignmentScore; }
    public String getGrades() { return grades; }
}