package com.project.chaechaeserver.application.service.chatbot;

import com.project.chaechaeserver.application.response.chatbot.ResChatbotPostAnswerDTO;
import com.project.chaechaeserver.presentation.request.chatbot.ReqChatbotPostQuestionDTO;

public interface ChatbotService {

    ResChatbotPostAnswerDTO ask(ReqChatbotPostQuestionDTO dto);
}
