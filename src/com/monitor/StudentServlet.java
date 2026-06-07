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

@WebServlet("/students")
public class StudentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        String fetchMode = request.getParameter("fetchMode");

        try (Connection conn = DBConnection.getConnection()) {
            if ("GET_PROFILE_CARD_METADATA".equals(fetchMode)) {
                String role = request.getParameter("role");
                String id = request.getParameter("id");
                String query = "";
                
                if ("STUDENT".equals(role)) query = "SELECT id, name FROM students WHERE id = ?";
                else if ("FACULTY".equals(role)) query = "SELECT id, name FROM faculty WHERE id = ?";
                else if ("PARENT".equals(role)) query = "SELECT id, name FROM parents WHERE id = ?";
                
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    out.print(String.format("{\"id\":\"%s\",\"name\":\"%s\"}", rs.getString("id"), rs.getString("name")));
                } else {
                    out.print(String.format("{\"id\":\"%s\",\"name\":\"System Profile User\"}", id));
                }
            }
            else if ("BULK_COHORT".equals(fetchMode)) {
                String query = "SELECT s.id, s.name, s.email, s.behavior_score, " +
                               "COALESCE(ROUND((SUM(CASE WHEN a.status='PRESENT' THEN 1 ELSE 0 END)/COUNT(a.id))*100), 100) as att_rate, " +
                               "COALESCE(g.grade, 'A') as letter_grade " +
                               "FROM students s " +
                               "LEFT JOIN attendance a ON s.id = a.student_id " +
                               "LEFT JOIN (SELECT student_id, CASE WHEN AVG(semester_exam)>=40 THEN 'A' WHEN AVG(semester_exam)>=30 THEN 'B' ELSE 'C' END as grade FROM grades_ledger GROUP BY student_id) g ON s.id = g.student_id " +
                               "GROUP BY s.id, s.name, s.email, s.behavior_score, g.grade";
                
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while(rs.next()) {
                    if(!first) sb.append(",");
                    sb.append(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"attendanceRate\":%d,\"gradeLetter\":\"%s\",\"behaviorScore\":%d}",
                            rs.getString("id"), rs.getString("name"), rs.getString("email"), rs.getInt("att_rate"), rs.getString("letter_grade"), rs.getInt("behavior_score")));
                    first = false;
                }
                sb.append("]");
                out.print(sb.toString());
            } 
            else if ("ENROLLED_STUDENTS_LIST".equals(fetchMode)) {
                String subjectCode = request.getParameter("subjectCode");
                String query = "SELECT s.id, s.name FROM students s INNER JOIN grades_ledger g ON s.id = g.student_id WHERE g.subject_code = ?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, subjectCode);
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while(rs.next()) {
                    if(!first) sb.append(",");
                    sb.append(String.format("{\"id\":\"%s\",\"name\":\"%s\"}", rs.getString("id"), rs.getString("name")));
                    first = false;
                }
                sb.append("]");
                out.print(sb.toString());
            }
            else if ("FACULTY_ENROLLMENT_LOGS".equals(fetchMode)) {
                String query = "SELECT student_id, subject_code, internal_assessment FROM grades_ledger ORDER BY subject_code ASC";
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while(rs.next()) {
                    if(!first) sb.append(",");
                    sb.append(String.format("{\"studentId\":\"%s\",\"subjectCode\":\"%s\",\"internal\":%d}",
                            rs.getString("student_id"), rs.getString("subject_code"), rs.getInt("internal_assessment")));
                    first = false;
                }
                sb.append("]");
                out.print(sb.toString());
            }
            else if ("FACULTY_LEAVE_VIEW_ALL".equals(fetchMode)) {
                String query = "SELECT id, student_id, start_date, end_date, reason, status FROM leave_requests ORDER BY id DESC";
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                while(rs.next()) {
                    if(!first) sb.append(",");
                    sb.append(String.format("{\"id\":%d,\"studentId\":\"%s\",\"start\":\"%s\",\"end\":\"%s\",\"reason\":\"%s\",\"status\":\"%s\"}",
                            rs.getInt("id"), rs.getString("student_id"), rs.getDate("start_date").toString(), rs.getDate("end_date").toString(), rs.getString("reason").replace("\"", "\\\""), rs.getString("status")));
                    first = false;
                }
                sb.append("]");
                out.print(sb.toString());
            }
            else if ("SINGLE_PROFILE".equals(fetchMode)) {
                String studentId = request.getParameter("id");
                
                int attRate = 100;
                String attQ = "SELECT COALESCE(ROUND((SUM(CASE WHEN status='PRESENT' THEN 1 ELSE 0 END)/COUNT(id))*100), 100) FROM attendance WHERE student_id=?";
                PreparedStatement psAtt = conn.prepareStatement(attQ); psAtt.setString(1, studentId);
                ResultSet rsAtt = psAtt.executeQuery();
                if(rsAtt.next() && rsAtt.getInt(1) > 0) attRate = rsAtt.getInt(1);

                int behavior = 100;
                String bhQ = "SELECT behavior_score FROM students WHERE id=?";
                PreparedStatement psBh = conn.prepareStatement(bhQ); psBh.setString(1, studentId);
                ResultSet rsBh = psBh.executeQuery();
                if(rsBh.next()) behavior = rsBh.getInt(1);

                double pendingFee = 0.0;
                String feeQ = "SELECT COALESCE(SUM(amount_due - amount_paid), 0.0) FROM fee_ledger WHERE student_id=?";
                PreparedStatement psFee = conn.prepareStatement(feeQ); psFee.setString(1, studentId);
                ResultSet rsFee = psFee.executeQuery();
                if(rsFee.next()) pendingFee = rsFee.getDouble(1);

                StringBuilder ttBuilder = new StringBuilder("[");
                String ttQ = "SELECT day_or_date, subject_code, time_slot, room_no FROM timetables WHERE type='CLASS' ORDER BY FIELD(day_or_date, 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday')";
                PreparedStatement psTt = conn.prepareStatement(ttQ);
                ResultSet rsTt = psTt.executeQuery();
                boolean fTimetable = true;
                while(rsTt.next()) {
                    if(!fTimetable) ttBuilder.append(",");
                    ttBuilder.append(String.format("{\"day\":\"%s\",\"subject\":\"%s\",\"time\":\"%s\",\"room\":\"%s\"}",
                            rsTt.getString("day_or_date"), rsTt.getString("subject_code"), rsTt.getString("time_slot"), rsTt.getString("room_no")));
                    fTimetable = false;
                }
                ttBuilder.append("]");

                StringBuilder leaveBuilder = new StringBuilder("[");
                String altQ = "SELECT start_date, end_date, reason, status FROM leave_requests WHERE student_id=?";
                PreparedStatement psLv = conn.prepareStatement(altQ); psLv.setString(1, studentId);
                ResultSet rsLv = psLv.executeQuery();
                boolean fLeave = true;
                while(rsLv.next()) {
                    if(!fLeave) leaveBuilder.append(",");
                    leaveBuilder.append(String.format("{\"start\":\"%s\",\"end\":\"%s\",\"reason\":\"%s\",\"status\":\"%s\"}",
                            rsLv.getDate("start_date").toString(), rsLv.getDate("end_date").toString(), rsLv.getString("reason"), rsLv.getString("status")));
                    fLeave = false;
                }
                leaveBuilder.append("]");

                StringBuilder granularBuilder = new StringBuilder("[");
                String gQ = "SELECT g.subject_code, sub.name as sub_name, g.internal_assessment, g.assignment_score, g.semester_exam, g.final_gpa, " +
                            "COALESCE((SELECT ROUND((SUM(CASE WHEN att.status='PRESENT' THEN 1 ELSE 0 END)/COUNT(att.id))*100) FROM attendance att WHERE att.student_id=g.student_id AND att.subject_code=g.subject_code), 100) as subject_att " +
                            "FROM grades_ledger g INNER JOIN subjects sub ON g.subject_code = sub.code WHERE g.student_id=?";
                PreparedStatement psGranular = conn.prepareStatement(gQ); psGranular.setString(1, studentId);
                ResultSet rsGranular = psGranular.executeQuery();
                
                double cumulativeGradeSum = 0.00;
                int subjectCount = 0;
                boolean fGranular = true;

                while(rsGranular.next()) {
                    if(!fGranular) granularBuilder.append(",");
                    double subGPA = rsGranular.getDouble("final_gpa");
                    cumulativeGradeSum += subGPA;
                    subjectCount++;

                    granularBuilder.append(String.format("{\"subjectCode\":\"%s\",\"subjectName\":\"%s\",\"subjectAttendance\":%d,\"internalAssessment\":%d,\"assignmentScore\":%d,\"semesterExam\":%d,\"subjectGPA\":%.2f}",
                            rsGranular.getString("subject_code"), rsGranular.getString("sub_name"), rsGranular.getInt("subject_att"), rsGranular.getInt("internal_assessment"), rsGranular.getInt("assignment_score"), rsGranular.getInt("semester_exam"), subGPA));
                    fGranular = false;
                }
                granularBuilder.append("]");

                double trueCGPA = (subjectCount == 0) ? 0.00 : (cumulativeGradeSum / subjectCount) * 2.5; 

                out.print(String.format("{\"attendanceRate\":%d,\"gpa\":%.2f,\"behaviorScore\":%d,\"pendingInvoiceFee\":%.2f,\"timetableRoster\":%s,\"leaveRoster\":%s,\"granularSubjectBreakdown\":%s}",
                        attRate, trueCGPA, behavior, pendingFee, ttBuilder.toString(), leaveBuilder.toString(), granularBuilder.toString()));
            } 
            else if ("PARENT_LOOKUP".equals(fetchMode)) {
                String parentId = request.getParameter("parentId");
                String query = "SELECT s.name, COALESCE(g.grade, 'A') as grade FROM students s " +
                               "LEFT JOIN parents p ON s.parent_id = p.id " +
                               "LEFT JOIN (SELECT student_id, CASE WHEN AVG(semester_exam)>=40 THEN 'A' ELSE 'B' END as grade FROM grades_ledger GROUP BY student_id) g ON s.id = g.student_id " +
                               "WHERE p.id = ?";
                PreparedStatement ps = conn.prepareStatement(query); ps.setString(1, parentId);
                ResultSet rs = ps.executeQuery();
                if(rs.next()) {
                    out.print(String.format("{\"name\":\"%s\",\"attendanceRate\":95,\"gradeLetter\":\"%s\"}", rs.getString("name"), rs.getString("grade")));
                } else {
                    out.print("{\"name\":\"No Ward Assigned\",\"attendanceRate\":0,\"gradeLetter\":\"N/A\"}");
                }
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        try (Connection conn = DBConnection.getConnection()) {
            if ("PROCESS_FEE_PAYMENT".equals(action)) {
                String studentId = request.getParameter("studentId");
                double payAmount = Double.parseDouble(request.getParameter("amount"));

                String payQuery = "UPDATE fee_ledger SET amount_paid = amount_paid + ? WHERE student_id = ? AND status = 'PENDING' LIMIT 1";
                PreparedStatement ps = conn.prepareStatement(payQuery);
                ps.setDouble(1, payAmount); ps.setString(2, studentId);
                ps.executeUpdate();
                
                String sweepQuery = "UPDATE fee_ledger SET status = 'PAID' WHERE student_id = ? AND amount_paid >= amount_due";
                PreparedStatement psSweep = conn.prepareStatement(sweepQuery); psSweep.setString(1, studentId);
                psSweep.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            }
            else if ("PROCESS_LEAVE_DECISION".equals(action)) {
                int leaveId = Integer.parseInt(request.getParameter("leaveId"));
                String decision = request.getParameter("decision");

                String leaveUpdateQ = "UPDATE leave_requests SET status = ? WHERE id = ?";
                PreparedStatement psL = conn.prepareStatement(leaveUpdateQ);
                psL.setString(1, decision); psL.setInt(2, leaveId);
                psL.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            }
            else if ("MANAGE_BEHAVIOR".equals(action)) {
                String studentId = request.getParameter("studentId");
                String type = request.getParameter("type");
                int points = Integer.parseInt(request.getParameter("points"));

                String opModifier = "ADD".equals(type) ? "behavior_score + ?" : "GREATEST(behavior_score - ?, 0)";
                String behaviorQ = "UPDATE students SET behavior_score = " + opModifier + " WHERE id = ?";
                PreparedStatement psB = conn.prepareStatement(behaviorQ);
                psB.setInt(1, points); psB.setString(2, studentId);
                psB.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            }
            else if ("ENROLL_STUDENT".equals(action)) {
                String studentId = request.getParameter("studentId");
                String subjectCode = request.getParameter("subjectCode");
                String enrollQuery = "INSERT INTO grades_ledger (student_id, subject_code, internal_assessment, assignment_score, semester_exam, final_gpa) VALUES (?, ?, 0, 0, 0, 0.00) ON DUPLICATE KEY UPDATE student_id=student_id";
                PreparedStatement ps = conn.prepareStatement(enrollQuery);
                ps.setString(1, studentId); ps.setString(2, subjectCode);
                ps.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            }
            else if ("MARK_ATTENDANCE".equals(action)) {
                String studentId = request.getParameter("studentId");
                String subjectCode = request.getParameter("subjectCode");
                String status = request.getParameter("status");

                String q = "INSERT INTO attendance (student_id, subject_code, date, status) VALUES (?, ?, CURDATE(), ?)";
                PreparedStatement ps = conn.prepareStatement(q);
                ps.setString(1, studentId); ps.setString(2, subjectCode); ps.setString(3, status);
                ps.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            } 
            else if ("UPDATE_PERFORMANCE".equals(action)) {
                String studentId = request.getParameter("studentId");
                String subjectCode = request.getParameter("subjectCode");
                int internal = Integer.parseInt(request.getParameter("internal"));
                int assignment = Integer.parseInt(request.getParameter("assignment"));
                int semester = Integer.parseInt(request.getParameter("semester"));
                
                double totalEarnedSum = internal + assignment + semester; 
                double calculatedGPA = 0.00;
                if(totalEarnedSum >= 90) calculatedGPA = 4.00;
                else if(totalEarnedSum >= 80) calculatedGPA = 3.50;
                else if(totalEarnedSum >= 70) calculatedGPA = 3.00;
                else if(totalEarnedSum >= 60) calculatedGPA = 2.50;
                else if(totalEarnedSum >= 50) calculatedGPA = 2.00;
                else calculatedGPA = 1.00;

                String q = "INSERT INTO grades_ledger (student_id, subject_code, internal_assessment, assignment_score, semester_exam, final_gpa) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE internal_assessment=?, assignment_score=?, semester_exam=?, final_gpa=?";
                PreparedStatement ps = conn.prepareStatement(q);
                ps.setString(1, studentId); ps.setString(2, subjectCode); ps.setInt(3, internal); ps.setInt(4, assignment); ps.setInt(5, semester); ps.setDouble(6, calculatedGPA);
                ps.setInt(7, internal); ps.setInt(8, assignment); ps.setInt(9, semester); ps.setDouble(10, calculatedGPA);
                ps.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            } 
            else if ("LOG_DISCIPLINE".equals(action)) {
                String studentId = request.getParameter("studentId");
                int points = Integer.parseInt(request.getParameter("points"));
                String desc = request.getParameter("desc");

                String q1 = "INSERT INTO disciplinary_incidents (student_id, description, penalty_points, date) VALUES (?, ?, ?, CURDATE())";
                PreparedStatement ps1 = conn.prepareStatement(q1);
                ps1.setString(1, studentId); ps1.setString(2, desc); ps1.setInt(3, points);
                ps1.executeUpdate();

                String q2 = "UPDATE students SET behavior_score = GREATEST(behavior_score - ?, 0) WHERE id = ?";
                PreparedStatement ps2 = conn.prepareStatement(q2);
                ps2.setInt(1, points); ps2.setString(2, studentId);
                ps2.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            } 
            else if ("ADD_ASSIGNMENT".equals(action)) {
                String title = request.getParameter("title");
                String subjectCode = request.getParameter("subjectCode");
                String due = request.getParameter("due");
                String attachment = request.getParameter("attachment");

                String q = "INSERT INTO assignments (title, subject_code, due_date, file_path) VALUES (?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(q);
                ps.setString(1, title); ps.setString(2, subjectCode); ps.setString(3, due + " 23:59:59");
                if (attachment == null || attachment.trim().isEmpty()) { ps.setNull(4, java.sql.Types.VARCHAR); } else { ps.setString(4, attachment); }
                ps.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            } 
            else if ("SUBMIT_ASSIGNMENT".equals(action)) {
                int assignmentId = Integer.parseInt(request.getParameter("assignmentId"));
                String studentId = request.getParameter("studentId");

                String q = "INSERT INTO assignment_submissions (assignment_id, student_id, status) VALUES (?, ?, 'ON_TIME')";
                PreparedStatement ps = conn.prepareStatement(q);
                ps.setInt(1, assignmentId); ps.setString(2, studentId);
                ps.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            } 
            else if ("FILE_LEAVE".equals(action)) {
                String studentId = request.getParameter("studentId");
                String start = request.getParameter("start");
                String end = request.getParameter("end");
                String reason = request.getParameter("reason");

                String q = "INSERT INTO leave_requests (student_id, reason, start_date, end_date, status) VALUES (?, ?, ?, ?, 'PENDING')";
                PreparedStatement ps = conn.prepareStatement(q);
                ps.setString(1, studentId); ps.setString(2, reason); ps.setString(3, start); ps.setString(4, end);
                ps.executeUpdate();
                response.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}