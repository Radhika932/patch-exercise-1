package com.internal.tasktracker;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/api/tasks")
    public ResponseEntity<?> searchTasks(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {

        // Validate paging values
        if (page < 1 || pageSize < 1) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "page and pageSize must be 1 or greater"));
        }
        if (pageSize > MAX_PAGE_SIZE) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "pageSize must be " + MAX_PAGE_SIZE + " or less"));
        }

        // Normalize query input and escape LIKE wildcards (\, %, _)
        String query = q == null ? "" : q.trim();
        String escaped = query.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        String searchTerm = "%" + escaped + "%";

        // Parse status filter (invalid value -> 400, not 500)
        String normalizedStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                normalizedStatus = TaskStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status: " + status));
            }
        }

        System.out.println("[TaskController] q=\"" + query + "\" status=" + normalizedStatus
                + " page=" + page + " pageSize=" + pageSize);

        List<Task> allResults = taskRepository.searchTasks(searchTerm, normalizedStatus);

        // Use long math so large page numbers cannot overflow
        long start = (long) (page - 1) * pageSize;
        List<Task> pageResults;
        if (start >= allResults.size()) {
            pageResults = Collections.emptyList();
        } else {
            int end = (int) Math.min(start + pageSize, allResults.size());
            pageResults = allResults.subList((int) start, end);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", pageResults);
        response.put("total", allResults.size());
        response.put("page", page);
        response.put("pageSize", pageSize);

        return ResponseEntity.ok(response);
    }
}