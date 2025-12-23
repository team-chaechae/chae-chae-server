package com.project.chatbotservice.presentation.controller.chatbot;

import com.project.chatbotservice.application.global.constants.ResCode;
import com.project.chatbotservice.application.global.dto.ResDTO;
import com.project.chatbotservice.application.response.chatbot.ResChatbotPostAnswerDTO;
import com.project.chatbotservice.application.service.chatbot.ChatbotService;
import com.project.chatbotservice.presentation.controller.chatbot.docs.ChatbotControllerSwagger;
import com.project.chatbotservice.presentation.request.chatbot.ReqChatbotPostQuestionDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatbotController implements ChatbotControllerSwagger {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<ResDTO<ResChatbotPostAnswerDTO>> ask(@Valid @RequestBody ReqChatbotPostQuestionDTO dto) {

        return new ResponseEntity<>(
                ResDTO.<ResChatbotPostAnswerDTO>builder()
                        .code(ResCode.OK)
                        .message("질문 응답 완료")
                        .data(chatbotService.ask(dto))
                        .build(),
                HttpStatus.OK
        );
    }
}
