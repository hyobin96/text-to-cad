package skala.mcpserver.cad.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class CadService {

    private static final String CAD_SCRIPT_SYSTEM_PROMPT = """
            You are an AutoCAD script generator.
            Convert the user's drafting request into a valid AutoCAD .scr command sequence.
            Return only executable script lines.
            Do not wrap the answer in markdown.
            Do not add explanations, numbering, comments, XML, or JSON.
            Use one command or argument per line when appropriate.
            If the request is ambiguous, make the smallest reasonable assumption that still yields a usable script.
            """;

    private final Path cadDir;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public CadService(
            @Value("${app.upload-dir:uploads}") String uploadDir,
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider
    ) {
        this.cadDir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("cad");
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    @Tool(description = "CAD 자동화를 위한 .scr 스크립트 파일을 생성합니다. fileName에는 확장자를 제외한 이름 또는 .scr 파일명을 넣고, commands에는 줄 단위 CAD 명령 목록을 전달합니다.")
    public String createScrFile(String fileName, List<String> commands) throws IOException {
        if (commands == null || commands.isEmpty()) {
            throw new IllegalArgumentException("commands must not be empty");
        }

        String normalizedFileName = normalizeFileName(fileName);
        Files.createDirectories(cadDir);

        Path targetPath = cadDir.resolve(normalizedFileName).normalize();
        if (!targetPath.startsWith(cadDir)) {
            throw new IllegalArgumentException("Invalid fileName");
        }

        String content = String.join(System.lineSeparator(), commands) + System.lineSeparator();
        Files.writeString(targetPath, content, StandardCharsets.UTF_8);

        log.info("Created CAD script file: {}", targetPath);
        return "SCR file created: %s (%d commands)".formatted(targetPath, commands.size());
    }

    @Tool(description = "자연어 요구사항을 AutoCAD .scr 명령 목록으로 생성합니다. 결과는 줄 단위 명령 리스트로 반환합니다.")
    public List<String> generateCadScript(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("prompt must not be blank");
        }

        log.info("Generating CAD script from prompt: {}", prompt);

        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        if (chatClientBuilder == null) {
            throw new IllegalStateException("ChatClient.Builder is not available");
        }

        String content = chatClientBuilder.build().prompt()
                .system(CAD_SCRIPT_SYSTEM_PROMPT)
                .user(prompt.strip())
                .call()
                .content();

        List<String> commands = parseCommands(content);
        if (commands.isEmpty()) {
            throw new IllegalStateException("Generated CAD script is empty");
        }

        log.info("Generated {} CAD commands", commands.size());
        return commands;
    }

    @Tool(description = "자연어 요구사항으로 CAD .scr 명령을 생성한 뒤 파일로 저장합니다.")
    public String generateAndSaveCadScript(String fileName, String prompt) throws IOException {
        List<String> commands = generateCadScript(prompt);
        return createScrFile(fileName, commands);
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }

        String trimmed = Paths.get(fileName.trim()).getFileName().toString();
        String baseName = trimmed.endsWith(".scr") ? trimmed : trimmed + ".scr";

        if (baseName.equals(".scr")) {
            throw new IllegalArgumentException("fileName must contain a valid name");
        }

        return baseName;
    }

    private List<String> parseCommands(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }

        return Arrays.stream(content.split("\\R"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(line -> !line.startsWith("```"))
                .toList();
    }
}
