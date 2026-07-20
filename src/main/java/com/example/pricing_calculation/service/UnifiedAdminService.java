package com.example.pricing_calculation.service;

import static com.example.pricing_calculation.dto.ManagementDtos.*;
import com.example.pricing_calculation.domain.*;
import com.example.pricing_calculation.repository.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnifiedAdminService {
    private final UserAccountRepository users;private final PasswordHashService passwords;private final SystemSettingRepository settings;private final SystemDeviceRepository devices;
    public UnifiedAdminService(UserAccountRepository users,PasswordHashService passwords,SystemSettingRepository settings,SystemDeviceRepository devices){this.users=users;this.passwords=passwords;this.settings=settings;this.devices=devices;}
    @Transactional(readOnly=true) public List<UserView> users(){return users.findAll().stream().map(UserView::from).toList();}
    @Transactional public UserView createUser(AdminUserCreateRequest r){if(r==null||r.email()==null||r.password()==null||r.fullName()==null)throw new BadRequestException("fullName, email and password are required");if(users.existsByEmailIgnoreCase(r.email()))throw new BadRequestException("Email already exists");UserAccount u=new UserAccount();u.setFullName(r.fullName());u.setEmail(r.email().trim().toLowerCase());u.setPhone(r.phone());u.setPasswordHash(passwords.hash(r.password()));u.setStatus("ACTIVE");u.setRole(UserRole.fromCode(r.role()).code());u.setCreatedAt(java.time.LocalDateTime.now());return UserView.from(users.save(u));}
    @Transactional public UserView updateUser(Long id,UserUpdateRequest r){UserAccount u=user(id);if(r.fullName()!=null)u.setFullName(r.fullName());if(r.phone()!=null)u.setPhone(r.phone());if(r.status()!=null)u.setStatus(r.status().toUpperCase());if(r.role()!=null)u.setRole(UserRole.fromCode(r.role()).code());if(r.newPassword()!=null&&!r.newPassword().isBlank())u.setPasswordHash(passwords.hash(r.newPassword()));return UserView.from(users.save(u));}
    @Transactional public void deleteUser(Long id){users.delete(user(id));}
    @Transactional(readOnly=true) public List<SystemSetting> settings(){return settings.findAll();}
    @Transactional public SystemSetting saveSetting(String key,SettingRequest r){if(key==null||key.isBlank()||r==null)throw new BadRequestException("key and request are required");SystemSetting s=settings.findById(key).orElseGet(SystemSetting::new);s.setKey(key);s.setValue(r.value());s.setDescription(r.description());return settings.save(s);}
    @Transactional public void deleteSetting(String key){settings.deleteById(key);}
    @Transactional(readOnly=true) public List<SystemDevice> devices(String type){return type==null?devices.findAll():devices.findByDeviceTypeIgnoreCase(type);}
    @Transactional public SystemDevice saveDevice(Long id,DeviceRequest r){if(r==null||r.deviceCode()==null||r.deviceType()==null)throw new BadRequestException("deviceCode and deviceType are required");SystemDevice d=id==null?new SystemDevice():devices.findById(id).orElseThrow(()->new ResourceNotFoundException("Device not found: "+id));d.setDeviceCode(r.deviceCode());d.setDeviceType(r.deviceType().toUpperCase());d.setLaneCode(r.laneCode());d.setStatus(r.status()==null?"ACTIVE":r.status().toUpperCase());d.setConfigurationJson(r.configurationJson());return devices.save(d);}
    @Transactional public void deleteDevice(Long id){devices.deleteById(id);}
    private UserAccount user(Long id){return users.findById(id).orElseThrow(()->new ResourceNotFoundException("User not found: "+id));}
}
