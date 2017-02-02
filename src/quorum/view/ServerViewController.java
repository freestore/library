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
package quorum.view;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;




/**
 *
 * @author eduardo
 */
public class ServerViewController extends ViewController {

        
    //private int[] otherProcesses;
    
    
    public ServerViewController(int procId) {
        this(procId,"");
       
    }

    public ServerViewController(int procId, String configHome) {
        super(procId, configHome);
        
        View cv = getViewStore().readView();
        
        
        if(cv == null){
            System.out.println("# Creating current view from configuration file");
            reconfigureTo(new View(getStaticConf().getInitialView(), getInitAddresses()));
        }else{
            System.out.println("#Using view retrieved from the store");
            reconfigureTo(cv);
        }
       
    }

    @Override
    public void reconfigureTo(View newView) {
        super.reconfigureTo(newView); 
        getViewStore().storeView(this.currentView);
    }
    
    
    

    private Map<Integer,InetSocketAddress> getInitAddresses() {
        Map<Integer,InetSocketAddress> ret = new HashMap<Integer,InetSocketAddress>();
        int nextV[] = getStaticConf().getInitialView();
        
        for (int i = 0; i < nextV.length; i++) {
            ret.put(nextV[i], getStaticConf().getRemoteAddress(nextV[i]));
        }

        return ret;
    }/*
    
    public void setTomLayer(TOMLayer tomLayer) {
        this.tomLayer = tomLayer;
    }

    
    public boolean isInCurrentView() {
        return this.currentView.isMember(getStaticConf().getProcessId());
    }

    public int[] getCurrentViewOtherAcceptors() {
        return this.otherProcesses;
    }

    public int[] getCurrentViewAcceptors() {
        return this.currentView.getProcesses();
    }

    public boolean hasUpdates() {
        return !this.updates.isEmpty();
    }*/

}
