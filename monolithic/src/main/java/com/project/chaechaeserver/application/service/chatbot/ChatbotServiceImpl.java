package com.project.chaechaeserver.application.service.chatbot;

import com.project.chaechaeserver.application.response.chatbot.ResChatbotPostAnswerDTO;
import com.project.chaechaeserver.presentation.request.chatbot.ReqChatbotPostQuestionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    @Value("${chatbot.base-url.fast-api}")
    private String baseUrl;

    public final RestClient restClient;

    public ResChatbotPostAnswerDTO ask(ReqChatbotPostQuestionDTO dto) {

        URI uri = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/rag/ask")
                .encode()
                .build()
                .toUri();

        return restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(ResChatbotPostAnswerDTO.class);
    }
}
