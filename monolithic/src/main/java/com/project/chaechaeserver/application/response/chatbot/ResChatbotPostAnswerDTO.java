package com.project.chaechaeserver.application.response.chatbot;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResChatbotPostAnswerDTO {

    @Schema(example = "시금치는 냉장보관을 합니다.")
    private String answer;

}