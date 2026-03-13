package skala.mcpserver.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
@Slf4j
public class DocumentService {

    private final VectorStore vectorStore;
    private final Path uploadDir;

    public DocumentService(
            VectorStore vectorStore,
            @Value("${app.upload-dir:uploads}") String uploadDir
    ) {
        this.vectorStore = vectorStore;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Tool(description = "저장된 문서를 읽어서 VectorDB에 저장하는 파이프라인을 실행합니다.")
    public List<Document> executeETL(String filename) throws IOException {
        log.info("Starting document ETL pipeline for file: {}", filename);

        List<Document> documents = load(filename);
        List<Document> chunkedDocuments = chunk(documents);

        vectorStore.write(chunkedDocuments);
        log.info("Ending document ETL pipeline for file: {}", filename);

        return chunkedDocuments;
    }

    public List<Document> load(String filename) {
        Path path = uploadDir.resolve(filename).normalize();
        Resource resource = new FileSystemResource(path);
        DocumentReader reader = new TikaDocumentReader(resource);
        return reader.get();
    }

    public List<Document> chunk(List<Document> documents) {
        DocumentTransformer transformer = new TokenTextSplitter(
                800,
                350,
                5,
                10000,
                true
        );
        return transformer.apply(documents);
    }
}
