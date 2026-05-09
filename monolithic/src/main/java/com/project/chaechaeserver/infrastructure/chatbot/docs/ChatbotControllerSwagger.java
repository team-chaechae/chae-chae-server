package com.project.chaechaeserver.infrastructure.chatbot.docs;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.chatbot.ResChatbotPostAnswerDTO;
import com.project.chaechaeserver.presentation.request.chatbot.ReqChatbotPostQuestionDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Chatbot", description = "챗봇 관련 API를 제공합니다.")
@RequestMapping("/api/chat")
public interface ChatbotControllerSwagger {

    @Operation(summary = "질의 생성", description = "질의를 생성하는 API 입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "질의 생성 성공", content = @Content(schema = @Schema(implementation = ResChatbotPostAnswerDTO.class))),
            @ApiResponse(responseCode = "400", description = "질의 생성 실패", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PostMapping("/ask")
    ResponseEntity<ResDTO<ResChatbotPostAnswerDTO>> ask(@Valid @RequestBody ReqChatbotPostQuestionDTO dto);
}
