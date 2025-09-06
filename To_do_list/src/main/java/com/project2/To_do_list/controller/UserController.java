package com.project2.To_do_list.controller;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    // ---------------- LOGIN & SIGNUP ----------------
    @GetMapping({"/", "/auth"})
    public String showLoginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "loginpage"; // Thymeleaf login template
    }

    @PostMapping("/auth/login")
    public String loginUser(@ModelAttribute LoginRequest loginRequest, Model model) {
        User user = userRepo.findByEmailAndPassword(
                loginRequest.getEmail(),
                loginRequest.getPassword());

        if (user != null) {
            return "redirect:/home"; // Spring Security will handle authentication
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "loginpage";
        }
    }

    @GetMapping("/signup")
    public String getSignupPage(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(@ModelAttribute User user) {
        userRepo.save(user);
        return "redirect:/auth";
    }

    // ---------------- HOME ----------------
    @GetMapping("/home")
    public String getHomepage(Principal principal, Model model) {
        if (principal == null) return "redirect:/auth";

        String email = principal.getName();
        User currentUser = userRepo.findByEmail(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    userRepo.save(u);
                    return u;
                });

        model.addAttribute("currentUser", currentUser);
        return "home";
    }

    // ---------------- HABIT TRACKER ----------------
    @GetMapping("/habits")
    public String showHabits(Model model, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return "redirect:/auth";

        List<Habit> habits = habitRepo.findByUser(currentUser);
        model.addAttribute("habits", habits);
        model.addAttribute("newHabit", new Habit());
        model.addAttribute("isCalendarView", false);
        return "habits";
    }

    @PostMapping("/habits")
    public String addHabit(@ModelAttribute("newHabit") Habit newHabit, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return "redirect:/auth";

        newHabit.setUser(currentUser);
        newHabit.setCompletedDays(0);
        habitRepo.save(newHabit);
        return "redirect:/habits";
    }

    @GetMapping("/calendar")
    public String showCalendar(Model model, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return "redirect:/auth";

        List<Habit> habits = habitRepo.findByUser(currentUser);
        model.addAttribute("habits", habits);
        model.addAttribute("newHabit", new Habit());
        model.addAttribute("isCalendarView", true);
        return "habits";
    }

    @PostMapping("/habits/increment/{id}")
    public String incrementHabit(@PathVariable Long id, Principal principal) {
        User currentUser = getCurrentUser(principal);
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
    public String resetHabit(@PathVariable Long id, Principal principal) {
        User currentUser = getCurrentUser(principal);
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
    public String deleteHabit(@PathVariable Long id, Principal principal) {
        User currentUser = getCurrentUser(principal);
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
    public String showRuleTracker(Model model, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return "redirect:/auth";

        List<Concept> concepts = conceptService.getUserConcepts(currentUser);

        for (Concept concept : concepts) {
            int completed = (int) concept.getRevisions()
                    .stream().filter(Revision::isCompleted).count();
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
    public String addConcept(@RequestParam String name, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return "redirect:/auth";

        conceptService.addConcept(name, currentUser);
        return "redirect:/147ruletracker";
    }

    @PostMapping("/147ruletracker/toggle/{conceptId}/{revisionIndex}")
    public String toggleRevision(@PathVariable Long conceptId,
                                 @PathVariable int revisionIndex,
                                 Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return "redirect:/auth";

        conceptService.toggleRevision(conceptId, revisionIndex, currentUser);
        return "redirect:/147ruletracker";
    }

    @PostMapping("/147ruletracker/delete/{conceptId}")
    public String deleteConcept(@PathVariable Long conceptId, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return "redirect:/auth";

        conceptService.deleteConcept(conceptId, currentUser);
        return "redirect:/147ruletracker";
    }

    // ---------------- NOTES ----------------
    @GetMapping("/notes")
    public String showNotes(Principal principal, Model model) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return "redirect:/auth";

        List<Note> notes = noteRepo.findByUser(currentUser);
        model.addAttribute("notes", notes);

        List<String> folders = notes.stream()
                .map(Note::getFolder).distinct().toList();
        model.addAttribute("folders", folders);

        return "notes";
    }

    @PostMapping("/notes")
    @ResponseBody
    public Note saveNote(@RequestBody Note note, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return null;

        note.setUser(currentUser);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepo.save(note);
    }

    @PutMapping("/notes/{id}")
    @ResponseBody
    public Note updateNote(@PathVariable Long id, @RequestBody Note noteData, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return null;

        Note note = noteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));
        if (!note.getUser().getId().equals(currentUser.getId())) return null;

        note.setTitle(noteData.getTitle());
        note.setBody(noteData.getBody());
        note.setFolder(noteData.getFolder());
        note.setUpdatedAt(LocalDateTime.now());

        return noteRepo.save(note);
    }

    @DeleteMapping("/notes/{id}")
    @ResponseBody
    public String deleteNote(@PathVariable Long id, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return "not-logged-in";

        Note note = noteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));
        if (!note.getUser().getId().equals(currentUser.getId())) return "not-authorized";

        noteRepo.delete(note);
        return "success";
    }

    @GetMapping("/notes/view/{id}")
    public ResponseEntity<String> viewNote(@PathVariable Long id, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return ResponseEntity.status(401).body("Not logged in");

        Note note = noteRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));
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
    public String showPdfLibrary(Principal principal, Model model) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return "redirect:/auth";

        List<PdfFile> pdfs = pdfRepo.findByUser(currentUser);
        model.addAttribute("pdfs", pdfs);
        return "pdf";
    }

    @PostMapping("/pdf/upload")
    public String uploadPdf(@RequestParam("file") MultipartFile[] files, Principal principal) throws IOException {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return "redirect:/auth";

        Path uploadPath = Paths.get(uploadDir);
        File uploadFolder = uploadPath.toFile();
        if (!uploadFolder.exists() && !uploadFolder.mkdirs()) {
            throw new IOException("Could not create upload folder: " + uploadDir);
        }

        for (MultipartFile file : files) {
            if (!"application/pdf".equals(file.getContentType())) continue;

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path destPath = uploadPath.resolve(fileName);
            file.transferTo(destPath.toFile());

            PdfFile pdf = new PdfFile();
            pdf.setUser(currentUser);
            pdf.setName(file.getOriginalFilename());
            pdf.setFilePath(destPath.toString());
            pdf.setUploadedAt(LocalDateTime.now());
            pdfRepo.save(pdf);
        }
        return "redirect:/pdf";
    }

    @GetMapping("/pdf/view/{id}")
    public ResponseEntity<Resource> viewPdf(@PathVariable Long id, Principal principal) throws IOException {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return ResponseEntity.status(401).build();

        PdfFile pdf = pdfRepo.findById(id).orElseThrow(() -> new RuntimeException("PDF not found"));
        if (!pdf.getUser().getId().equals(currentUser.getId())) return ResponseEntity.status(403).build();

        File file = new File(pdf.getFilePath());
        if (!file.exists()) throw new RuntimeException("File not found");

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdf.getName() + "\"")
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/pdf/download/{id}")
    public ResponseEntity<FileSystemResource> downloadPdf(@PathVariable Long id, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return ResponseEntity.status(401).build();

        PdfFile pdf = pdfRepo.findById(id).orElseThrow(() -> new RuntimeException("PDF not found"));
        if (!pdf.getUser().getId().equals(currentUser.getId())) return ResponseEntity.status(403).build();

        FileSystemResource resource = new FileSystemResource(pdf.getFilePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdf.getName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @PostMapping("/pdf/delete/{id}")
    public String deletePdf(@PathVariable Long id, Principal principal) {
        User currentUser = getCurrentUser(principal);
        if (currentUser == null) return "redirect:/auth";

        PdfFile pdf = pdfRepo.findById(id).orElseThrow(() -> new RuntimeException("PDF not found"));
        if (pdf.getUser().getId().equals(currentUser.getId())) {
            File file = new File(pdf.getFilePath());
            if (file.exists()) file.delete();
            pdfRepo.delete(pdf);
        }
        return "redirect:/pdf";
    }

    // ---------------- HELPER ----------------
    private User getCurrentUser(Principal principal) {
        if (principal == null) return null;
        return userRepo.findByEmail(principal.getName())
                .orElse(null);
    }
}
