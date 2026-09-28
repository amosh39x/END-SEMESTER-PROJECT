package com.garagedesk.service;

import com.garagedesk.model.*;
import com.garagedesk.model.JobCard.Status;
import com.garagedesk.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core business rules:
 *  - one job card per vehicle (no duplicate open cards)
 *  - a job is scheduled only when BOTH a free bay and a free mechanic exist -> no bay conflicts
 *  - otherwise it waits in a queue ordered by priority, then arrival time
 *  - whenever a bay frees up (complete / delete), waiting jobs are auto-dispatched
 */
@Service
@Transactional
public class SchedulingService {
    private static final List<Status> ACTIVE = List.of(Status.SCHEDULED, Status.IN_PROGRESS);
    private static final List<Status> OPEN = List.of(Status.WAITING, Status.SCHEDULED, Status.IN_PROGRESS);

    private final JobCardRepository jobs;
    private final BayRepository bays;
    private final MechanicRepository mechanics;

    public SchedulingService(JobCardRepository jobs, BayRepository bays, MechanicRepository mechanics) {
        this.jobs = jobs; this.bays = bays; this.mechanics = mechanics;
    }

    public JobCard create(JobCard in) {
        if (jobs.existsByVehicleNumberIgnoreCaseAndStatusIn(in.vehicleNumber.trim(), OPEN))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle " + in.vehicleNumber + " already has an open job card");
        in.id = null;
        in.vehicleNumber = in.vehicleNumber.trim().toUpperCase();
        in.status = Status.WAITING; in.progress = 0; in.bay = null; in.mechanic = null;
        in.createdAt = LocalDateTime.now();
        if (in.priority == null) in.priority = JobCard.Priority.NORMAL;
        JobCard saved = jobs.save(in);
        tryAssign(saved);
        return saved;
    }

    public JobCard update(Long id, JobCard in) {
        JobCard j = get(id);
        j.vehicleModel = in.vehicleModel; j.customerName = in.customerName;
        j.customerPhone = in.customerPhone; j.issue = in.issue;
        if (in.priority != null) j.priority = in.priority;
        return jobs.save(j);
    }

    public JobCard setStatus(Long id, Status s) {
        JobCard j = get(id);
        if (j.status == Status.WAITING && s != Status.WAITING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job is waiting for a free bay and mechanic");
        if (s == Status.WAITING) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move a job back to WAITING");
        j.status = s;
        if (s == Status.COMPLETED) finish(j);
        else if (s == Status.SCHEDULED) j.progress = 0;
        jobs.save(j);
        dispatchWaiting();
        return j;
    }

    public JobCard setProgress(Long id, int value) {
        JobCard j = get(id);
        if (j.status == Status.WAITING) throw new ResponseStatusException(HttpStatus.CONFLICT, "Job has no bay yet");
        if (j.status == Status.COMPLETED) throw new ResponseStatusException(HttpStatus.CONFLICT, "Job already completed");
        j.progress = Math.max(0, Math.min(100, value));
        if (j.progress >= 100) finish(j);
        else if (j.progress > 0) j.status = Status.IN_PROGRESS;
        jobs.save(j);
        dispatchWaiting();
        return j;
    }

    public void delete(Long id) {
        jobs.delete(get(id));
        jobs.flush();
        dispatchWaiting();
    }

    public int dispatchWaiting() {
        List<JobCard> waiting = jobs.findByStatusIn(List.of(Status.WAITING)).stream()
                .sorted(Comparator.comparing((JobCard j) -> j.priority).thenComparing(j -> j.createdAt))
                .toList();
        int n = 0;
        for (JobCard j : waiting) if (tryAssign(j)) n++;
        return n;
    }

    private boolean tryAssign(JobCard j) {
        List<JobCard> active = jobs.findByStatusIn(ACTIVE);
        Set<Long> busyBays = active.stream().filter(a -> a.bay != null).map(a -> a.bay.id).collect(Collectors.toSet());
        Set<Long> busyMechs = active.stream().filter(a -> a.mechanic != null).map(a -> a.mechanic.id).collect(Collectors.toSet());
        Optional<Bay> bay = bays.findAll().stream().filter(b -> !busyBays.contains(b.id)).findFirst();
        Optional<Mechanic> mech = mechanics.findAll().stream().filter(m -> !busyMechs.contains(m.id)).findFirst();
        if (bay.isEmpty() || mech.isEmpty()) return false;
        j.bay = bay.get(); j.mechanic = mech.get();
        j.status = Status.SCHEDULED; j.scheduledAt = LocalDateTime.now();
        jobs.save(j);
        return true;
    }

    private void finish(JobCard j) {
        j.status = Status.COMPLETED; j.progress = 100; j.completedAt = LocalDateTime.now();
    }

    public JobCard get(Long id) {
        return jobs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job card " + id + " not found"));
    }

    /** Customer-facing status lookup, including queue position for waiting vehicles. */
    public Map<String, Object> track(String vehicle) {
        List<JobCard> list = jobs.findByVehicleNumberIgnoreCaseOrderByCreatedAtDesc(vehicle.replace(" ", ""));
        if (list.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No job card found for " + vehicle);
        JobCard j = list.get(0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("job", j);
        if (j.status == Status.WAITING) {
            long ahead = jobs.findByStatusIn(List.of(Status.WAITING)).stream()
                    .filter(w -> w.priority.compareTo(j.priority) < 0
                            || (w.priority == j.priority && w.createdAt.isBefore(j.createdAt))).count();
            m.put("queuePosition", ahead + 1);
        }
        return m;
    }

    public Bay deleteBay(Long id) {
        if (jobs.existsByBay_Id(id) && jobs.findByStatusIn(ACTIVE).stream().anyMatch(a -> a.bay != null && a.bay.id.equals(id)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bay is occupied by an active job");
        jobs.findAll().stream().filter(j -> j.bay != null && j.bay.id.equals(id)).forEach(j -> { j.bay = null; jobs.save(j); });
        bays.deleteById(id);
        return null;
    }

    public void deleteMechanic(Long id) {
        if (jobs.findByStatusIn(ACTIVE).stream().anyMatch(a -> a.mechanic != null && a.mechanic.id.equals(id)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mechanic is assigned to an active job");
        jobs.findAll().stream().filter(j -> j.mechanic != null && j.mechanic.id.equals(id)).forEach(j -> { j.mechanic = null; jobs.save(j); });
        mechanics.deleteById(id);
    }

    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Status s : Status.values()) m.put(s.name(), jobs.findByStatusIn(List.of(s)).size());
        long busy = jobs.findByStatusIn(ACTIVE).size();
        m.put("totalBays", bays.count());
        m.put("freeBays", bays.count() - busy);
        m.put("mechanics", mechanics.count());
        return m;
    }
}
