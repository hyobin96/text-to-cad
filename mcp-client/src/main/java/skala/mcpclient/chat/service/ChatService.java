package skala.mcpclient.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import skala.mcpclient.chat.dto.ChatRequest;
import skala.mcpclient.file.service.FileStorageService;

import java.io.IOException;
import java.util.List;

@Service
public class ChatService {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            너는 도구를 스스로 선택해 사용하는 비서다.
            사용자의 요청과 도구 목록을 확인해서 사용자의 요청을 적절하게 처리해라.
            사용자가 저장된 정보, 저장된 문서, 업로드된 파일, 사양, 제원, 규격, 참고 자료, 근거를 요구하면 먼저 retrieval 도구를 호출해 관련 정보를 찾아라.
            저장된 정보를 활용하라는 요청에서는 모델의 일반 지식만으로 답하지 말고 retrieval 도구 결과를 근거로 답하라.
            retrieval 결과가 없으면 찾지 못했다고 분명히 설명하라.
            응답은 도구 호출 결과를 반영해 자연스러운 한국어로 작성해라.
            툴 실행이 실패하면 원인을 추측하지 마라.
            툴이 반환한 오류 메시지만 그대로 설명하라.
            목차 부족, 형식 문제 등 확인되지 않은 사유를 만들어내지 마라.
            """;

    private final ChatClient chatClient;
    private final FileStorageService fileStorageService;
    private final SyncMcpToolCallbackProvider toolCallbackProvider;

    public ChatService(ChatClient.Builder chatClientBuilder, FileStorageService fileStorageService,
                       VectorStore vectorStore, SyncMcpToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClientBuilder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1),
                        VectorStoreChatMemoryAdvisor.builder(vectorStore).build())
                .build();
        this.fileStorageService = fileStorageService;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    public String chat(ChatRequest request, String conversationId) throws IOException {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("메시지를 입력해 주세요.");
        }

        List<String> savedFileNames = fileStorageService.saveFiles(request.getFileList());
        String message = getSummaryMessage(request.getMessage(), savedFileNames);

        return chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(
                        ChatMemory.CONVERSATION_ID, conversationId
                ))
                .user(message)
                .toolCallbacks(toolCallbackProvider)
                .call()
                .content();
    }

    private String getSummaryMessage(String message, List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return message;
        }

        StringBuilder fileSummary = new StringBuilder();
        for (String fileName : fileNames) {
            fileSummary.append("- ").append(fileName).append('\n');
        }

        return message + "\n\n[첨부 파일(저장됨)]\n" + fileSummary;
    }
}
