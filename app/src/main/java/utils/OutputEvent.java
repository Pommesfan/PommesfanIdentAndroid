package utils;

public interface OutputEvent {

    class PersonalIDValidEvent implements OutputEvent {
        public final String personalIDprintout;
        public PersonalIDValidEvent(String pPersonalIDprintout) {
            personalIDprintout = pPersonalIDprintout;
        }
    }

    class PersonalIDInvalidEvent implements OutputEvent {
        public PersonalIDInvalidEvent() {}
    }

    class ServerStartedEvent implements OutputEvent {
        public final String ip;
        public final int port;
        public ServerStartedEvent(String pIP, int pPort) {
            ip = pIP;
            port = pPort;
        }
    }

    class NoSuchPublicProfileEvent implements OutputEvent{
        public final String name;
        public NoSuchPublicProfileEvent(String name) {
            this.name = name;
        }
    }

    class DynamicAttributesDoesntFitEvent implements OutputEvent{
        public final int nDynamicAttributes;
        public DynamicAttributesDoesntFitEvent(int nDynamicAttributes) {
            this.nDynamicAttributes = nDynamicAttributes;
        }
    }
}
