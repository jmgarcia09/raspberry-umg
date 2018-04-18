package com.umg.raspberry.web;

import com.pi4j.io.gpio.*;
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

    public GpioRestController() {
        controller = null;
    }


    @PostConstruct
    public void loadPins(){
        validPins = new HashMap<>();
        for(String pin : pinsConfig){
            logger.info("Init pin {}", pin);
            validPins.put(pin,Integer.parseInt(pin));
        }

        logger.info("Loaded pins {}", validPins.toString());
    }


    @GetMapping("/{gpioEntry}/toggle")
    private String toggleGpio(@PathVariable(name = "gpioEntry") String gpioEntry){

        if(validPins.containsKey(gpioEntry)){
            GpioPinDigitalOutput digitalPin = controller.provisionDigitalOutputPin(RaspiPin.getPinByAddress(validPins.get(gpioEntry)));
            if(digitalPin.isHigh()){
                digitalPin.low();
            }else {
                digitalPin.high();
            }
        }else {
            return "Pin not valid, check configuration";
        }

        return null;
    }

    @GetMapping("/valid")
    private String toggleGpio(){

        return pinsConfig.toString();
    }
}
