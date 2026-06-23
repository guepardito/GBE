module es.guepardito.gbe {
    requires javafx.controls;
    requires java.compiler;

    exports es.guepardito.gbe;

    opens es.guepardito.gbe.cpu to org.junit.platform.commons;
}