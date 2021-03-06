/**
 * 
 */
package com.davidoyeku.azure.practical.managment_demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.services.servicebus.ServiceBusContract;
import com.microsoft.windowsazure.services.servicebus.models.BrokeredMessage;

/**
 * @author DavidOyeku
 *
 */
public class SpeedCamera {
	private int id;
	private String streetName;
	private String town;
	private int maxSpeed;
	public int getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(int maxSpeed) {
		//on change of max speed, the camera is restarted
		sendStartUpMessage();
		this.maxSpeed = maxSpeed;
	}

	public static final int TYPE=1;
	public static final String SPEED_CAM_SUB = "speed_camera_sub";
	private ArrayList<BrokeredMessage> offlineQueue;
	private ServiceBusContract service;
	private String topic;
	private Date d;

	public SpeedCamera(String[] cameraProperties, ServiceBusContract service, String topic) {
		offlineQueue = new ArrayList<BrokeredMessage>();
		this.id = Integer.parseInt(cameraProperties[0]);
		this.streetName = cameraProperties[1];
		this.town = cameraProperties[2];
		this.maxSpeed = Integer.parseInt(cameraProperties[3]);
		this.service = service;
		this.topic = topic;
		this.d = new Date();
		sendStartUpMessage();
	}

	private void sendStartUpMessage() {
		// TODO Auto-generated method stub
		BrokeredMessage message = new BrokeredMessage(this.toString());
		message.setProperty("id", this.id);
		message.setProperty("town", this.town);
		message.setProperty("streetName", this.streetName);
		message.setProperty("maxSpeed", this.maxSpeed);
		message.setProperty("type", this.TYPE);
		message.setProperty("date", this.d.getTime());
		try {
			if(internetConnection()){
				//send offline messages
				sendOfflineMessages();
				//send speed camera message
				service.sendTopicMessage(topic, message);
				System.out.println("Camera sent start up message "+this.toString());
//				System.out.println("sending ...");
			}else{
				//add to offline queue if no internet
				offlineQueue.add(message);
			}
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "id:"+id +" "+"street:" + streetName + " "+"town:" + town + " "+"max_speed:" + maxSpeed;
	}

	public void recordVehiclePassing() {
		Vehicle vehicle;
			vehicle = new Vehicle();
			vehicle.setCameraId(this.id);
			vehicle.setCameraMaxSpeed(this.maxSpeed);
			sendVehiclePassing(vehicle);
			System.out.println(vehicle);
	}



	private void sendVehiclePassing(Vehicle vehicle) {
		BrokeredMessage message = new BrokeredMessage(vehicle.toString());
		//extracting vehicle properties 
		message.setProperty("regPlate", vehicle.getRegPlate());
		message.setProperty("vehicleType", vehicle.getVehicleType());
		message.setProperty("currentSpeed", vehicle.getCurrentSpeed());
		message.setProperty("cameraId", vehicle.getCameraId());
		message.setProperty("cameraMaxSpeed", vehicle.getCameraMaxSpeed());
		message.setProperty("type", vehicle.TYPE);
		try {
			if(internetConnection()){
				sendOfflineMessages();
				service.sendTopicMessage(topic, message);	
				System.out.println("cameraId "+this.id+" sending to vehicle subscription");
			}else{
				System.out.println("adding to offline queue");
				offlineQueue.add(message);
			}
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendOfflineMessages() throws ServiceException {
		// TODO Auto-generated method stub
		//if the offline queue contains any messages
		if(!offlineQueue.isEmpty()){
			System.out.println(offlineQueue.size());
			for (int i = 0; i < offlineQueue.size(); i++) {
				service.sendTopicMessage(topic, offlineQueue.get(i));
				offlineQueue.remove(i);
				System.out.println("sending offline messages");
			}
		}
	}

	private boolean internetConnection() {
		// TODO Auto-generated method stub
		Socket sock = new Socket();
	    InetSocketAddress addr = new InetSocketAddress("google.com",80);
	    try {
	        sock.connect(addr,3000);
	        return true;
	    } catch (IOException e) {
	        return false;
	    } finally {
	        try {sock.close();}
	        catch (IOException e) {}
	    }
	}
}
