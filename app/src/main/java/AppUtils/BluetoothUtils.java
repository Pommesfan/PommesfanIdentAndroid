package AppUtils;

import utils.OutputEvent;

public class BluetoothUtils {
    public static class BluetoothServerStartedEvent implements OutputEvent {
        public final String mac;
        public final String password;
        public BluetoothServerStartedEvent(String pMac, String pPassword) {
            mac = pMac;
            password = pPassword;
        }
    }
}
