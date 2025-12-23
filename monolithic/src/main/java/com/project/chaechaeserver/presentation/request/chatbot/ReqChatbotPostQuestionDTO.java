package com.project.chaechaeserver.presentation.request.chatbot;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqChatbotPostQuestionDTO {

    @Schema(example = "시금치의 보관방법을 알려주세요")
    @NotBlank(message = "질문을 입력해주세요.")
    private String question;

}
