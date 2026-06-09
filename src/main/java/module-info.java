module es.guepardito.gbe {
    requires javafx.controls;

    exports es.guepardito.gbe;

    opens es.guepardito.gbe.cpu to org.junit.platform.commons;
}