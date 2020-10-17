/**
 *  Shark IQ Robot
 *
 *  Copyright 2020 Chris Stevens
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 *    2020-02-13  Chris Stevens  Original Creation
 *    2020-10-16  Chris Stevens  Initial 'Public' Release
 *    2020-10-17  Chris Stevens  Revision for newer AlyaNetworks API endpoints - Support for Multi Devices (Just create Multiple Drivers) - Spoof iOS or Android Devices when making API calls.
 *
 */

import groovy.json.*
import java.util.regex.*

metadata {
    definition (name: "Shark IQ Robot", namespace: "cstevens", author: "Chris Stevens") {
        capability "Switch"
        capability "Momentary"
    }
 
    preferences {
        input(name: "loginusername", type: "string", title:"Email", description: "Shark Account Email Address", required: true, displayDuringSetup: true)
        input(name: "loginpassword", type: "password", title:"Password", description: "Shark Account Password", required: true, displayDuringSetup: true)
        input(name: "sharkdevicename", type: "string", title:"Device Name", description: "Name you've given your Shark Device within the App", required: true, displayDuringSetup: true)
        input(name: "mobiletype", type: "enum", title:"Mobile Device", description: "Type of Mobile Device your Shark is setup on", required: true, displayDuringSetup: true, options:["Apple iOS", "Android OS"])
    }
}
 
def parse(String description) {
    log.debug(description)
}
 
def push(String action, String operation, Integer operationValue) {
    def authtoken = ''
    def uuid = ''
    def dsnForDevice = ''
    //toggle the switch to generate events for anything that is subscribed
    sendEvent(name: "switch", value: "on", isStateChange: true)
    runIn(1, toggleOff)
    //sendEvent(name: "switch", value: "off", isStateChange: true)
    login()
    getDevices()
    getUserProfile()
    runCmd(action, operation, operationValue)
}
 
def toggleOff() {
    sendEvent(name: "switch", value: "off", isStateChange: true)
}
 
def on() {
    push("on", "SET_Operating_Mode", 2)
}
 
def off() {
    push("off", "SET_Operating_Mode", 3)
}

def login() {
    def localDevicePort = (devicePort==null) ? "80" : devicePort
    def app_id = ""
    def app_secret = ""
    if (mobiletype == "Apple iOS") {
        app_id = "Shark-iOS-field-id"
        app_secret = "Shark-iOS-field-_wW7SiwgrHN8dpU_ugCattOoDk8"
    }
    else if (mobiletype == "Android OS") {
        app_id = "Shark-Android-field-id"
        app_secret = "Shark-Android-field-Wv43MbdXRM297HUHotqe6lU1n-w"
    }
	def body = """{"user":{"email":"$loginusername","application":{"app_id":"$app_id","app_secret":"$app_secret"},"password":"$loginpassword"}}"""
    
    //log.info body
	def params = [
        uri: "https://ads-field.aylanetworks.com",
		path: "/users/sign_in.json",
        requestContentType: "application/json",
        headers: ["Content-Type": "application/json", "Accept": "*/*"],
        body: "$body"
    ]
    try {
    httpPost(params) { response ->
        
        if(response.getStatus() == 200 || response.getStatus() == 201) {
			log.debug "Response received from Shark in the postReponseHandler. $response.data"
            def accesstokenstring = ("$response.data" =~ /access_token:([A-Za-z0-9]*.*?)/)
            authtoken = accesstokenstring[0][1]
    	}
        else {
    		log.error "Shark failed. Shark returned ${response.getStatus()}."
        	log.error "Error = ${response.getErrorData()}"
    	}
    }
    } catch (e) {
    	log.error "Error during login: $e"
	}
}

def getUserProfile() {
	def params = [
        uri: "https://ads-field.aylanetworks.com",
		path: "/users/get_user_profile.json",
        headers: ["Content-Type": "application/json", "Accept": "*/*", "Authorization": "auth_token $authtoken"],
    ]
    try {
    httpGet(params) { response ->
        
        if(response.getStatus() == 200 || response.getStatus() == 201) {
			log.debug "Response received from Shark in the postReponseHandler. $response.data"
            def uuidstring = ("$response.data" =~ /uuid:([A-Za-z0-9-]*.*?)/)
            uuid = uuidstring[0][1]
    	}
        else {
    		log.error "Shark failed. Shark returned ${response.getStatus()}."
        	log.error "Error = ${response.getErrorData()}"
    	}
    }
    } catch (e) {
    	log.error "Error during getUserProfile: $e"
	}

}

def getDevices() {
	def params = [
        uri: "https://ads-field.aylanetworks.com",
		path: "/apiv1/devices.json",
        headers: ["Content-Type": "application/json", "Accept": "*/*", "Authorization": "auth_token $authtoken"],
    ]
    try {
    httpGet(params) { response ->
        if(response.getStatus() == 200 || response.getStatus() == 201) {
			log.debug "Response received from Shark in the postReponseHandler. $response.data"
            def devicedsn = ""
            for (devices in response.data.device ) {
                if ("$sharkdevicename" == "${devices.product_name}")
                {   
                    dsnForDevice = "${devices.dsn}"
                }
            }
            if ("$dsnForDevice" == '')
            {
                log.error "$sharkdevicename did not match any product_name on your account. Please verify your `Device Name`."
            }
    	}
        else {
    		log.error "Shark failed. Shark returned ${response.getStatus()}."
        	log.error "Error = ${response.getErrorData()}"
    	}
    }
    } catch (e) {
    	log.error "Error during getDevices: $e"
	}

}
 
def runCmd(String action, String operation, Integer operationValue) {
    def localDevicePort = (devicePort==null) ? "80" : devicePort
	def params = [
        uri: "https://ads-field.aylanetworks.com",
		path: "/apiv1/dsns/$dsnForDevice/properties/$operation/datapoints.json",
        requestContentType: "application/json",
        headers: ["Content-Type": "application/json", "Accept": "*/*", "Authorization": "auth_token $authtoken"],
        body: "{\"datapoint\":{\"value\":\"$operationValue\",\"metadata\":{\"userUUID\":\"$uuid\"}}}"
    ]
    log.debug "$params"
    try {
    httpPost(params) { response ->
        
        if(response.getStatus() == 200 || response.getStatus() == 201) {
			log.debug "Response received from Shark in the postReponseHandler. $response"
    	}
        else {
    		log.error "Shark failed. Shark returned ${response.getStatus()}."
        	log.error "Error = ${response.getErrorData()}"
    	}
    }
    } catch (e) {
    	log.error "Error during runCmd: $e"
	}
}
