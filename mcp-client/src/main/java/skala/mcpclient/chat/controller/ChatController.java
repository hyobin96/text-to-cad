package skala.mcpclient.chat.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;
import skala.mcpclient.chat.dto.ChatRequest;
import skala.mcpclient.chat.dto.ChatResponseDto;
import skala.mcpclient.chat.service.ChatService;

import java.io.IOException;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatResponseDto> chat(@ModelAttribute ChatRequest request, HttpSession session) {
        try {
            String answer = chatService.chat(request, session.getId());
            return ResponseEntity.ok(new ChatResponseDto(answer));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ChatResponseDto(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(new ChatResponseDto("파일 저장 중 오류가 발생했습니다."));
        }
    }
}
