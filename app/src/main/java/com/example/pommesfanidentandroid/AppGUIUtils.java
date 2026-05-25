package com.example.pommesfanidentandroid;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowMetrics;
import android.widget.ImageView;
import utils.OutputEvent;

import static controller.Controller.*;

public class AppGUIUtils {
    public static String handleMsg(OutputEvent e) {
        if(e instanceof OutputEvent.PersonalIDValidEvent) {
            return "Ausweis ist korrekt\n" + ((OutputEvent.PersonalIDValidEvent) e).personalIDprintout;
        } else if(e instanceof OutputEvent.PersonalIDInvalidEvent){
            return "Ausweis ist nicht korrekt\n";
        } else if (e instanceof OutputEvent.ServerStartedEvent) {
            OutputEvent.ServerStartedEvent evt = (OutputEvent.ServerStartedEvent) e;
            return "IP-Adresse:" + evt.ip + "\nPortnummer: " + evt.port + "\nKrypto-Passwort:" + evt.password;
        } else if (e instanceof OutputEvent.NoSuchProfileEvent) {
            OutputEvent.NoSuchProfileEvent evt = (OutputEvent.NoSuchProfileEvent) e;
            if(evt.namePresent)
                return "Profil mit der Sequenznummer " + evt.sequence_number + " nicht gespeichert, aber Profilname " + evt.name + " gespeichert";
            else {
                return "Profil mit dem Profilnamen " + evt.name + " nicht gespeichert";
            }
        } else if (e instanceof OutputEvent.DynamicAttributesDoesntFitEvent) {
            return "Anzahl dynamischer Attribute unpassend: Profil hat " + ((OutputEvent.DynamicAttributesDoesntFitEvent) e).nDynamicAttributes + " Attribute";
        } else if (e instanceof OutputEvent.ShowProfileEvent) {
            return ((OutputEvent.ShowProfileEvent) e).msg;
        } else if (e instanceof  OutputEvent.ProfileAlreadyExistsEvent) {
            return "Profil mit diesem Namen sowie Folgenummer bereits gespeichert";
        } else if (e instanceof OutputEvent.IDalreadyExistsEvent) {
            return "Ausweis mit dieser Ausweisnummer bereits gespeichert";
        } else if (e instanceof OutputEvent.InvalidDateEvent) {
            return "Fehlerhafte Datumsangabe";
        } else if (e instanceof OutputEvent.InvalidDateSequenceEvent) {
            return "Reihenfolge der Datumsangaben für Profil ungültig";
        } else if (e instanceof OutputEvent.PersonalIDoutOfValidityPeriodEvent) {
            return "Gültigkeitsdatum von Ausweis passt nicht zu Profil";
        } else if (e instanceof OutputEvent.NoSuchPersonalIDevent) {
            return "Ausweis mit der Nummer: " + ((OutputEvent.NoSuchPersonalIDevent) e).idNumber + " nicht gespeichert";
        } else if (e instanceof OutputEvent.PersonalIDoutdatedEvent) {
            return "Ausweis mit der Nummer: " + ((OutputEvent.PersonalIDoutdatedEvent) e).idNumber + " abgelaufen";
        } else if(e instanceof OutputEvent.CryptoPasswordInvalidEvent) {
            return "Krypto-Passwort ungültig";
        } else if (e instanceof OutputEvent.FileNotFromHereEvent) {
            return "Diese Datei ist nicht von diesem Programm";
        } else if (e instanceof OutputEvent.WrongFileTypeEvent) {
            OutputEvent.WrongFileTypeEvent evt = (OutputEvent.WrongFileTypeEvent) e;
            String msg = "Datei beinhaltet ";
            if(evt.type == FILE_TYPE_PRIVATE_PROFILE)
                msg += "ein privates Profil\n";
            else if(evt.type == FILE_TYPE_PUBLIC_PROFILE)
                msg += "ein öffentliches Profil\n";
            else if (evt.type == FILE_TYPE_ID) {
                msg += "einen Ausweis\n";
            } else {
                throw new RuntimeException("No such FileType");
            }
            return msg;
        } else if(e instanceof OutputEvent.IDaggregatedEvent) {
            return "Diesem Profil ist noch mindestens ein Ausweis zugeordnet";
        }
        return "";
    }

    public static String nameFromURL(String url) {
        String[]splittedUrl = url.split("/");
        return splittedUrl[splittedUrl.length - 1];
    }

    public static void bytesToImageView(Activity activity, byte[]image, ImageView view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        // https://stackoverflow.com/questions/13854742/byte-array-of-image-into-imageview
        Bitmap bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
        int width = displayMetrics.widthPixels;
        int height = width / bmp.getWidth() * bmp.getHeight();
        view.setImageBitmap(Bitmap.createScaledBitmap(bmp, width, height, false));
    }
}
