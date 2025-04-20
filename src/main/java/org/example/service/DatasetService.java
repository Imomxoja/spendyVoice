package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.entity.Dataset;
import org.example.repository.DatasetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatasetService {
    private final DatasetRepository datasetRepository;

    @PostConstruct
    @Transactional
    public void saveDataset() {
        if (datasetRepository.count() > 0) {
            return;
        }
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            URL resource = classLoader.getResource("amazon_dataset");
            if (resource == null) {
                System.out.println("Dataset folder not found!");
                return;
            }


            File dir = new File(resource.toURI());
            File[] csvFiles = dir.listFiles((d, name) -> name.endsWith(".csv"));
            if (csvFiles == null) {
                return;
            }

            List<Dataset> categories = new ArrayList<>();
            for (File file : csvFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                    String headerLine = reader.readLine();
                    if (headerLine == null) continue;

                    String[] headers = splitCsvLine(headerLine);
                    int nameIdx = indexOf(headers, "name");
                    int mainCatIdx = indexOf(headers, "main_category");
                    int subCatIdx = indexOf(headers, "sub_category");

                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] row = splitCsvLine(line);
                        if (nameIdx >= 0 && row.length > nameIdx && !row[nameIdx].trim().isEmpty()) {
                            String productName = row[nameIdx].trim();
                            String category = (mainCatIdx >= 0 && row.length > mainCatIdx && !row[mainCatIdx].trim().isEmpty())
                                    ? row[mainCatIdx].trim()
                                    : file.getName().replace(".csv", "");
                            String subCategory = (subCatIdx >= 0 && row.length > subCatIdx && !row[subCatIdx].trim().isEmpty())
                                    ? row[subCatIdx].trim()
                                    : category;

                            categories.add(new Dataset(productName, category, subCategory));
                            if (categories.size() >= 1000) {
                                datasetRepository.saveAll(categories);
                                categories.clear();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + file.getName() + ": " + e.getMessage());
                }
            }
            if (!categories.isEmpty()) {
                datasetRepository.saveAll(categories);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    private int indexOf(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && array[i].trim().equalsIgnoreCase(value)) return i;
        }
        return -1;
    }

    public Long getCount() {
       return datasetRepository.count();
    }

    public void deleteAll() {
        datasetRepository.truncate();
    }
}
