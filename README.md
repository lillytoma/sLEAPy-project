# sLEAPy–sprint1-project

## Overview
This repository establishes the core workflow, collaboration structure, and early project scaffolding that the team will build upon in future sprints.

---

## Project Description
Sprint 1 focuses on setting up the essential groundwork for the sLEAPy project. Key objectives include:

- [Placeholder]
  
More detailed feature descriptions will be added as the project evolves.

---

## Branching Strategy
We follow **trunk‑based development** with **short‑lived feature branches**:

- All stable code lives in `main`  
- New work is done in small, focused feature branches  
- Branches are merged quickly through pull requests  
- Frequent integration reduces merge conflicts and keeps progress continuous  

This approach supports fast iteration and strong team collaboration.

---

## Team Members
This project is developed by a team of five:

- **Lilly Toma**  
- **Robbie Thurston**  
- **Rishav Mohanty**  
- **Edosa Aigbuza**  
- **Ian Jackson**

---

## Initial Setup

1. Clone the repository:
   ```
   git clone <repo-url>
   cd sLEAPy-sprint1-project
   ```
2. Make sure you have installed:
   - Java 21
   - Maven
   - Docker

---

## Running the Program

1. Build the jar:
   ```
   cd starter
   mvn clean package
   ```
2. Build the Docker image:
   ```
   docker build -t myapp .
   ```
3. Run a container instance from the image:
   ```
   docker run --name myapp sLEAPy
   ```

That's it — the app runs inside the `myapp` container. Use `docker logs myapp` to view output, and `docker rm -f myapp` to remove it when done.

---

## Notes
This README will evolve throughout the sprint as more details are finalized.
