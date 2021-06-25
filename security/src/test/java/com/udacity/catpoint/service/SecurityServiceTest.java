package com.udacity.catpoint.service;

import com.udacity.catpoint.service.SecurityService;
import com.udacity.catpoint.data.*;
import com.udacity.catpoint.application.StatusListener;

import com.udacity.catpoint.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest <ImageService extends com.images.services.ImageService> {

    /*  SENSOR */
    private Sensor sensor;
    private String sensorTestName;
    /*door , window, motion*/
    private SensorType sensorTestType;
    private Boolean SensortestActive;
    private final String random = UUID.randomUUID().toString();

    private Set<Sensor> getAllSensors(int count, boolean status) {

        Set<Sensor> sensors = new HashSet<>();

        for (int i = 0; i < count; i++) {
            sensors.add(new Sensor(random, SensorType.DOOR));
        }
        sensors.forEach(sensor -> sensor.setActive(status));

        return sensors;
    }

    public Sensor getSensorTest() {
        return new Sensor(sensorTestName,sensorTestType);
    }

    /*  MOCKS */
    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private StatusListener statusListener;

    /*THE SECURITY SERVICE TO TEST !!!*/
    private SecurityService securityService;


    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository,  imageService);
        sensor = getSensorTest();
    }

    @Test
    /*If alarm is armed and a sensor becomes activated, put the system into pending alarm status.*/
    public void alarm_is_armed_and_sensor_becomes_activated_put_the_system_in_pending_alarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    /*If alarm is armed and a sensor becomes activated and the system is already pending alarm,
    set the alarm status to alarm.*/
    public void alarm_is_armed_sensor_becomes_activated_system_already_pending_alarm_set_alarm_to_alarm(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    /*If pending alarm and all sensors are inactive, return to no alarm state.*/
    public void pending_alarm_and_all_sensors_inactive_return_to_no_alarm_state(){
        Set<Sensor> allSensors = getAllSensors(3, false);
        //when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,false);


        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    @Test
    //If alarm is active, change in sensor state should not affect the alarm state.*/
    public void alarm_active_change_sensor_state_should_not_affect_alarm_state(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        Set<Sensor> allSensors = getAllSensors(3, false);

        for(Sensor sensor : allSensors){
            securityService.changeSensorActivationStatus(sensor,true);
        }

        verify(securityRepository,never()).setAlarmStatus(any(AlarmStatus.class));

    }

    @Test
    /*If a sensor is activated while already active and
    the system is in pending state, change it to alarm state.*/
    public void sensor_activated_while_already_active_system_in_pending_state_change_it_to_alarm_state(){

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor,true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    /*If a sensor is deactivated while already inactive, make no changes to the alarm state*/
    public void sensor_deactivated_while_already_inactive_no_changes_to_alarm_state(){
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor,false);


        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    /*If the image service identifies an image containing a cat while the system is armed-home,
     put the system into alarm status.*/
    public void image_service_identifies_image_containing_cat_system_armed_home_put_system_in_alarm_status(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));



        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    /*If the image service identifies an image that does not contain a cat,
    change the status to no alarm as long as the sensors are not active.*/
    public void image_service_with_no_cat_change_status_to_no_alarm_when_sensors_no_active(){
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(false);

        securityService.processImage(mock(BufferedImage.class));

        Set<Sensor> sensors = getAllSensors(3, false);

        for(Sensor sensor : sensors){
            sensor.setActive(false);
        }
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    /*If the system is disarmed, set the status to no alarm*/
    public void system_disarmed_set_status_to_no_alarm(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    /*If the system is armed, reset all sensors to inactive.*/
    public void system_armed_reset_all_sensors_to_inactive(ArmingStatus armingStatus){
        securityRepository.setArmingStatus(armingStatus);

        securityRepository.setAlarmStatus(AlarmStatus.NO_ALARM);
        securityRepository.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        Set<Sensor> sensors = getAllSensors(3, true);

        for(Sensor sensor : sensors){
            sensor.setActive(false);
            securityService.changeSensorActivationStatus( sensor, false);
        }

    }


    @Test
    /*If the system is armed-home while the camera shows a cat, set the alarm status to alarm*/
    public void system_armed_home_camera_shows_cat_set_alarm_to_status_alarm(){

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);

        when(imageService.imageContainsCat(catImage,50.0f)).thenReturn(true);

        securityService.processImage(catImage);


        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    /*Register the StatusListener for alarm system updates from within the SecurityService*/
    public void add_Status_Listener_Test(){
        securityService.addStatusListener(statusListener);
    }

    @Test
    public void remove_Status_Listener_Test(){
        securityService.removeStatusListener(statusListener);
    }

    /*Sensors*/
    @Test
    public void add_sensor_test(){
        securityService.addSensor(sensor);
    }

    @Test
    public void remove_sensor_test(){
        securityService.removeSensor(sensor);
    }



}
