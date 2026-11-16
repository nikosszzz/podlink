package dev.podlink.system;

interface IShizukuBatteryBridge {
    String pushBatteryState(
        String macAddress,
        int leftLevel,
        boolean leftCharging,
        int rightLevel,
        boolean rightCharging,
        int caseLevel,
        boolean caseCharging,
        int headsetLevel,
        boolean headsetCharging,
        String modelName,
        String mainIconUri,
        String leftIconUri,
        String rightIconUri,
        String caseIconUri,
        String companionPackage
    );
    void destroy();
}
