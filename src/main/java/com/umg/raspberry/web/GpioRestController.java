package com.umg.raspberry.web;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerAnalog;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bowpi GT
 * Created by Jose on 5/04/2018.
 */
@RestController
@RequestMapping("/rasp")
public class GpioRestController {

    private final Logger logger = LoggerFactory.getLogger(GpioRestController.class);

    private final GpioController controller;

    @Value("${rasp.valid.pins}")
    private String[] pinsConfig;
    private Map<String,Integer> validPins;
    private Map<String,GpioPinDigitalOutput> activePins;

    public GpioRestController() {
        controller = GpioFactory.getInstance();
    }


    @PostConstruct
    public void loadPins(){
        validPins = new HashMap<>();
        activePins = new HashMap<>();
        for(String pin : pinsConfig){
            logger.info("Init pin {}", pin);
            validPins.put(pin,Integer.parseInt(pin));
        }

        logger.info("Loaded pins {}", validPins.toString());
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

}
