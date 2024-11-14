package model;

import controller.Controller;
import utils.OutputEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Personal_ID {
    public final String ID_number;
    public final PublicProfile publicProfile;
    public final String name;
    public final String surname;
    public final int birthdate_day;
    public final int birthdate_month;
    public final int birthdate_year;
    public final String address;
    public final String[] dynamicAttributesValues;
    public final String personalImagePath;
    public final String handSignaturePath;

    public Personal_ID(String pIDnumber, PublicProfile pPublicProfile, String pName, String pSurname, Date pBirthDate,
                       String pAddress, String[] pDynamicAttributesValues, String pPersonalImagePath, String pHandSignaturePath) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(pBirthDate);
        ID_number = pIDnumber;
        publicProfile = pPublicProfile;
        name = pName;
        surname = pSurname;
        birthdate_day = calendar.get(Calendar.DAY_OF_MONTH);
        birthdate_month = calendar.get(Calendar.MONTH) + 1;
        birthdate_year = calendar.get(Calendar.YEAR);
        address = pAddress;
        dynamicAttributesValues = pDynamicAttributesValues;
        personalImagePath = pPersonalImagePath;
        handSignaturePath = pHandSignaturePath;
    }

    public static Personal_ID fromString(Controller controller, int own_or_imported_profile, String[] attributes) throws Exception {
        String ID_number = attributes[0];
        PublicProfile publicProfile = null;
        if (own_or_imported_profile == Controller.LOAD_PROFILE_FROM_OWN) {
            publicProfile = PrivateProfile.loadInternal(controller, controller.appDataLocation + "MyPublicProfiles/", attributes[1]);
        } else if(own_or_imported_profile == Controller.LOAD_PROFILE_FROM_IMPORTED) {
            publicProfile = PublicProfile.loadInternal(controller, controller.appDataLocation + "ImportedPublicProfiles/", attributes[1]);
        }
        if(publicProfile == null) {
            return null;
        }
        String name = attributes[2];
        String surname = attributes[3];
        int birthdate_day = Integer.parseInt(attributes[4]);
        int birthdate_month = Integer.parseInt(attributes[5]);
        int birthdate_year = Integer.parseInt(attributes[6]);
        String address = attributes[7];

        int nDynamicAttributes = publicProfile.dynamicAttributes.length;
        if(attributes.length != 10 + nDynamicAttributes) {
            controller.notifyObservers(new OutputEvent.DynamicAttributesDoesntFitEvent(nDynamicAttributes));
            return null;
        }

        String[] dynamicAttributesValues = new String[nDynamicAttributes];
        for (int i = 0; i < nDynamicAttributes; i++) {
            dynamicAttributesValues[i] = attributes[8 + i];
        }

        String personalImagePath = attributes[8 + nDynamicAttributes];
        String handSignaturePath = attributes[9 + nDynamicAttributes];
        Date birtdate = new SimpleDateFormat("dd.MM.yyyy").parse(birthdate_day + "." + birthdate_month + "." + birthdate_year);
        return new Personal_ID(ID_number, publicProfile, name, surname, birtdate, address, dynamicAttributesValues, personalImagePath, handSignaturePath);
    }

    public byte[] toByte(boolean withPaths) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ID_number.getBytes());
        baos.write('\n');
        baos.write(publicProfile.name.getBytes());
        baos.write('\n');
        baos.write(name.getBytes());
        baos.write('\n');
        baos.write(surname.getBytes());
        baos.write('\n');
        baos.write(Integer.toString(birthdate_day).getBytes());
        baos.write('\n');
        baos.write(Integer.toString(birthdate_month).getBytes());
        baos.write('\n');
        baos.write(Integer.toString(birthdate_year).getBytes());
        baos.write('\n');
        baos.write(address.getBytes());
        baos.write('\n');

        for (String attribute : dynamicAttributesValues) {
            baos.write(attribute.getBytes());
            baos.write('\n');
        }

        if(withPaths) {
            baos.write(personalImagePath.getBytes());
            baos.write('\n');
            baos.write(handSignaturePath.getBytes());
            baos.write('\n');
        }
        return baos.toByteArray();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ausweisnummer:\n");
        sb.append(ID_number);
        sb.append("\nÖffentliches Profil:\n");
        sb.append(publicProfile.name);
        sb.append("\nVorname:\n");
        sb.append(name);
        sb.append("\nNachname:\n");
        sb.append(surname);
        sb.append("\nGeburtsdatum\n");
        sb.append(birthdate_day);
        sb.append(".");
        sb.append(birthdate_month);
        sb.append(".");
        sb.append(birthdate_year);
        sb.append("\nAdresse:\n");
        sb.append(address);

        for (int i = 0; i < publicProfile.dynamicAttributes.length; i++) {
            sb.append('\n');
            sb.append(publicProfile.dynamicAttributes[i]);
            sb.append(":\n");
            sb.append(dynamicAttributesValues[i]);
        }

        sb.append("\nPfad Passbild:\n");
        sb.append(personalImagePath);
        sb.append("\nPfad händische Signatur:\n");
        sb.append(handSignaturePath);
        sb.append('\n');
        return sb.toString();
    }
}
