package com.team.corporate.backend.handlers;

import com.team.corporate.core.services.CsvImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/imports")
public class ImportsHandler {
    private final CsvImportService importer;

    public ImportsHandler(CsvImportService importer) {
        this.importer = importer;
    }

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CsvImportService.Result csv(@RequestParam("file") List<MultipartFile> files) throws IOException {
        var sources = files.stream()
                .map(file -> new CsvImportService.Source(file.getOriginalFilename(), file::getInputStream))
                .toList();
        return importer.importFiles(sources);
    }
}
