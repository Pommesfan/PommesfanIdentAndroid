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

    class NetworkServerStartedEvent implements OutputEvent {
        public final String ip;
        public final int port;
        public final String password;
        public NetworkServerStartedEvent(String pIP, int pPort, String pPassword) {
            ip = pIP;
            port = pPort;
            password = pPassword;
        }
    }

    class NoSuchProfileEvent implements OutputEvent{
        public final String name;
        public final int sequence_number;
        public final boolean namePresent;
        public NoSuchProfileEvent(String name, int sequence_number, boolean namePresent) {
            this.name = name;
            this.sequence_number = sequence_number;
            this.namePresent = namePresent;
        }
    }

    class DynamicAttributesDoesntFitEvent implements OutputEvent{
        public final int nDynamicAttributes;
        public DynamicAttributesDoesntFitEvent(int nDynamicAttributes) {
            this.nDynamicAttributes = nDynamicAttributes;
        }
    }

    class ShowProfileEvent implements OutputEvent{
        public final String msg;
        public ShowProfileEvent(String msg) {
            this.msg = msg;
        }
    }

    class ProfileAlreadyExistsEvent implements OutputEvent{}

    class IDalreadyExistsEvent implements OutputEvent {}

    class InvalidDateEvent implements OutputEvent {}

    class InvalidDateSequenceEvent implements OutputEvent {}

    class PersonalIDoutOfValidityPeriodEvent implements OutputEvent {}

    class NoSuchPersonalIDevent implements OutputEvent{
        public final String idNumber;
        public NoSuchPersonalIDevent(String idNumber) {
            this.idNumber = idNumber;
        }
    }

    class PersonalIDoutdatedEvent implements OutputEvent {
        public final String idNumber;
        public PersonalIDoutdatedEvent(String idNumber) {
            this.idNumber = idNumber;
        }
    }

    class CryptoPasswordInvalidEvent implements OutputEvent {}

    class FileNotFromHereEvent implements OutputEvent {}

    class WrongFileTypeEvent implements OutputEvent {
        public final int type;
        public WrongFileTypeEvent(int type) {
            this.type = type;
        }
    }

    class WrongConnectionPurposeTypeEvent implements OutputEvent {
        public final int type;
        public WrongConnectionPurposeTypeEvent(int type) {
            this.type = type;
        }
    }

    class CheckIDcancelled implements OutputEvent {}

    class IDhandedInSuccessEvent implements OutputEvent {}

    class IDaggregatedEvent implements OutputEvent {}

    class DummyEvent implements OutputEvent {}

    class OtherProfileFoundEvent implements OutputEvent {}
    class CreationSuccessEvent implements OutputEvent {}
}
