package com.project.chatbotservice.application.service.chatbot;

import com.project.chatbotservice.application.response.chatbot.ResChatbotPostAnswerDTO;
import com.project.chatbotservice.presentation.request.chatbot.ReqChatbotPostQuestionDTO;

public interface ChatbotService {

    ResChatbotPostAnswerDTO ask(ReqChatbotPostQuestionDTO dto);
}
