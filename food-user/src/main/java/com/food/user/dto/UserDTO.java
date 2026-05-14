package com.food.user.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer role;
    private String phone;
    private String email;
    private LocalDateTime createTime;
}
