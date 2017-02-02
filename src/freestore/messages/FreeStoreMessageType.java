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
package freestore.messages;

/**
 * Possible types of FreeStoreMessageType
 *
 * @author
 */
public enum FreeStoreMessageType {

    RECONFIG, //0
    REC_CONFIRM, //1
    INSTALL_SEQ, //2
    STATE_UPDATE, //3
    VIEW_UPDATED, //4
    L_SEQ_VIEW, //5
    L_SEQ_CONV, //6
    S_PREPARE, //7
    S_PREPARE_REPLY, //8
    S_ACCEPT; //9

    public int toInt() {
        switch (this) {
            case RECONFIG:
                return 0;
            case REC_CONFIRM:
                return 1;
            case INSTALL_SEQ:
                return 2;
            case STATE_UPDATE:
                return 3;
            case VIEW_UPDATED:
                return 4;
            case L_SEQ_VIEW:
                return 5;
            case L_SEQ_CONV:
                return 6;
            case S_PREPARE:
                return 7;
            case S_PREPARE_REPLY:
                return 8;
            case S_ACCEPT:
                return 9;
            default:
                return -1;
        }
    }

    public static FreeStoreMessageType fromInt(int i) {
        switch (i) {
            case 0:
                return RECONFIG;
            case 1:
                return REC_CONFIRM;
            case 2:
                return INSTALL_SEQ;
            case 3:
                return STATE_UPDATE;
            case 4:
                return VIEW_UPDATED;
            case 5:
                return L_SEQ_VIEW;
            case 6:
                return L_SEQ_CONV;
            case 7:
                return S_PREPARE;
            case 8:
                return S_PREPARE_REPLY;
            case 9:
                return S_ACCEPT;
            default:
                return RECONFIG;
        }
    }
}
