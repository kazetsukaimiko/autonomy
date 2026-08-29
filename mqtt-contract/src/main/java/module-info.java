module io.freedriver.autonomy.mqtt.contract {
    requires com.fasterxml.jackson.databind;
    requires jakarta.validation;
    requires static lombok;

    exports io.freedriver.autonomy.mqtt.contract;
    opens io.freedriver.autonomy.mqtt.contract to com.fasterxml.jackson.databind;
}
