# GarageDesk – Vehicle Service Job Card and Bay Scheduling System
Spring Boot 3 · Java 17 · Spring Data JPA · H2 · Maven

## Run in IntelliJ IDEA
1. File > Open > select the `pom.xml` (Open as Project). Wait for Maven to import.
2. Set Project SDK to JDK 17+ (File > Project Structure).
3. Run `GarageDeskApplication`.
4. Ctrl+Click the link in the Run console: **http://localhost:8080**

H2 console: http://localhost:8080/h2-console  (JDBC URL `jdbc:h2:file:./data/garagedesk`, user `sa`, blank password)

## Postman
Import `postman/GarageDesk.postman_collection.json` (variable `baseUrl` = http://localhost:8080).

## Business rules
- One open job card per vehicle. A new card is auto-scheduled into a free bay with a free mechanic.
- No free bay or mechanic: the card waits in a queue (URGENT > HIGH > NORMAL, then arrival time).
- Completing or deleting a job frees its bay and the next waiting job is assigned automatically.
- Status flow: WAITING > SCHEDULED > IN_PROGRESS > COMPLETED; progress 100% completes the job.
