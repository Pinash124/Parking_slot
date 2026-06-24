package com.example.pricing_calculation.authtest;

record AuthFlowResult(
        String email,
        boolean successful,
        long durationMillis,
        int registrationStatus,
        int invalidLoginStatus,
        int loginStatus,
        int logoutStatus,
        int reusedTokenStatus,
        String error
) {
}
