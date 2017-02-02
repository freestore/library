/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and
 * the authors indicated in the @author tags
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package abd.messages;

/**
 * Possible types of QuorumMessage
 *
 * @author alysson
 */
public enum ABDMessageType {

    READ_TS, //0
    READ_TS_RESP, //1
    WRITE, //2
    READ, //3
    READ_RESP, //4
    WRITE_RESP; //5

    public int toInt() {
        switch (this) {
            case READ_TS:
                return 0;
            case READ_TS_RESP:
                return 1;
            case WRITE:
                return 2;
            case READ:
                return 3;
            case READ_RESP:
                return 4;
            case WRITE_RESP:
                return 5;
            default:
                return -1;
        }
    }

    public static ABDMessageType fromInt(int i) {
        switch (i) {
            case 0:
                return READ_TS;
            case 1:
                return READ_TS_RESP;
            case 2:
                return WRITE;
            case 3:
                return READ;
            case 4:
                return READ_RESP;
            case 5:
                return WRITE_RESP;
            default:
                return READ_TS;
        }
    }
}
