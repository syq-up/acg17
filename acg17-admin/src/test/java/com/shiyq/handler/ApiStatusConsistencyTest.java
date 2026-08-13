package com.shiyq.handler;

import com.shiyq.entity.VO.ResultVO;
import com.shiyq.entity.DTO.NovelCreateDTO;
import com.shiyq.entity.DTO.GameUploadDTO;
import com.shiyq.exception.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void invalidRequestBodyReturnsBadRequestWithValidationMessage() throws Exception {
        mockMvc.perform(post("/status/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.msg").value("书名不能为空"));
    }

    @Test
    void invalidDirectRequestParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/status/positive").param("pageNum", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.msg").value("页码必须大于0"));
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/status/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.msg").value("请求体格式错误"));
    }

    @Test
    void invalidMultipartModelReturnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/status/model"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void missingAndWrongTypeParametersReturnBadRequest() throws Exception {
        mockMvc.perform(get("/status/positive"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.msg").value("缺少请求参数: pageNum"));

        mockMvc.perform(get("/status/positive").param("pageNum", "wrong"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.msg").value("请求参数类型错误: pageNum"));
    }

    @Test
    void oversizedUploadReturnsContentTooLarge() {
        ResponseEntity<ResultVO> response = new CustomExceptionHandler()
                .handleMaxUploadSizeExceededException(new MaxUploadSizeExceededException(1024));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, response.getStatusCode());
        assertEquals(HttpStatus.CONTENT_TOO_LARGE.value(), response.getBody().get("code"));
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

        @PostMapping("/status/validated")
        ResultVO validated(@Valid @RequestBody NovelCreateDTO request) {
            return ResultVO.success(request);
        }

        @GetMapping("/status/positive")
        ResultVO positive(
                @RequestParam @Positive(message = "页码必须大于0") int pageNum) {
            return ResultVO.success(pageNum);
        }

        @PostMapping("/status/model")
        ResultVO model(@Valid GameUploadDTO request) {
            return ResultVO.success(request);
        }
    }
}
