package com.kushan.aut_svc.controller;

import com.kushan.aut_svc.Model.Lecturer;
import com.kushan.aut_svc.Model.User;
import com.kushan.aut_svc.repository.LecturerRepository;
import com.kushan.aut_svc.repository.UserRepository;
import com.kushan.aut_svc.security.jwt.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/req/admin")
public class AdminController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final LecturerRepository lecturerRepository;

    public AdminController(JwtService jwtService,
                           UserRepository userRepository,
                           LecturerRepository lecturerRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.lecturerRepository = lecturerRepository;
    }

    /* Every admin endpoint requires a valid ROLE_ADMIN token. */
    private void assertAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        String role = jwtService.getRole(authHeader.substring(7));
        if (!"ROLE_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private static String displayName(User u) {
        String first = u.getFirstName() != null ? u.getFirstName() : "";
        String last = u.getLastName() != null ? u.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }

    private static String userStatus(User u) {
        if (u instanceof Lecturer l) {
            String s = l.getStatus();
            return s == null ? "Active" : s;
        }
        return "Active";
    }

    // ---- Aggregate stats for the dashboard header ----
    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        assertAdmin(authHeader);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("users", userRepository.count());
        out.put("students", userRepository.countByRole("student"));
        out.put("lecturers", userRepository.countByRole("lecturer"));
        // Course data lives in the course microservice; not tracked here yet.
        out.put("courses", 0);
        out.put("pending", lecturerRepository.countByStatus("PENDING"));
        return out;
    }

    // ---- Recent users (newest first) ----
    @GetMapping("/users")
    public List<Map<String, Object>> users(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        assertAdmin(authHeader);
        List<Map<String, Object>> result = new ArrayList<>();
        List<User> all = userRepository.findAll();
        all.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        int limit = Math.min(all.size(), 12);
        for (int i = 0; i < limit; i++) {
            User u = all.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", u.getId());
            row.put("name", displayName(u));
            row.put("email", u.getEmail());
            row.put("role", u.getRole());
            row.put("status", userStatus(u));
            result.add(row);
        }
        return result;
    }

    // ---- Lecturers awaiting approval ----
    @GetMapping("/lecturers/pending")
    public List<Map<String, Object>> pendingLecturers(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        assertAdmin(authHeader);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Lecturer l : lecturerRepository.findByStatus("PENDING")) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", l.getId());
            row.put("name", displayName(l));
            row.put("email", l.getEmail());
            row.put("area", l.getArea() != null ? l.getArea() : "");
            row.put("title", l.getTitle() != null ? l.getTitle() : "");
            row.put("experience", l.getExperience() != null ? l.getExperience() : 0);
            row.put("bio", l.getBio() != null ? l.getBio() : "");
            result.add(row);
        }
        return result;
    }

    @PostMapping("/lecturers/{id}/approve")
    public Map<String, Object> approve(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @PathVariable Long id) {
        assertAdmin(authHeader);
        Lecturer l = lecturerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecturer not found"));
        l.setStatus("ACTIVE");
        lecturerRepository.save(l);
        return Map.of("id", id, "status", "ACTIVE");
    }

    @PostMapping("/lecturers/{id}/reject")
    public Map<String, Object> reject(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                      @PathVariable Long id) {
        assertAdmin(authHeader);
        Lecturer l = lecturerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecturer not found"));
        l.setStatus("REJECTED");
        lecturerRepository.save(l);
        return Map.of("id", id, "status", "REJECTED");
    }
}