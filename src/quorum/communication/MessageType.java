/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package quorum.communication;

/**
 * Possible types of QuorumMessage
 * 
 * @author alysson
 */
public enum MessageType {
    QUORUM_REQUEST, //0
    QUORUM_RESPONSE, //1
    RECONFIGURATION_MESSAGE; //2
    
    
    public int toInt() {
        switch(this) {
            case QUORUM_REQUEST: return 0;
            case QUORUM_RESPONSE: return 1;
            case RECONFIGURATION_MESSAGE: return 2;
            default: return -1;
        }
    }
    
    public static MessageType fromInt(int i) {
        switch(i) {
            case 0: return QUORUM_REQUEST;
            case 1: return QUORUM_RESPONSE;
            case 2: return RECONFIGURATION_MESSAGE;
            default: return QUORUM_REQUEST;
        }            
    }
}
