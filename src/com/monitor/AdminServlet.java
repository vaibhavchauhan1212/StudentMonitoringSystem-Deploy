package com.monitor;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/admin/users")
public class AdminServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        String fetchMode = request.getParameter("fetchMode");

        try (Connection conn = DBConnection.getConnection()) {
            if ("FACULTY_ONLY".equals(fetchMode)) {
                String query = "SELECT id, name FROM faculty ORDER BY name ASC";
                PreparedStatement pstmt = conn.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery();
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append(String.format("{\"id\":\"%s\",\"name\":\"%s\"}", rs.getString("id"), rs.getString("name")));
                    first = false;
                }
                sb.append("]");
                out.print(sb.toString());
            } 
            else if ("ALL_SUBJECTS".equals(fetchMode)) {
                String query = "SELECT code, name, faculty_id FROM subjects ORDER BY code ASC";
                PreparedStatement pstmt = conn.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery();
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    String facId = rs.getString("faculty_id");
                    String finalFac = (facId == null) ? "" : facId;
                    sb.append(String.format("{\"code\":\"%s\",\"name\":\"%s\",\"facultyId\":\"%s\"}", rs.getString("code"), rs.getString("name"), finalFac));
                    first = false;
                }
                sb.append("]");
                out.print(sb.toString());
            } 
            else {
                String query = "SELECT id, role, created_at FROM users ORDER BY created_at DESC";
                PreparedStatement pstmt = conn.prepareStatement(query);
                ResultSet rs = pstmt.executeQuery();
                boolean first = true;
                StringBuilder sb = new StringBuilder("[");
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append(String.format("{\"id\":\"%s\",\"role\":\"%s\",\"createdAt\":\"%s\"}", rs.getString("id"), rs.getString("role"), rs.getTimestamp("created_at").toString()));
                    first = false;
                }
                sb.append("]");
                out.print(sb.toString());
            }
            out.flush();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        
        try (Connection conn = DBConnection.getConnection()) {
            if ("PROVISION".equals(action)) {
                String id = request.getParameter("id");
                String password = request.getParameter("password");
                String role = request.getParameter("role");
                String name = request.getParameter("name");
                String email = request.getParameter("email");

                String userQ = "INSERT INTO users (id, password, role) VALUES (?, ?, ?)";
                PreparedStatement ps1 = conn.prepareStatement(userQ);
                ps1.setString(1, id); ps1.setString(2, password); ps1.setString(3, role);
                ps1.executeUpdate();

                if ("STUDENT".equals(role)) {
                    String sQ = "INSERT INTO students (id, name, email, phone) VALUES (?, ?, ?, '0000000000')";
                    PreparedStatement ps2 = conn.prepareStatement(sQ);
                    ps2.setString(1, id); ps2.setString(2, name); ps2.setString(3, email);
                    ps2.executeUpdate();
                } else if ("FACULTY".equals(role)) {
                    String fQ = "INSERT INTO faculty (id, name, email, department) VALUES (?, ?, ?, 'General Academics')";
                    PreparedStatement ps2 = conn.prepareStatement(fQ);
                    ps2.setString(1, id); ps2.setString(2, name); ps2.setString(3, email);
                    ps2.executeUpdate();
                } else if ("PARENT".equals(role)) {
                    String pQ = "INSERT INTO parents (id, name, email, phone, student_id) VALUES (?, ?, ?, '0000000000', NULL)";
                    PreparedStatement ps2 = conn.prepareStatement(pQ);
                    ps2.setString(1, id); ps2.setString(2, name); ps2.setString(3, email);
                    ps2.executeUpdate();
                }
                response.setStatus(HttpServletResponse.SC_OK);
            } 
            // NEW MODULE ENGINE ACTION: PERFORMS MASTER SECURITY ID RESET MODIFICATIONS OR PASSWORD REWRITES ON MATCHING KEYS
            else if ("MASTER_SECURITY_OVERRIDE".equals(action)) {
                String targetId = request.getParameter("targetId");
                String type = request.getParameter("type");
                String newValue = request.getParameter("newValue");

                if ("UPDATE_PASSWORD".equals(type)) {
                    String updatePassQ = "UPDATE users SET password = ? WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(updatePassQ);
                    ps.setString(1, newValue);
                    ps.setString(2, targetId);
                    ps.executeUpdate();
                } else if ("REASSIGN_ID".equals(type)) {
                    String updateIdQ = "UPDATE users SET id = ? WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(updateIdQ);
                    ps.setString(1, newValue);
                    ps.setString(2, targetId);
                    ps.executeUpdate();
                }
                response.setStatus(HttpServletResponse.SC_OK);
            }
            else if ("MAP_PARENT_STUDENT".equals(action)) {
                String parentId = request.getParameter("parentId");
                String studentId = request.getParameter("studentId");

                String updateParentQ = "UPDATE parents SET student_id = ? WHERE id = ?";
                PreparedStatement psP = conn.prepareStatement(updateParentQ);
                psP.setString(1, studentId); psP.setString(2, parentId);
                psP.executeUpdate();

                String updateStudentQ = "UPDATE students SET parent_id = ? WHERE id = ?";
                PreparedStatement psS = conn.prepareStatement(updateStudentQ);
                psS.setString(1, parentId); psS.setString(2, studentId);
                psS.executeUpdate();

                response.setStatus(HttpServletResponse.SC_OK);
            }
            else if ("CREATE_SUBJECT".equals(action)) {
                String code = request.getParameter("code");
                String name = request.getParameter("name");
                String facultyId = request.getParameter("facultyId");

                String query = "INSERT INTO subjects (code, name, faculty_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE name=?, faculty_id=?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, code); ps.setString(2, name);
                if ("NONE".equals(facultyId) || facultyId.trim().isEmpty()) { ps.setNull(3, java.sql.Types.VARCHAR); ps.setString(4, name); ps.setNull(5, java.sql.Types.VARCHAR); } 
                else { ps.setString(3, facultyId); ps.setString(4, name); ps.setString(5, facultyId); }
                ps.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            }
            else if ("ADD_TIMETABLE".equals(action)) {
                String type = request.getParameter("type");
                String subjectCode = request.getParameter("subjectCode");
                String day = request.getParameter("day");
                String slot = request.getParameter("slot");
                String room = request.getParameter("room");

                String query = "INSERT INTO timetables (type, subject_code, day_or_date, time_slot, room_no) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, type); ps.setString(2, subjectCode); ps.setString(3, day); ps.setString(4, slot); ps.setString(5, room);
                ps.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            } 
            else if ("ADD_INVOICE".equals(action)) {
                String studentId = request.getParameter("studentId");
                double amount = Double.parseDouble(request.getParameter("amount"));

                String query = "INSERT INTO fee_ledger (student_id, amount_due, billing_date) VALUES (?, ?, CURDATE())";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, studentId); ps.setDouble(2, amount);
                ps.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}