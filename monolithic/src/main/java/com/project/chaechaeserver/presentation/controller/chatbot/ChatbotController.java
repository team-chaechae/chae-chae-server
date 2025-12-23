package com.project.chaechaeserver.presentation.controller.chatbot;

import com.project.chaechaeserver.application.global.constants.ResCode;
import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.chatbot.ResChatbotPostAnswerDTO;
import com.project.chaechaeserver.application.service.chatbot.ChatbotService;
import com.project.chaechaeserver.presentation.request.chatbot.ReqChatbotPostQuestionDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.project.chaechaeserver.domain.model.user.constraint.RoleType.Role.ADMIN;
import static com.project.chaechaeserver.domain.model.user.constraint.RoleType.Role.EMPLOYEE;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    @Secured({ADMIN, EMPLOYEE})
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
