package com.shuike.manager.modules.auth.dto;
import javax.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class RefreshTokenRequest {
    @NotBlank(message = "RefreshToken不能为空")
    private String refreshToken;
}
