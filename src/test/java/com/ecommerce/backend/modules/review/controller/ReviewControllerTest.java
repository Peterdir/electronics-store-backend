package com.ecommerce.backend.modules.review.controller;

import com.ecommerce.backend.common.exception.GlobalExceptionHandler;
import com.ecommerce.backend.modules.review.dto.response.ReviewResponse;
import com.ecommerce.backend.modules.review.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewControllerTest {

    private final ReviewService service = mock(ReviewService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject("7").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        mvc = MockMvcBuilders.standaloneSetup(new ReviewController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bindsMultipartPutToUpdateRequestAndImage() throws Exception {
        var image = new MockMultipartFile("image", "replacement.png", "image/png", new byte[]{1});
        when(service.updateReview(eq(7L), eq(11L), any(), eq(image)))
                .thenReturn(ReviewResponse.builder().rating(4.0).build());

        mvc.perform(multipart(HttpMethod.PUT, "/api/reviews/{reviewId}", 11)
                        .file(image)
                        .param("rating", "4")
                        .param("reviewText", "Updated review"))
                .andExpect(status().isOk());

        verify(service).updateReview(eq(7L), eq(11L),
                argThat(request -> Double.valueOf(4.0).equals(request.getRating())
                        && "Updated review".equals(request.getReviewText())),
                eq(image));
    }

    @Test
    void rejectsMultipartPutWithMultipleImages() throws Exception {
        mvc.perform(multipart(HttpMethod.PUT, "/api/reviews/{reviewId}", 11)
                        .file(new MockMultipartFile("image", "first.png", "image/png", new byte[]{1}))
                        .file(new MockMultipartFile("image", "second.png", "image/png", new byte[]{2}))
                        .param("rating", "4"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
