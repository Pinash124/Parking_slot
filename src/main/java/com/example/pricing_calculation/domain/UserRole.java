package com.example.pricing_calculation.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum UserRole {

    ADMINISTRATOR(
            "ADMINISTRATOR",
            "System Administrator",
            "Quan tri he thong, phan quyen, audit log, sao luu va cau hinh van hanh.",
            List.of(
                    "Quan ly tai khoan va phan quyen truy cap theo role",
                    "Xem audit log va trang thai sao luu/phuc hoi",
                    "Quan ly cau hinh he thong va tham so van hanh"
            )
    ),

    PARKING_MANAGER(
            "PARKING_MANAGER",
            "Parking Manager",
            "Quan ly van hanh bai xe, bang gia, bao cao va thong tin suc chua.",
            List.of(
                    "Xem thong tin bai xe, dashboard va bao cao van hanh",
                    "Quan ly suc chua, khu vuc, slot va bang gia",
                    "Theo doi ngoai le nhu mat ve, sai phi, sai khu vuc va xe qua gio"
            )
    ),

    PARKING_STAFF(
            "PARKING_STAFF",
            "Parking Staff",
            "Nhan vien xu ly xe vao/ra, scan bien so va ho tro su co tai bai xe.",
            List.of(
                    "Check-in, tao ticket va huong dan xe vao khu vuc phu hop",
                    "Checkout, tinh phi, xac nhan thanh toan va validate exit",
                    "Ghi nhan scan bien so, su co va ho tro khach hang"
            )
    ),

    PARKING_USER(
            "PARKING_USER",
            "Parking User / Driver",
            "Nguoi dung gui xe, dat cho, theo doi luot gui xe va thanh toan phi gui xe.",
            List.of(
                    "Xem thong tin bai xe: thoi gian hoat dong, loai xe duoc phuc vu, bang gia, quy dinh gui xe va so slot trong",
                    "Gui xe theo luot: nhan the xe hoac ma gui xe khi vao bai va thanh toan phi khi ra",
                    "Dat cho truoc theo loai phuong tien, thoi gian gui va khu vuc con trong neu he thong ho tro",
                    "Theo doi luot gui xe hien tai: gio vao, loai xe, khu vuc gui va phi tam tinh",
                    "Thanh toan phi gui xe va dich vu bo sung neu co",
                    "Gui phan hoi ve mat the xe, sai phi, kho tim xe, slot bi chiem hoac van de trong bai xe"
            )
    );

    private final String code;
    private final String displayName;
    private final String description;
    private final List<String> capabilities;

    UserRole(String code, String displayName, String description, List<String> capabilities) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.capabilities = List.copyOf(capabilities);
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public List<String> capabilities() {
        return capabilities;
    }

    public static UserRole fromCode(String code) {
        String normalized = normalize(code);
        if ("CUSTOMER".equals(normalized) || "DRIVER".equals(normalized) || "USER".equals(normalized)) {
            return PARKING_USER;
        }
        if ("ADMIN".equals(normalized)) {
            return ADMINISTRATOR;
        }
        if ("MANAGER".equals(normalized)) {
            return PARKING_MANAGER;
        }
        if ("STAFF".equals(normalized)) {
            return PARKING_STAFF;
        }
        return Arrays.stream(values())
                .filter(role -> role.code.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported user role: " + code));
    }

    private static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }
}
