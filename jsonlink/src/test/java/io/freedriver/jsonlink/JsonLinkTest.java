package io.freedriver.jsonlink;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.freedriver.util.ProcessUtil;
import jssc.SerialPortException;
import jssc.SerialPortList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.print.attribute.standard.PrinterMoreInfoManufacturer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// Requires Arduino
@Disabled
public class JsonLinkTest implements ExecutionCondition {

    private static final String jsonFile = "/home/luna/mappings.json";

    private boolean enabled = false;
    private Connector connector;
    private ExecutorService executorService;
    private ObjectMapper objectMapper;


    List<Integer> badPinsToSetAsDigital = Stream.of(28)
            .collect(Collectors.toList());

    //@Test
    void testControl() throws IOException, InterruptedException, ExecutionException {
        List<PinMapping> pinMappings = readPinMappings();
        initializePins(pinMappings);

        boolean keepGoing = true;
        while (keepGoing) {
            mappingsMenu(pinMappings);
            keepGoing = yesNo("Set more appliances?").get();
        }

    }

    //@Test
    void testJsonLink() throws IOException, InterruptedException {
        Map<String, String> mappings = IntStream.range(27, 100)
                .filter(this::goodPinsOnly)
                .filter(this::testPinAsDigital)
                .mapToObj(this::askForNameForPin)
                .collect(Collectors.toMap(
                        pinMapping -> String.valueOf(pinMapping.getPinNumber()),
                        PinMapping::getName
                ));

        objectMapper.writeValue(Paths.get(jsonFile).toFile(), mappings);
    }

    boolean goodPinsOnly(Integer pinNumber) {
        return !badPinsToSetAsDigital.contains(pinNumber) && pinNumber < 54;
    }


    List<PinMapping> readPinMappings() throws IOException {
        Map<String, String> mappings = objectMapper.readValue(Paths.get(jsonFile).toFile(), new TypeReference<HashMap<String, String>>(){});
        List<PinMapping> pinMappings = new ArrayList<>();
        mappings.forEach((k, v) -> pinMappings.add(new PinMapping(Integer.parseInt(k), v)));
        return pinMappings;
    }

    void initializePins(List<PinMapping> pinMappings) throws ConnectorException {
        Request r = new Request();
        pinMappings.forEach(pinMapping -> r.modeSet(PinNumber.of(pinMapping.getPinNumber()).setMode(Mode.OUTPUT)));
        connector.send(r);
    }

    Response checkDigitalPins(List<PinNumber> pinNumbers) throws ConnectorException {
        Request r = new Request();
        pinNumbers.forEach(r::digitalRead);
        return connector.send(r);
    }

    boolean mappingsMenu(List<PinMapping> pinMappings) throws IOException, InterruptedException {
        // kdialog --checklist "Select" 1 A off 2 B off
        //List<PinMapping> pinMappings = readPinMappings();
        //initializePins(pinMappings);
        Response r = checkDigitalPins(pinMappings.stream().mapToInt(PinMapping::getPinNumber).mapToObj(PinNumber::of).collect(Collectors.toList()));
        List<String> arguments = Stream.concat(
                Stream.of("kdialog", "--checklist", "Select light status", "--geometry", "500x900"),
                r.getDigital().entrySet()
                    .stream()
                    .flatMap(entry -> Stream.of(
                            String.valueOf(entry.getKey().getPin()),
                            pinMappings.stream()
                                    .filter(pinMapping -> Objects.equals(pinMapping.getPinNumber(), entry.getKey().getPin()))
                                    .map(PinMapping::getName)
                                    .findFirst()
                                    .orElse("null"),
                            (r.getDigital().get(entry.getKey()) ? "off" : "on")
                    )))
                .collect(Collectors.toList());

        Process p = new ProcessBuilder(arguments.toArray(new String[]{}))
                .start();
        int returnCode = p.waitFor();

        String returnedPins = ProcessUtil.readFromInputStream(p.getInputStream());

        List<PinNumber> setToOn = Stream.of(returnedPins.split("\\s+"))
                .map(quoted -> quoted.replace("\"", ""))
                .filter(unquoted -> !unquoted.isEmpty())
                .map(Integer::parseInt)
                .map(PinNumber::of)
                .collect(Collectors.toList());
        List<PinNumber> setToOff = pinMappings.stream()
                .map(PinMapping::getPinNumber)
                .map(PinNumber::of)
                .filter(pinNumber -> !setToOn.contains(pinNumber))
                .collect(Collectors.toList());

        connector.send(new Request().turnOn(setToOn.stream()).turnOff(setToOff.stream()));
        return returnCode == 0;
    }

    boolean interactiveYesNo(String input) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(
                "kdialog",
                "--yesno",
                input
        ).start();
        switch (p.waitFor()) {
            case 0:
                return true;
            case 1:
                return false;
            default:
                throw new RuntimeException(ProcessUtil.readFromInputStream(p.getErrorStream()));
        }
    }

    Future<String> textInput(final String prompt) {
        return executorService.submit(() -> interactiveTextInput(prompt));
    }

    Future<Boolean> yesNo(final String prompt) {
        return executorService.submit(() -> interactiveYesNo(prompt));
    }

    String interactiveTextInput(String prompt) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(
                "kdialog",
                "--inputbox",
                prompt
        ).start();
        switch (p.waitFor()) {
            case 0:
                return ProcessUtil.readFromInputStream(p.getInputStream());
            case 1:
                return null;
            default:
                throw new RuntimeException(ProcessUtil.readFromInputStream(p.getErrorStream()));
        }
    }

    PinMapping askForNameForPin(int pinNumber) {
        try {
            return new PinMapping(pinNumber, interactiveTextInput("Which light was flashing?"));
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get pin name:", e);
        }
    }

    boolean testPinAsDigital(final int pinNumber) {
        boolean state = true;
        try {
            connector.send(new Request()
                    .modeSet(PinNumber.of(pinNumber).setMode(Mode.OUTPUT))
                    .digitalWrite(PinNumber.of(pinNumber).setDigital(state)));
            Future<Boolean> lightFlashing = yesNo("Testing pin number "+pinNumber+": Is a light flashing?");
            while (!lightFlashing.isDone()) {
                //Thread.sleep(150);
                state = !state;
                connector.send(new Request()
                        .digitalWrite(PinNumber.of(pinNumber).setDigital(state)));
            }

            connector.send(new Request()
                    .digitalWrite(PinNumber.of(pinNumber).setDigital(true)));
            return lightFlashing.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to test pin as digital: ", e);
        }
    }


    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        System.out.println("Eval");
        Stream.of(SerialPortList.getPortNames())
                .forEach(System.out::println);
        //enabled = serialPortCount > 0;
        return SerialPortList.getPortNames().length > 0 ?
                ConditionEvaluationResult.enabled("Serial ports present")
                :
                ConditionEvaluationResult.disabled("No Serial ports to test against");
    }
}
