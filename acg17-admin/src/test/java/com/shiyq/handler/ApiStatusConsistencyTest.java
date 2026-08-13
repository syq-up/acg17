package com.shiyq.handler;

import com.shiyq.entity.VO.ResultVO;
import com.shiyq.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ApiStatusConsistencyTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new StatusController())
                .setControllerAdvice(new CustomExceptionHandler())
                .build();
    }

    @Test
    void createdStatusMatchesResponseCode() throws Exception {
        mockMvc.perform(post("/status/created"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(HttpStatus.CREATED.value()));
    }

    @Test
    void apiExceptionStatusMatchesResponseCode() throws Exception {
        mockMvc.perform(get("/status/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void illegalArgumentStatusMatchesResponseCode() throws Exception {
        mockMvc.perform(get("/status/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()));
    }

    @RestController
    private static class StatusController {

        @PostMapping("/status/created")
        ResponseEntity<ResultVO> created() {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResultVO.created("创建成功", null));
        }

        @GetMapping("/status/not-found")
        ResultVO notFound() {
            throw ApiException.notFound("资源不存在");
        }

        @GetMapping("/status/bad-request")
        ResultVO badRequest() {
            throw new IllegalArgumentException("参数错误");
        }
    }
}
