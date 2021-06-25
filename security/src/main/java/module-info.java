module security {
    requires java.desktop;
    requires com.miglayout.swing;
    requires images;
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;

    opens com.udacity.catpoint.data to com.google.gson;

}