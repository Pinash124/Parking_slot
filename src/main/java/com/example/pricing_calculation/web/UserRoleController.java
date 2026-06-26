package com.example.pricing_calculation.web;

import com.example.pricing_calculation.domain.UserRole;
import com.example.pricing_calculation.dto.UserRoleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "User Roles", description = "Role va quyen han nguoi dung trong he thong bai xe")
public class UserRoleController {

    @GetMapping
    @Operation(summary = "Danh sach role", description = "Tra ve cac role hien co va danh sach quyen/capability tuong ung.")
    @ApiResponse(responseCode = "200", description = "Danh sach role",
            content = @Content(schema = @Schema(implementation = UserRoleResponse.class)))
    public List<UserRoleResponse> listRoles() {
        return Arrays.stream(UserRole.values())
                .map(UserRoleResponse::from)
                .toList();
    }

    @GetMapping("/parking-user")
    @Operation(
            summary = "Role Parking User / Driver",
            description = "Role mac dinh cho tai khoan dang ky moi, dung cho nguoi gui xe/lai xe."
    )
    @ApiResponse(responseCode = "200", description = "Thong tin role Parking User / Driver",
            content = @Content(schema = @Schema(implementation = UserRoleResponse.class)))
    public UserRoleResponse parkingUserRole() {
        return UserRoleResponse.from(UserRole.PARKING_USER);
    }
}
