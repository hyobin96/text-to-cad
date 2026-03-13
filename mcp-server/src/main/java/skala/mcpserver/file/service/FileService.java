package skala.mcpserver.file.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileService {

    private final Path uploadDir;

    public FileService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Tool(description = "저장된 파일의 목록을 반환합니다.")
    public List<String> listFiles() throws IOException {
        if (!Files.exists(uploadDir) || !Files.isDirectory(uploadDir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(uploadDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }
}
