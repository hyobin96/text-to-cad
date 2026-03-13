package skala.mcpserver.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RagRetrievalToolService {

    private static final int DEFAULT_TOP_K = 4;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.30;

    private final VectorStore vectorStore;

    public RagRetrievalToolService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "저장된 문서와 적재된 정보에서 질문과 관련된 근거를 검색합니다. 제원, 규격, 사양, 참고 자료를 찾아야 할 때 사용합니다.")
    public String searchStoredInformation(String query) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("검색어를 입력해 주세요.");
        }

        log.info("Searching for knowledge related to: {}", query);
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(DEFAULT_TOP_K)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                .build();

        log.info("Search request: {}", request);

        List<Document> documents = vectorStore.similaritySearch(request);
        log.info("Search results: {}", documents);
        if (documents == null || documents.isEmpty()) {
            return "관련 문서를 찾지 못했습니다.";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            result.append("[검색 결과 ").append(i + 1).append("]\n");
            result.append("출처: ").append(resolveSource(document)).append('\n');

            if (document.getScore() != null) {
                result.append("유사도: ")
                        .append(String.format("%.3f", document.getScore()))
                        .append('\n');
            }

            result.append("내용: ").append(trimText(document.getText())).append("\n\n");
        }

        return result.toString().trim();
    }

    private String resolveSource(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return firstNonBlank(
                metadata.get("file_name"),
                metadata.get("filename"),
                metadata.get("source"),
                metadata.get("title"),
                document.getId()
        );
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value instanceof String text && StringUtils.hasText(text)) {
                return text;
            }
        }
        return "알 수 없음";
    }

    private String trimText(String text) {
        if (!StringUtils.hasText(text)) {
            return "(본문 없음)";
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 300) {
            return normalized;
        }

        return normalized.substring(0, 300) + "...";
    }
}
