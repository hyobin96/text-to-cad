package skala.mcpclient.file.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉토리 생성 실패: " + this.uploadDir, e);
        }
    }

    public List<String> saveFiles(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<String> savedFileNames = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String rawFilename = file.getOriginalFilename();
            String originalFilename = StringUtils.hasText(rawFilename) ? StringUtils.cleanPath(rawFilename) : "file";
            if (originalFilename.contains("..")) {
                throw new IllegalArgumentException("잘못된 파일명입니다: " + originalFilename);
            }

            String savedFileName = UUID.randomUUID() + "_" + originalFilename;
            Path targetPath = uploadDir.resolve(savedFileName);

            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            savedFileNames.add(savedFileName);
        }

        return savedFileNames;
    }
}
