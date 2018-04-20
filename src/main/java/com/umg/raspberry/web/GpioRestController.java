package com.umg.raspberry.web;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerAnalog;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.umg.raspberry.MotorPin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * Bowpi GT
 * Created by Jose on 5/04/2018.
 */
@RestController
@RequestMapping("/rasp")
public class GpioRestController {

    private final Logger logger = LoggerFactory.getLogger(GpioRestController.class);
    private static boolean motorActive = false;
    private static boolean rightMovement = true;

    private GpioController controller;

    @Value("${rasp.valid.pins}")
    private String[] pinsConfig;
    @Value("${rasp.motor.time}")
    private long motorTime;
    private Map<String,Integer> validPins;
    private Map<String,GpioPinDigitalOutput> activePins;
    private List<MotorPin> motorPins;

    public GpioRestController() {
        controller = GpioFactory.getInstance();
    }


    @PostConstruct
    public void loadPins(){
        controller = GpioFactory.getInstance();
        validPins = new HashMap<>();
        activePins = new HashMap<>();
        for(String pin : pinsConfig){
            logger.info("Init pin {}", pin);
            validPins.put(pin,Integer.parseInt(pin));
        }
        logger.info("Loaded pins {}", validPins.toString());

        motorPins = new ArrayList<>();
        motorPins.add(new MotorPin("22", Arrays.asList(PinState.HIGH,PinState.HIGH,
                PinState.LOW,PinState.LOW,PinState.LOW,PinState.LOW,PinState.LOW,PinState.HIGH)));

        motorPins.add(new MotorPin("23", Arrays.asList(PinState.LOW,PinState.HIGH,
                PinState.HIGH,PinState.HIGH,PinState.LOW,PinState.LOW,PinState.LOW,PinState.LOW)));

        motorPins.add(new MotorPin("24", Arrays.asList(PinState.LOW,PinState.LOW,
                PinState.LOW,PinState.HIGH,PinState.HIGH,PinState.HIGH,PinState.LOW,PinState.LOW)));

        motorPins.add(new MotorPin("25", Arrays.asList(PinState.LOW,PinState.LOW,
                PinState.LOW,PinState.LOW,PinState.LOW,PinState.HIGH,PinState.HIGH,PinState.HIGH)));

        motorPins
                .parallelStream()
                .forEach(
                motorPin -> activePins.put(motorPin.getPinNumber(),
                        controller.provisionDigitalOutputPin(RaspiPin.getPinByAddress(Integer.valueOf(motorPin.getPinNumber())))));
    }


    @GetMapping("/{gpioEntry}/toggle")
    private String toggleGpio(@PathVariable(name = "gpioEntry") String gpioEntry){

        if(validPins.containsKey(gpioEntry)){
            GpioPinDigitalOutput digitalPin;
            if(!activePins.containsKey(gpioEntry)){
                activePins.put(gpioEntry,controller.provisionDigitalOutputPin(RaspiPin.getPinByAddress(validPins.get(gpioEntry))));

            }
            digitalPin = activePins.get(gpioEntry);
            digitalPin.toggle();
        }else {
            return "Pin not valid, check configuration";
        }

        return null;
    }


    @GetMapping("/{gpioEntry}/listen")
    private String listenGpio(@PathVariable(name = "gpioEntry") String gpioEntry){

        if(validPins.containsKey(gpioEntry)){
            GpioPinDigitalOutput digitalPin;
            if(!activePins.containsKey(gpioEntry)){
                activePins.put(gpioEntry,controller.provisionDigitalOutputPin(RaspiPin.getPinByAddress(validPins.get(gpioEntry))));

            }
            digitalPin = activePins.get(gpioEntry);
            digitalPin.setShutdownOptions(true);
            // create and register gpio pin listener
            digitalPin.addListener(new GpioPinListenerDigital() {


                @Override
                public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {

                    if(PinState.HIGH == event.getState()){
                        logger.info("Se activo pin {}", event.getPin());

                    }
                }

            });
        }else {
            return "Pin not valid, check configuration";
        }

        return null;
    }

    @GetMapping("/motor/inverse")
    private String executeMotor() throws InterruptedException {

        executeMotor("off");
        logger.info("Change movement of motor...");
        motorPins.forEach(motorPin -> Collections.reverse(motorPin.getPinStates()));
        executeMotor("on");

        return "Motor reversed";
    }

    @GetMapping("/motor/{action}")
    private String executeMotor(@PathVariable(name = "action") String action) throws InterruptedException {



        if("on".equalsIgnoreCase(action)){
            if(motorActive){
                logger.info("Motor is already running");
                return "Motor is already running";
            }

            motorActive = true;
            logger.info("Turning motor ON");

            ForkJoinPool pool = new ForkJoinPool(4);

            pool.execute(() -> {
                motorPins.parallelStream().forEach(motorPin -> {
                    GpioPinDigitalOutput pin = activePins.get(motorPin.getPinNumber());
                    while(motorActive){
                        for(PinState state : motorPin.getPinStates()){
                            if(!motorActive) break;
                            pin.setState(state);
                            logger.info("Putting state {} to pin {}", state.getValue(),pin.getName());
                            try {
                                Thread.sleep(motorTime);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                });
            });
            return "Executing motor.";
        }else {
            motorActive = false;
            Thread.sleep(1000);
            logger.info("Turning motor OFF");
            logger.info("Return all pins to low state for motor.");
            motorPins.forEach(motorPin -> {
                activePins.get(motorPin.getPinNumber()).low();
            });
            return "Motor turning off";
        }

    }

}
