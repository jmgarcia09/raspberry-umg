package com.umg.raspberry.web;

import com.pi4j.io.gpio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bowpi GT
 * Created by Jose on 5/04/2018.
 */
@RestController
@RequestMapping("/rasp")
public class GpioRestController {

    private final GpioController controller;

    @Value("${rasp.valid.pins}")
    private List<String> validPins;

    public GpioRestController() {
        controller = GpioFactory.getInstance();
    }


    @GetMapping("/{gpioEntry}/toggle")
    private String toggleGpio(@PathVariable(name = "gpioEntry") int gpioEntry){

        if(validPins.contains(gpioEntry)){
            GpioPinDigitalOutput digitalPin = controller.provisionDigitalOutputPin(RaspiPin.getPinByAddress(gpioEntry));
            controller.toggle(digitalPin);

        }else {
            return "Pin not valid, check configuration";
        }

        return null;
    }
}
