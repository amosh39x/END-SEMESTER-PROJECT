package com.garagedesk.web;

import com.garagedesk.model.*;
import com.garagedesk.repo.*;
import com.garagedesk.service.SchedulingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final SchedulingService svc;
    private final JobCardRepository jobs;
    private final BayRepository bays;
    private final MechanicRepository mechanics;

    public ApiController(SchedulingService svc, JobCardRepository jobs, BayRepository bays, MechanicRepository mechanics) {
        this.svc = svc; this.jobs = jobs; this.bays = bays; this.mechanics = mechanics;
    }

    // ---- Job cards ----
    @GetMapping("/jobs") public List<JobCard> allJobs() { return jobs.findAll(); }
    @GetMapping("/jobs/{id}") public JobCard job(@PathVariable Long id) { return svc.get(id); }
    @PostMapping("/jobs") @ResponseStatus(HttpStatus.CREATED)
    public JobCard create(@Valid @RequestBody JobCard j) { return svc.create(j); }
    @PutMapping("/jobs/{id}") public JobCard update(@PathVariable Long id, @Valid @RequestBody JobCard j) { return svc.update(id, j); }
    @PatchMapping("/jobs/{id}/status") public JobCard status(@PathVariable Long id, @RequestParam JobCard.Status value) { return svc.setStatus(id, value); }
    @PatchMapping("/jobs/{id}/progress") public JobCard progress(@PathVariable Long id, @RequestParam int value) { return svc.setProgress(id, value); }
    @DeleteMapping("/jobs/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { svc.delete(id); }

    // ---- Scheduling / tracking ----
    @PostMapping("/schedule/run") public Map<String, Object> run() { return Map.of("assigned", svc.dispatchWaiting()); }
    @GetMapping("/track/{vehicle}") public Map<String, Object> track(@PathVariable String vehicle) { return svc.track(vehicle); }
    @GetMapping("/stats") public Map<String, Object> stats() { return svc.stats(); }

    // ---- Bays ----
    @GetMapping("/bays") public List<Bay> allBays() { return bays.findAll(); }
    @PostMapping("/bays") @ResponseStatus(HttpStatus.CREATED)
    public Bay addBay(@Valid @RequestBody Bay b) { b.id = null; Bay s = bays.save(b); svc.dispatchWaiting(); return s; }
    @DeleteMapping("/bays/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delBay(@PathVariable Long id) { svc.deleteBay(id); }

    // ---- Mechanics ----
    @GetMapping("/mechanics") public List<Mechanic> allMechanics() { return mechanics.findAll(); }
    @PostMapping("/mechanics") @ResponseStatus(HttpStatus.CREATED)
    public Mechanic addMech(@Valid @RequestBody Mechanic m) { m.id = null; Mechanic s = mechanics.save(m); svc.dispatchWaiting(); return s; }
    @DeleteMapping("/mechanics/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delMech(@PathVariable Long id) { svc.deleteMechanic(id); }
}
