package skala.mcpserver.config;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import skala.mcpserver.cad.service.CadService;
import skala.mcpserver.document.service.DocumentService;
import skala.mcpserver.file.service.FileService;
import skala.mcpserver.rag.service.RagRetrievalToolService;

@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            CadService cadService,
            DocumentService documentService,
            FileService fileService,
            RagRetrievalToolService ragRetrievalToolService
    ) {
        return MethodToolCallbackProvider.builder().toolObjects(
                cadService,
                documentService,
                fileService,
                ragRetrievalToolService
        ).build();
    }
}
