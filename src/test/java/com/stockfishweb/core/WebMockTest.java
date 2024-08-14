package com.stockfishweb.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SfController.class)
class WebMockTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SfService service;

    @Test
    void greetingShouldReturnMessageFromService() throws Exception {
        when(service.getBestMoveAsync()).thenReturn("e2e4");
//        when(service.getBestMove(any())).thenReturn("e2e4");
        this.mockMvc.perform(get("/")).andDo(print()).andExpect(status().isOk())
                .andExpect(content()
//                        .string(containsString("<html><body><h1>Please use POST instead of GET ;)</h1></body></html>")));
                    .json("{'error':'POST must be used instead of GET'}"));
    }
}
