package com.project2.To_do_list.controller;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.project2.To_do_list.model.*;
import com.project2.To_do_list.repo.*;
import com.project2.To_do_list.service.ConceptService;
import com.project2.To_do_list.vo.LoginRequest;

@Controller
public class UserController {

    @Autowired private UserRepo userRepo;
    @Autowired private HabitRepo habitRepo;
    @Autowired private ConceptService conceptService;
    @Autowired private NoteRepo noteRepo;
    @Autowired private PdfFileRepo pdfRepo;

    @Value("${pdf.upload-dir:C:/Users/chvys/Documents/java/springboot/To_do_list/uploads}")
    private String uploadDir;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------------- LOGIN & SIGNUP ----------------
    @GetMapping({"/", "/auth"})
    public String showLoginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "loginpage";
    }
/*
    @PostMapping("/auth")
    public String loginUser(@ModelAttribute("loginRequest") LoginRequest loginRequest,
                            Model model,
                            HttpSession session) {
        User user = userRepo.findByEmail(loginRequest.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            model.addAttribute("error", "Invalid email or password");
            return "loginpage";
        }

        session.setAttribute("currentUser", user);
        return "redirect:/home";
    }*/

    @GetMapping("/signup")
    public String getSignupPage(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(@ModelAttribute User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.save(user);
        return "redirect:/auth?registered=true";
    }

    @GetMapping("/home")
    public String getHomepage(HttpSession session, Model model,
                              Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User currentUser = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            session.setAttribute("currentUser", currentUser);
            model.addAttribute("currentUser", currentUser);
            return "home";
        }
        return "redirect:/auth";
    }

    // ---------------- HABIT TRACKER ----------------
    @GetMapping("/habits")
    public String showHabits(Model model, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "redirect:/auth";

        List<Habit> habits = habitRepo.findByUser(currentUser);
        model.addAttribute("habits", habits);
        model.addAttribute("newHabit", new Habit());
        model.addAttribute("isCalendarView", false);
        return "habits";
    }

    @PostMapping("/habits")
    public String addHabit(@ModelAttribute("newHabit") Habit newHabit, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "redirect:/auth";

        newHabit.setUser(currentUser);
        newHabit.setCompletedDays(0);
        habitRepo.save(newHabit);
        return "redirect:/habits";
    }

    @GetMapping("/calendar")
    public String showCalendar(Model model, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "redirect:/auth";

        List<Habit> habits = habitRepo.findByUser(currentUser);
        model.addAttribute("habits", habits);
        model.addAttribute("newHabit", new Habit());
        model.addAttribute("isCalendarView", true);
        return "habits";
    }

    @PostMapping("/habits/increment/{id}")
    public String incrementHabit(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "redirect:/auth";

        Habit habit = habitRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Habit not found"));

        if (habit.getUser().getId().equals(currentUser.getId())) {
            habit.setCompletedDays(habit.getCompletedDays() + 1);
            habitRepo.save(habit);
        }
        return "redirect:/habits";
    }

    @PostMapping("/habits/reset/{id}")
    public String resetHabit(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "redirect:/auth";

        Habit habit = habitRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Habit not found"));

        if (habit.getUser().getId().equals(currentUser.getId())) {
            habit.setCompletedDays(0);
            habitRepo.save(habit);
        }
        return "redirect:/habits";
    }

    @PostMapping("/habits/delete/{id}")
    public String deleteHabit(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "redirect:/auth";

        Habit habit = habitRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Habit not found"));

        if (habit.getUser().getId().equals(currentUser.getId())) {
            habitRepo.delete(habit);
        }
        return "redirect:/habits";
    }

    // ---------------- 147 RULE TRACKER ----------------
    @GetMapping("/147ruletracker")
    public String showRuleTracker(Model model, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "redirect:/auth";

        List<Concept> concepts = conceptService.getUserConcepts(currentUser);

        for (Concept concept : concepts) {
            int completed = (int) concept.getRevisions().stream().filter(Revision::isCompleted).count();
            int total = concept.getRevisions().size();
            concept.setProgress(total > 0 ? completed * 100 / total : 0);
        }

        ConceptService.ConceptStats stats = conceptService.getStats(currentUser);

        model.addAttribute("concepts", concepts);
        model.addAttribute("stats", stats);
        model.addAttribute("totalConcepts", stats.getTotalConcepts());
        model.addAttribute("completedRevisions", stats.getCompletedRevisions());
        model.addAttribute("pendingRevisions", stats.getPendingRevisions());

        return "147ruletracker";
    }

    @PostMapping("/147ruletracker/add")
    public String addConcept(@RequestParam String name, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "redirect:/auth";

        conceptService.addConcept(name, currentUser);
        return "redirect:/147ruletracker";
    }

    @PostMapping("/147ruletracker/toggle/{conceptId}/{revisionIndex}")
    public String toggleRevision(@PathVariable Long conceptId,
                                 @PathVariable int revisionIndex,
                                 HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "redirect:/auth";

        conceptService.toggleRevision(conceptId, revisionIndex, currentUser);
        return "redirect:/147ruletracker";
    }

    @PostMapping("/147ruletracker/delete/{conceptId}")
    public String deleteConcept(@PathVariable Long conceptId, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "redirect:/auth";

        conceptService.deleteConcept(conceptId, currentUser);
        return "redirect:/147ruletracker";
    }

    // ---------------- NOTES ----------------
    @GetMapping("/notes")
    public String showNotes(HttpSession session, Model model) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "redirect:/auth";

        List<Note> notes = noteRepo.findByUser(currentUser);
        model.addAttribute("notes", notes);

        List<String> folders = notes.stream().map(Note::getFolder).distinct().toList();
        model.addAttribute("folders", folders);

        return "notes";
    }

    @PostMapping("/notes")
    @ResponseBody
    public Note saveNote(@RequestBody Note note, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return null;

        note.setUser(currentUser);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepo.save(note);
    }

    @PutMapping("/notes/{id}")
    @ResponseBody
    public Note updateNote(@PathVariable Long id, @RequestBody Note noteData, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return null;

        Note note = noteRepo.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
        if (!note.getUser().getId().equals(currentUser.getId())) return null;

        note.setTitle(noteData.getTitle());
        note.setBody(noteData.getBody());
        note.setFolder(noteData.getFolder());
        note.setUpdatedAt(LocalDateTime.now());

        return noteRepo.save(note);
    }

    @DeleteMapping("/notes/{id}")
    @ResponseBody
    public String deleteNote(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "not-logged-in";

        Note note = noteRepo.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
        if (!note.getUser().getId().equals(currentUser.getId())) return "not-authorized";

        noteRepo.delete(note);
        return "success";
    }

    @GetMapping("/notes/view/{id}")
    public ResponseEntity<String> viewNote(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return ResponseEntity.status(401).body("Not logged in");

        Note note = noteRepo.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
        if (!note.getUser().getId().equals(currentUser.getId()))
            return ResponseEntity.status(403).body("Not authorized");

        String htmlContent = "<html><head><title>" + note.getTitle() + "</title></head>"
                + "<body style='font-family:sans-serif; padding:20px;'>"
                + "<h2>" + note.getTitle() + "</h2>"
                + "<div>" + note.getBody().replaceAll("\n", "<br>") + "</div>"
                + "</body></html>";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/html")
                .body(htmlContent);
    }

    // ---------------- PDF ----------------
    @GetMapping("/pdf")
    public String pdfLibrary(Model model, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/auth"; // redirect to login
        }

        List<PdfFile> pdfs = pdfRepo.findByUser(currentUser);
        model.addAttribute("pdfs", pdfs);

        return "pdf"; // must match src/main/resources/templates/pdf.html
    }

    @PostMapping("/pdf/upload")
    public String uploadPdf(@RequestParam("file") MultipartFile file, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/auth"; // Not logged in
        }

        if (file.isEmpty()) {
            return "redirect:/pdf?error=empty";
        }

        try {
            PdfFile pdf = new PdfFile();
            pdf.setName(file.getOriginalFilename());
            pdf.setData(file.getBytes());
            pdf.setUploadedAt(java.time.LocalDateTime.now());
            pdf.setUser(currentUser);

            pdfRepo.save(pdf);

            return "redirect:/pdf?success=uploaded";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/pdf?error=uploadfail";
        }
    }
    @GetMapping("/pdf/view/{id}")
    public ResponseEntity<?> viewPdf(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return ResponseEntity.status(401).body("You must be logged in");

        PdfFile pdf = pdfRepo.findById(id).orElse(null);
        if (pdf == null) return ResponseEntity.status(404).body("PDF not found");
        if (!pdf.getUser().getId().equals(currentUser.getId()))
            return ResponseEntity.status(403).body("Not authorized");

        if (pdf.getData() == null) return ResponseEntity.status(404).body("PDF data is empty");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdf.getName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.getData().length)
                .body(new ByteArrayResource(pdf.getData()));
    }


    @GetMapping("/pdf/download/{id}")
    public ResponseEntity<?> downloadPdf(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return ResponseEntity.status(401).body("You must be logged in");

        PdfFile pdf = pdfRepo.findById(id).orElse(null);
        if (pdf == null) return ResponseEntity.status(404).body("PDF not found");
        if (!pdf.getUser().getId().equals(currentUser.getId()))
            return ResponseEntity.status(403).body("Not authorized");
        if (pdf.getData() == null) return ResponseEntity.status(404).body("PDF data is empty");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.getName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.getData().length)
                .body(new ByteArrayResource(pdf.getData()));
    }

    @PostMapping("/pdf/delete/{id}")
    public String deletePdf(@PathVariable Long id, HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) return "redirect:/auth";

        PdfFile pdf = pdfRepo.findById(id).orElse(null);
        if (pdf == null) return "redirect:/pdf?error=notfound";

        if (!pdf.getUser().getId().equals(currentUser.getId()))
            return "redirect:/pdf?error=notauthorized";

        pdfRepo.delete(pdf);
        return "redirect:/pdf?success=deleted";
    }


    // ---------------- HELPER ----------------
    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }
}
