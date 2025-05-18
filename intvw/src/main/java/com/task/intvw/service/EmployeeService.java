package com.task.intvw.service;

import com.task.intvw.model.Employee;
import com.task.intvw.model.EmployeePair;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Duration;
import java.util.stream.Collectors;


@Service
public class EmployeeService {
    final int ID_COLUMN = 0;
    final int PROJECT_ID_COLUMN = 1;
    final int DATE_FROM_COLUMN = 2;
    final int DATE_TO_COLUMN = 3;

    public String getLongestPair(final MultipartFile file, final Model model) {
        try {
            final List<Employee> records = parseCSV(file);
            final Map<Integer, List<Employee>> employeesByProject = records.stream()
                    .collect(Collectors.groupingBy(Employee::projectId));

            final Map<String, Long> pairDurationMap = new HashMap<>();
            final Map<String, List<EmployeePair>> projectDetails = new HashMap<>();

            for (List<Employee> employeesInProject : employeesByProject.values()) {
                final int projectSize = employeesInProject.size();
                for (int i = 0; i < projectSize; i++) {
                    for (int j = i + 1; j < projectSize; j++) {
                        final Employee employee1 = employeesInProject.get(i);
                        final Employee employee2 = employeesInProject.get(j);

                        final LocalDate overlapStart = Collections.max(List.of(employee1.dateFrom(), employee2.dateFrom()));
                        final LocalDate overlapEnd = Collections.min(List.of(employee1.dateTo(), employee2.dateTo()));

                        if (!overlapStart.isAfter(overlapEnd)) {
                            final long days = Duration.between(overlapStart.atStartOfDay(), overlapEnd.atStartOfDay()).toDays();
                            final int id1 = Math.min(employee1.employeeId(), employee2.employeeId());
                            final int id2 = Math.max(employee1.employeeId(), employee2.employeeId());
                            final String pairKey = id1 + "-" + id2;

                            pairDurationMap.put(pairKey, pairDurationMap.getOrDefault(pairKey, 0L) + days);
                            projectDetails.computeIfAbsent(pairKey, k -> new ArrayList<>())
                                    .add(new EmployeePair(id1, id2, employee1.projectId(), days));
                        }
                    }
                }
            }

            final Optional<Map.Entry<String, Long>> maxPair = pairDurationMap.entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue());

            if (maxPair.isPresent()) {
                final String bestPairKey = maxPair.get().getKey();
                final List<EmployeePair> resultPairs = projectDetails.get(bestPairKey);

                model.addAttribute("bestPairs", resultPairs);
                model.addAttribute("totalDays", maxPair.get().getValue());
            } else {
                model.addAttribute("message", "No overlapping projects found.");
            }
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", "Error processing the file: " + e.getMessage());
        }

        return "upload";
    }

    public List<Employee> parseCSV(final MultipartFile file) {
        final List<Employee> records = new ArrayList<>();
        try (BufferedReader excelFile = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            final DateTimeFormatter[] formatters = {
                    // Most common formats - Europe, USA, 8601 etc
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"),  //17-05-2025
                    DateTimeFormatter.ofPattern("MM/dd/yyyy"),  //05/17/2025
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),  //2025-05-17
            };

            int currentLine = 1;
            while ((line = excelFile.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 4) {
                    throw new IllegalArgumentException("Incorrect number of columns on line [" + currentLine + "]. Expected 4 columns, but found " + parts.length);
                }

                try {
                    final int empId = Integer.parseInt(parts[ID_COLUMN].trim());
                    final int projectId = Integer.parseInt(parts[PROJECT_ID_COLUMN].trim());
                    final LocalDate dateFrom = parseDate(parts[DATE_FROM_COLUMN].trim(), formatters);
                    final LocalDate dateTo = parts[DATE_TO_COLUMN].trim().equalsIgnoreCase("NULL")
                            ? LocalDate.now()
                            : parseDate(parts[DATE_TO_COLUMN].trim(), formatters);

                    records.add(new Employee(empId, projectId, dateFrom, dateTo));
                    currentLine++;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number format on line " + currentLine + " for Employee ID or Project ID: " + e.getMessage());
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Invalid date format on line " + currentLine + " for Date From or Date To: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV file: " + e.getMessage(), e);
        }

        return records;
    }

    private LocalDate parseDate(final String dateStr, final DateTimeFormatter[] formatters) {
        for (final DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Wrong format, try different one
            }
        }
        throw new IllegalArgumentException("Invalid date format: " + dateStr);
    }
}
