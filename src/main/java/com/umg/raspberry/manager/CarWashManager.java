package com.umg.raspberry.manager;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinAnalogValueChangeEvent;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerAnalog;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.umg.raspberry.RaspberryPin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
public class CarWashManager {

    private final Logger logger = LoggerFactory.getLogger(CarWashManager.class);

    private final GpioController gpioController;
    private Map<String,GpioPinDigitalOutput> activePins;
    private List<RaspberryPin> motorPins;

    @Value("${rasp.motor.time}")
    private long motorTime;

    private RaspberryPin initProcessPin;
    private RaspberryPin endProcessPin;
    private RaspberryPin washMotorPin;
    private RaspberryPin waterMotorPin;
    private RaspberryPin airMotorPin;

    private static boolean execute = false;
    private static boolean motorActive = false;


    public CarWashManager() {
        this.gpioController = GpioFactory.getInstance();
    }


    @PostConstruct
    public void loadPins(){
        activePins = new HashMap<>();

        initProcessPin = new RaspberryPin("3");
        endProcessPin = new RaspberryPin("26");

        washMotorPin = new RaspberryPin("12");
        waterMotorPin = new RaspberryPin("10");
        airMotorPin = new RaspberryPin("11");

        loadMotorPins();
        addListeners();
    }

    @PreDestroy
    public void destroy(){
        gpioController.shutdown();
    }

    void loadMotorPins(){
        motorPins = new ArrayList<>();
        motorPins.add(new RaspberryPin("22", Arrays.asList(PinState.HIGH,PinState.HIGH,
                PinState.LOW,PinState.LOW,PinState.LOW,PinState.LOW,PinState.LOW,PinState.HIGH)));
        motorPins.add(new RaspberryPin("23", Arrays.asList(PinState.LOW,PinState.HIGH,
                PinState.HIGH,PinState.HIGH,PinState.LOW,PinState.LOW,PinState.LOW,PinState.LOW)));
        motorPins.add(new RaspberryPin("24", Arrays.asList(PinState.LOW,PinState.LOW,
                PinState.LOW,PinState.HIGH,PinState.HIGH,PinState.HIGH,PinState.LOW,PinState.LOW)));
        motorPins.add(new RaspberryPin("25", Arrays.asList(PinState.LOW,PinState.LOW,
                PinState.LOW,PinState.LOW,PinState.LOW,PinState.HIGH,PinState.HIGH,PinState.HIGH)));
        motorPins.parallelStream().forEach(this::getPin);
    }

    public GpioPinDigitalOutput getPin(RaspberryPin pin){
        if(!activePins.containsKey(pin.getPinNumber())){
            activePins.put(pin.getPinNumber(),gpioController.provisionDigitalOutputPin(RaspiPin.getPinByAddress(Integer.valueOf(pin.getPinNumber()))));
            logger.info("PIN [{}] loaded",pin.getPinNumber());
        }
        return activePins.get(pin.getPinNumber());
    }


    public GpioPinDigitalOutput getPin(String pin){
        if(!activePins.containsKey(pin)){
            activePins.put(pin,gpioController.provisionDigitalOutputPin(RaspiPin.getPinByAddress(Integer.valueOf(pin))));
            logger.info("PIN [{}] loaded",pin);
        }
        return activePins.get(pin);
    }


    public void addListeners(){
        gpioController.provisionDigitalInputPin(RaspiPin.getPinByAddress(Integer.valueOf(initProcessPin.getPinNumber()))).addListener((GpioPinListenerDigital) gpioPinDigitalStateChangeEvent -> {
            logger.info("Pin status");
            if(gpioPinDigitalStateChangeEvent.getState().isHigh() && !execute){

                logger.info("Starting carwash process", gpioPinDigitalStateChangeEvent.getPin().getName());

                try {
                    startCarWash();
                    execute = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });


        gpioController.provisionDigitalInputPin(RaspiPin.getPinByAddress(Integer.valueOf(endProcessPin.getPinNumber()))).addListener((GpioPinListenerDigital) gpioPinDigitalStateChangeEvent -> {
            if(gpioPinDigitalStateChangeEvent.getState().isHigh() && execute ){
                logger.info("Stop carwash process", gpioPinDigitalStateChangeEvent.getPin().getName());
                try {
                    stopProcess();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });
    }

    public void stopProcess() throws InterruptedException {
        executeMotor(false,0);
        logger.info("Return all pins to LOW state");
        activePins.entrySet().forEach(pin -> pin.getValue().low());
        execute = false;
    }


    public boolean startCarWash() throws InterruptedException {

        if(execute){
            logger.info("Car Wash is already executing...");
            return false;
        }
        execute = true;
        logger.info("Starting CarWash....");
        logger.info("Turning on movement motor");
        executeMotor(true,5000);
        logger.info("Turning off movement motor");
        logger.info("Turning on wash motor");
        turnOnPin(washMotorPin,5000);
        logger.info("Turning off wash motor");
        logger.info("Turning on movement motor");
        executeMotor(true,5000);
        logger.info("Turning off movement motor");
        logger.info("Turning on water motor");
        turnOnPin(waterMotorPin,5000);
        logger.info("Turning off water motor");
        logger.info("Turning on movement motor");
        executeMotor(true,5000);
        logger.info("Turning off movement motor");
        logger.info("Turning on air motor");
        turnOnPin(airMotorPin,5000);
        logger.info("Turning off air motor");

        logger.info("Turning on movement motor until receive end notification");
        executeMotor(true,-1);

        return true;
    }


    private void executeMotor(boolean start, long time) throws InterruptedException {

        if(start){
            if(motorActive){
                logger.info("Motor is already running");
                return;
            }

            motorActive = true;
            logger.info("Turning motor ON");

            ForkJoinPool pool = new ForkJoinPool(4);
            pool.execute(() -> {
                motorPins.parallelStream().forEach(raspberryPin -> {
                    GpioPinDigitalOutput pin = activePins.get(raspberryPin.getPinNumber());
                    while(motorActive){
                        for(PinState state : raspberryPin.getPinStates()){
                            if(!motorActive) break;
                            pin.setState(state);
                            try {
                                Thread.sleep(motorTime);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            });

            if(time > 0){
                Thread.sleep(time);
                executeMotor(false,0);
            }
        }else {
            motorActive = false;
            Thread.sleep(1000);
            logger.info("Turning motor OFF");
            logger.info("Return all pins to low state for motor.");
            motorPins.forEach(raspberryPin -> {
                activePins.get(raspberryPin.getPinNumber()).low();
            });
        }

    }

    private void turnOnPin(RaspberryPin pin, long time) throws InterruptedException {
        getPin(pin).high();

        if(time > 0){
            Thread.sleep(time);
            getPin(pin).low();
        }
    }

    private void turnOffPin(RaspberryPin pin) throws InterruptedException {
        getPin(pin).low();
    }


}
