package com.project2.To_do_list.controller;


import com.project2.To_do_list.model.Task;
import com.project2.To_do_list.repo.TaskRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskRepo taskRepo;

    // Get all tasks
    @GetMapping
    public List<Task> getTasks() {
        return taskRepo.findAll();
    }

    // Add a new task
    @PostMapping
    public Task addTask(@RequestBody Task task) {
        task.setCompleted(false);
        task.setCompletedDate(null);
        return taskRepo.save(task);
    }

    // Update a task
    @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id, @RequestBody Task taskData) {
        Task task = taskRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setText(taskData.getText());
        task.setCompleted(taskData.isCompleted());

        if (taskData.isCompleted() && task.getCompletedDate() == null) {
            task.setCompletedDate(LocalDate.now());
        } else if (!taskData.isCompleted()) {
            task.setCompletedDate(null);
        }

        return taskRepo.save(task);
    }

    // Delete a task
    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id) {
        taskRepo.deleteById(id);
    }
}

