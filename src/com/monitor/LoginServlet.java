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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String userId = request.getParameter("id");
        String userPassword = request.getParameter("password");

        try (Connection conn = DBConnection.getConnection()) {
            String authQuery = "SELECT id, role FROM users WHERE id = ? AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(authQuery);
            pstmt.setString(1, userId);
            pstmt.setString(2, userPassword);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                out.print(String.format("{\"id\":\"%s\",\"role\":\"%s\",\"authenticated\":true}", userId, rs.getString("role")));
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"error\":\"Access Mismatch. Invalid Credentials.\"}");
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}