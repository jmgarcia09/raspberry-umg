package com.umg.raspberry.web;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.umg.raspberry.RaspberryPin;
import com.umg.raspberry.manager.CarWashManager;
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

/**
 * Bowpi GT
 * Created by Jose on 5/04/2018.
 */
@RestController
@RequestMapping("/rasp")
public class GpioRestController {

    private final Logger logger = LoggerFactory.getLogger(GpioRestController.class);

    private final CarWashManager carWashManager;

    public GpioRestController(CarWashManager carWashManager) {
        this.carWashManager = carWashManager;
    }

    @GetMapping("/{gpioEntry}/toggle")
    private String toggleGpio(@PathVariable(name = "gpioEntry") String gpioEntry){

        carWashManager.getPin(gpioEntry).toggle();
        return null;
    }

    @GetMapping("/{gpioEntry}/status")
    private int getStatus(@PathVariable(name = "gpioEntry") String gpioEntry){

        return carWashManager.getPin(gpioEntry).getState().getValue();

    }

    @GetMapping("/wash")
    private String toggleGpio() throws InterruptedException {

        carWashManager.startCarWash();
        return null;
    }


//    @GetMapping("/motor/inverse")
//    private String executeMotor() throws InterruptedException {
//
//        executeMotor("off");
//        logger.info("Change movement of motor...");
//        raspberryPins.forEach(raspberryPin -> Collections.reverse(raspberryPin.getPinStates()));
//        executeMotor("on");
//
//        return "Motor reversed";
//    }



}
