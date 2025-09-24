package com.project.chaechaeserver.presentation.request.chatbot;

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

    @NotBlank(message = "질문을 입력해주세요.")
    private String question;

}
