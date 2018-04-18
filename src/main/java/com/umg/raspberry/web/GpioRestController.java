package com.umg.raspberry.web;

import com.pi4j.io.gpio.*;
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

    private final GpioController controller;

    @Value("${rasp.valid.pins}")
    private List<String> pinsConfig;
    private Map<String,Integer> validPins;

    public GpioRestController() {
        controller = GpioFactory.getInstance();
    }


    @PostConstruct
    public void loadPins(){
        validPins = new HashMap<>();
        pinsConfig.forEach(pin -> validPins.put(pin,Integer.parseInt(pin)));
    }


    @GetMapping("/{gpioEntry}/toggle")
    private String toggleGpio(@PathVariable(name = "gpioEntry") String gpioEntry){

        if(validPins.containsKey(gpioEntry)){
            GpioPinDigitalOutput digitalPin = controller.provisionDigitalOutputPin(RaspiPin.getPinByAddress(validPins.get(gpioEntry)));
            controller.toggle(digitalPin);

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
