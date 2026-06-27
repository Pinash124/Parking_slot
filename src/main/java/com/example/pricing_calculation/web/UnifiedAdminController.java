package com.example.pricing_calculation.web;

import static com.example.pricing_calculation.dto.ManagementDtos.*;
import com.example.pricing_calculation.domain.*;
import com.example.pricing_calculation.service.UnifiedAdminService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin") @SecurityRequirement(name="bearerAuth")
@Tag(name="System Administrator",description="Tai khoan, phan quyen, cau hinh, camera, barrier va QR/RFID")
public class UnifiedAdminController {
    private final UnifiedAdminService service;public UnifiedAdminController(UnifiedAdminService service){this.service=service;}
    @GetMapping("/users") public List<UserView> users(){return service.users();}
    @PostMapping("/users") public UserView createUser(@RequestBody AdminUserCreateRequest r){return service.createUser(r);}
    @PatchMapping("/users/{id}") public UserView updateUser(@PathVariable Long id,@RequestBody UserUpdateRequest r){return service.updateUser(id,r);}
    @DeleteMapping("/users/{id}") public void deleteUser(@PathVariable Long id){service.deleteUser(id);}
    @GetMapping("/settings") public List<SystemSetting> settings(){return service.settings();}
    @PutMapping("/settings/{key}") public SystemSetting saveSetting(@PathVariable String key,@RequestBody SettingRequest r){return service.saveSetting(key,r);}
    @DeleteMapping("/settings/{key}") public void deleteSetting(@PathVariable String key){service.deleteSetting(key);}
    @GetMapping("/devices") public List<SystemDevice> devices(@RequestParam(required=false)String type){return service.devices(type);}
    @PostMapping("/devices") public SystemDevice createDevice(@RequestBody DeviceRequest r){return service.saveDevice(null,r);}
    @PutMapping("/devices/{id}") public SystemDevice updateDevice(@PathVariable Long id,@RequestBody DeviceRequest r){return service.saveDevice(id,r);}
    @DeleteMapping("/devices/{id}") public void deleteDevice(@PathVariable Long id){service.deleteDevice(id);}
}
