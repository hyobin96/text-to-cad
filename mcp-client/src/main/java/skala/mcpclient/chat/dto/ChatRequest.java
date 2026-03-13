package skala.mcpclient.chat.dto;

import java.util.List;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ChatRequest {

    private String message;
    private List<MultipartFile> fileList;

}
