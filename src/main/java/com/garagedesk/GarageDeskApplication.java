package com.garagedesk;

import com.garagedesk.model.*;
import com.garagedesk.repo.*;
import com.garagedesk.service.SchedulingService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GarageDeskApplication {
    public static void main(String[] args) { SpringApplication.run(GarageDeskApplication.class, args); }

    /** Seeds demo data on first run and prints a clickable link in the IntelliJ Run console. */
    @Bean
    CommandLineRunner init(BayRepository bays, MechanicRepository mechs, SchedulingService svc) {
        return args -> {
            if (bays.count() == 0) {
                bays.save(new Bay("Bay 1", "Lift"));
                bays.save(new Bay("Bay 2", "Wheel Alignment"));
                bays.save(new Bay("Bay 3", "General Service"));
                mechs.save(new Mechanic("Ravi Kumar", "Engine"));
                mechs.save(new Mechanic("Arun Prakash", "Brakes & Suspension"));
                mechs.save(new Mechanic("Meena Devi", "Electrical"));
                svc.create(job("TN34AB1234", "Maruti Swift", "Karthik", "9876543210", "Engine noise and oil leak", JobCard.Priority.HIGH));
                svc.create(job("TN52CD5678", "Hyundai i20", "Suresh", "9123456780", "Full periodic service", JobCard.Priority.NORMAL));
                svc.create(job("TN30EF9012", "Tata Nexon", "Divya", "9988776655", "Brake pads worn out", JobCard.Priority.URGENT));
                svc.create(job("TN34GH3456", "Honda City", "Anand", "9090909090", "AC not cooling", JobCard.Priority.NORMAL));
            }
            System.out.println("\n  GarageDesk is running -> http://localhost:8080\n  H2 console            -> http://localhost:8080/h2-console (jdbc:h2:file:./data/garagedesk, user: sa)\n");
        };
    }

    private static JobCard job(String v, String m, String c, String p, String i, JobCard.Priority pr) {
        JobCard j = new JobCard();
        j.vehicleNumber = v; j.vehicleModel = m; j.customerName = c; j.customerPhone = p; j.issue = i; j.priority = pr;
        return j;
    }
}
