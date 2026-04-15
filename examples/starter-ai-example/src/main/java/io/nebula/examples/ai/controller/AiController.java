package io.nebula.examples.ai.controller;

import io.nebula.ai.core.chat.ChatService;
import io.nebula.core.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {
    @Autowired(required = false)
    private ChatService chatService;

    @GetMapping("/ai/echo")
    public Result<String> echo(@RequestParam(defaultValue = "hello") String q) {
        if (chatService == null) {
            return Result.success("AI disabled");
        }
        String r = chatService.chat(q).getContent();
        return Result.success(r);
    }
}
